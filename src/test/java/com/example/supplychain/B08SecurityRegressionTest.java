package com.example.supplychain;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:b08_security_regression;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class B08SecurityRegressionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void anonymousUserCannotReadAccountList() throws Exception {
        mvc.perform(get("/api/user-accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void warehouseStaffCannotAccessEitherAuditLogRoute() throws Exception {
        mvc.perform(get("/api/audit-logs")
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit-logs")
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void stateChangingApiRequiresCsrf() throws Exception {
        mvc.perform(post("/api/auth/logout")
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidProductIdentifierIsHandledAsClientError() throws Exception {
        mvc.perform(get("/api/products/{id}", "not-a-number")
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void secureApiResponseContainsSecurityHeaders() throws Exception {
        mvc.perform(get("/api/test/protected").secure(true)
                        .with(user("staff").roles("WAREHOUSE_STAFF")))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }
}
