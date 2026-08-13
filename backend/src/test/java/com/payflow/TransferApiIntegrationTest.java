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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.startsWith;
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
        String token = registerAndLogin("transfer@example.com", "Transfer User");
        java.util.List<String> accountIds = accountIds(token);
        String sourceId = accountIds.get(0);
        String destinationId = accountIds.get(1);

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(sourceId, destinationId, "350.00")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/v1/accounts/" + sourceId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2150.00));
        mvc.perform(get("/api/v1/accounts/" + destinationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1350.00));
    }

    @Test
    void rejectsInsufficientBalanceWithoutPersistingTransfer() throws Exception {
        String token = registerAndLogin("insufficient@example.com", "Insufficient User");
        java.util.List<String> accountIds = accountIds(token);
        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accountIds.get(0), accountIds.get(1), "999999.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Saldo insuficiente para realizar a transferência."));
    }

    @Test
    void listsOnlyAuthenticatedUsersAccounts() throws Exception {
        String token = registerAndLogin("owner@example.com", "Owner User");
        mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].holderName", everyItem(startsWith("Owner User"))));

        mvc.perform(get("/api/v1/accounts/5b99802c-24c0-4462-8260-6317a984da20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        String ownAccount = accountIds(token).get(0);
        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson("5b99802c-24c0-4462-8260-6317a984da20", ownAccount, "10.00")))
                .andExpect(status().isNotFound());
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
        mvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/transfers"))
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

    private String registerAndLogin(String email, String name) throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"safe-password"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated());
        String response = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"safe-password"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private java.util.List<String> accountIds(String token) throws Exception {
        String response = mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$[*].id");
    }

    private String transferJson(String sourceId, String destinationId, String amount) {
        return """
                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":%s,"currency":"BRL"}
                """.formatted(sourceId, destinationId, amount);
    }
}
