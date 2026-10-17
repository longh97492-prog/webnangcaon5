package com.example.supplychain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.supplychain.entity.Product;
import com.example.supplychain.entity.Role;
import com.example.supplychain.entity.UserAccount;
import com.example.supplychain.repository.ProductRepository;
import com.example.supplychain.repository.AuditLogRepository;
import com.example.supplychain.repository.UserAccountRepository;
import com.example.supplychain.service.AuditLogService;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:b08_v4_risk;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class B08V4RiskVerificationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditLogService auditLogService;
    @Autowired private AuditLogRepository auditLogRepository;

    @Test // BM1
    void bm01InjectionTextDoesNotBypassProductSearchFilter() throws Exception {
        Product product = new Product();
        product.setSku("BM08-" + UUID.randomUUID());
        product.setName("Known safe product " + UUID.randomUUID());
        product.setUnit("pcs");
        product.setPrice(BigDecimal.ONE);
        Product saved = productRepository.saveAndFlush(product);
        try {
            mvc.perform(get("/api/products/search")
                            .param("keyword", "' OR 1=1 --")
                            .with(user("staff").roles("WAREHOUSE_STAFF")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get("/api/products/search")
                            .param("keyword", saved.getSku())
                            .with(user("staff").roles("WAREHOUSE_STAFF")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        } finally {
            productRepository.deleteById(saved.getId());
        }
    }

    @Test // BM1
    void bm01SqlWildcardCharactersAreEscapedInSearch() throws Exception {
        mvc.perform(get("/api/products/search")
                        .param("keyword", "%' OR 'a'='a")
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test // BM2, stored XSS is also subject to the browser evidence step
    void bm02AuditUiUsesTextNodesInsteadOfInjectingHtml() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/static/audit-logs.js")) {
            assertNotNull(input, "Missing audit JS");
            String js = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(js.contains("cell.textContent = value ?? \"\""));
            assertFalse(js.contains("innerHTML"));
        }
        try (InputStream input = getClass().getResourceAsStream("/static/audit-logs.html")) {
            assertNotNull(input, "Missing audit HTML");
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(html.contains("src=\"/audit-logs.js\""));
            assertTrue(html.contains("href=\"/audit-logs.css\""));
            assertFalse(html.contains("<script>"));
            assertFalse(html.contains("<style>"));
        }
        String payload = "<img src=x onerror=alert(1)>";
        auditLogService.record("b08-xss", "B08_XSS_TEST", payload);
        mvc.perform(get("/api/audit-logs").param("action", "B08_XSS_TEST")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityName").value(payload));
    }

    @Test // BM3
    void bm03CrossSiteStateChangingRequestWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/logout")
                        .header("Origin", "https://external.invalid")
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout")
                        .with(csrf().asHeader())
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isOk());
    }

    @Test // BM3, static audit page uses CSRF token issued to the same session
    void bm03CsrfEndpointPublishesHeaderNameAndToken() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/static/audit-logs.js")) {
            assertNotNull(input, "Missing audit JS");
            String js = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(js.contains("/api/auth/csrf"));
            assertTrue(js.contains("[csrf.headerName]: csrf.token"));
        }
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test // BM3 end-to-end same-session token (not just mocked csrf postprocessor)
    void bm03BrowserCanUseIssuedCsrfTokenInSameSession() throws Exception {
        var issued = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode token = new ObjectMapper().readTree(issued.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) issued.getRequest().getSession(false);
        assertNotNull(session, "CSRF endpoint must establish a session");
        mvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test // BM4
    void bm04StaffCannotReadOtherAccountsByChangingIdentifiers() throws Exception {
        for (String path : new String[] {"/api/user-accounts/1", "/api/user-accounts/999999"}) {
            mvc.perform(get(path).with(user("staff").roles("WAREHOUSE_STAFF")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/users/another-person")
                        .with(user("alice").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test // BM5
    void bm05RoleMatrixRejectsDeniedOperations() throws Exception {
        for (String path : new String[] {
                "/api/admin/test", "/api/audit-logs", "/api/admin/audit-logs", "/api/user-accounts"
        }) {
            mvc.perform(get(path).with(user("staff").roles("WAREHOUSE_STAFF")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/audit-logs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test // BM6
    void bm06RegistrationHashesPasswordAndDoesNotAllowAdminRole() throws Exception {
        String username = "b08-v4-" + UUID.randomUUID();
        String password = "Strong123!Secure";
        try {
            mvc.perform(post("/api/auth/register").with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\""
                                    + password + "\",\"role\":\"ADMIN\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("WAREHOUSE_STAFF"))
                    .andExpect(jsonPath("$.password").value(org.hamcrest.Matchers.nullValue()));
            UserAccount saved = userAccountRepository.findByUsername(username).orElseThrow();
            assertEquals(Role.WAREHOUSE_STAFF, saved.getRole());
            assertFalse(password.equals(saved.getPassword()));
            assertTrue(passwordEncoder.matches(password, saved.getPassword()));
        } finally {
            userAccountRepository.findByUsername(username)
                    .ifPresent(userAccountRepository::delete);
        }
    }

    @Test // BM7
    void bm07UnauthenticatedAccessIsRejectedAndCookieFlagsAreConfiguredInProd() throws Exception {
        mvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized());
        try (InputStream input = getClass().getResourceAsStream("/application-prod.properties")) {
            assertNotNull(input);
            String properties = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(properties.contains("server.servlet.session.cookie.http-only=true"));
            assertTrue(properties.contains("server.servlet.session.cookie.secure=true"));
        }
    }

    @Test // BM8
    void bm08RejectsFakeImagePayload() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("file", "test.png", "image/png",
                "not a PNG file".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/files/upload").file(fake)
                        .with(csrf().asHeader())
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test // BM8
    void bm08RejectsSpreadsheetContainingVbaMacro() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (String entry : new String[] {
                    "[Content_Types].xml", "xl/workbook.xml", "xl/vbaProject.bin"
            }) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write("safe test fixture".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        MockMultipartFile macro = new MockMultipartFile("file", "macro.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        mvc.perform(multipart("/api/files/upload").file(macro)
                        .with(csrf().asHeader())
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test // BM9
    void bm09MalformedInputsNeverExposeInternalExceptionDetails() throws Exception {
        mvc.perform(get("/api/products/not-a-long")
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("org.springframework"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("SQLException"))));
        mvc.perform(post("/api/auth/register").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test // BM10
    void bm10RepeatedWrongLoginsReceive429() throws Exception {
        String username = "b08-missing-" + UUID.randomUUID();
        String body = "{\"username\":\"" + username + "\",\"password\":\"invalid\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(log -> username.equals(log.getUsername())
                        && "LOGIN_BLOCKED".equals(log.getAction())));
    }

    @Test // BM12, transport headers need a real public HTTPS check too
    void bm12HttpsResponseIncludesFiveSecurityHeaders() throws Exception {
        mvc.perform(get("/api/test/protected").secure(true)
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test // BM12
    void bm12ProdDisablesH2ConsoleAndDetailedErrors() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/application-prod.properties")) {
            assertNotNull(input);
            String properties = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(properties.contains("spring.h2.console.enabled=false"));
            assertTrue(properties.contains("server.error.include-stacktrace=never"));
            assertTrue(properties.contains("app.security.require-https=true"));
        }
    }
}
