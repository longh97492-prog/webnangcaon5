package com.example.supplychain;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.security.require-https=true",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:b08_prod_https;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class B08ProductionHttpsTest {

    @Autowired private MockMvc mvc;

    @Test
    void httpRequestsRedirectToHttpsWhenEnabled() throws Exception {
        mvc.perform(get("/api/auth/csrf").secure(false))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://")));
    }

    @Test
    void httpsCsrfEndpointWorksWithoutLogin() throws Exception {
        mvc.perform(get("/api/auth/csrf").secure(true))
                .andExpect(status().isOk());
    }
}
