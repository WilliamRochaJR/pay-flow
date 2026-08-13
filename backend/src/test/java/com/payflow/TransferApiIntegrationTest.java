package com.payflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransferApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> "cGF5Zmxvdy10ZXN0LWp3dC1zZWNyZXQta2V5LTMyLWJ5dGVz");
    }

    @Autowired
    MockMvc mvc;

    @Test
    void completesTransferAndUpdatesBalances() throws Exception {
        mvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": "5b99802c-24c0-4462-8260-6317a984da20",
                                  "destinationAccountId": "565620a5-e66d-48c9-8ff2-39aa22ace194",
                                  "amount": 350.00,
                                  "currency": "BRL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/v1/accounts/5b99802c-24c0-4462-8260-6317a984da20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2150.00));
        mvc.perform(get("/api/v1/accounts/565620a5-e66d-48c9-8ff2-39aa22ace194"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2150.00));
    }

    @Test
    void rejectsInsufficientBalanceWithoutPersistingTransfer() throws Exception {
        mvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": "5b99802c-24c0-4462-8260-6317a984da20",
                                  "destinationAccountId": "565620a5-e66d-48c9-8ff2-39aa22ace194",
                                  "amount": 999999.00,
                                  "currency": "BRL"
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Saldo insuficiente para realizar a transferência."));
    }

    @Test
    void listsSeededAccounts() throws Exception {
        mvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void publishesOpenApiContractAndSwaggerUi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PayFlow API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transfers'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me'].get.security[0].bearerAuth").exists());

        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void registersLogsInAndReturnsAuthenticatedUser() throws Exception {
        String email = "william@example.com";
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "William Rocha",
                                  "email": "WILLIAM@example.com",
                                  "password": "safe-password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("William Rocha"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Outro nome",
                                  "email": "william@example.com",
                                  "password": "another-password"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este e-mail já está cadastrado."));

        String loginResponse = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "william@example.com",
                                  "password": "safe-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();

        String accessToken = JsonPath.read(loginResponse, "$.accessToken");
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("William Rocha"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void rejectsUnauthenticatedMeAndInvalidCredentials() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }
}
