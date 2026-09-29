package com.payflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = {
        "app.rate-limit.login=1000",
        "app.rate-limit.registration=1000",
        "app.rate-limit.transfer=1000"
})
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

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void completesTransferAndUpdatesBalances() throws Exception {
        String token = registerAndLogin("transfer@example.com", "Transfer User");
        java.util.List<String> accountIds = accountIds(token);
        String sourceId = accountIds.get(0);
        String destinationId = accountIds.get(1);

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
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
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accountIds.get(0), accountIds.get(1), "999999.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Saldo insuficiente para realizar a transferência."));
    }

    @Test
    void recordsOneVersionedOutboxEventForACompletedTransfer() throws Exception {
        String token = registerAndLogin("outbox@example.com", "Outbox User");
        java.util.List<String> accounts = accountIds(token);
        UUID idempotencyKey = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        String request = transferJson(accounts.get(0), accounts.get(1), "25.00");

        String response = mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID transferId = UUID.fromString(JsonPath.read(response, "$.id"));

        String payload = jdbcTemplate.queryForObject(
                "SELECT payload::text FROM outbox_events WHERE aggregate_id = ?",
                String.class,
                transferId
        );
        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(payload, "$.eventType"))
                .isEqualTo("TransferCompleted");
        org.assertj.core.api.Assertions.assertThat(JsonPath.<Integer>read(payload, "$.eventVersion"))
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(payload, "$.correlationId"))
                .isEqualTo(correlationId.toString());
        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(payload, "$.transferId"))
                .isEqualTo(transferId.toString());
        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(payload, "$.amount"))
                .isEqualTo("25.00");

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?",
                Integer.class,
                transferId
        );
        org.assertj.core.api.Assertions.assertThat(eventCount).isEqualTo(1);
    }

    @Test
    void doesNotRecordOutboxEventWhenTransferIsRejected() throws Exception {
        String token = registerAndLogin("outbox-rejected@example.com", "Rejected Outbox User");
        java.util.List<String> accounts = accountIds(token);
        UUID correlationId = UUID.randomUUID();

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .header("X-Correlation-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accounts.get(0), accounts.get(1), "999999.00")))
                .andExpect(status().isUnprocessableContent());

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE correlation_id = ?",
                Integer.class,
                correlationId
        );
        org.assertj.core.api.Assertions.assertThat(eventCount).isZero();
    }

    @Test
    void exposesBoundedTransferMetricsThroughActuator() throws Exception {
        String token = registerAndLogin("metrics@example.com", "Metrics User");
        java.util.List<String> accounts = accountIds(token);

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accounts.get(0), accounts.get(1), "10.00")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accounts.get(0), accounts.get(1), "999999.00")))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(get("/actuator/metrics/payflow.transfers.completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("payflow.transfers.completed"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThan(0.0)));
        mvc.perform(get("/actuator/metrics/payflow.transfers.rejected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTags[?(@.tag == 'reason')].values[*]")
                        .value(org.hamcrest.Matchers.hasItem("insufficient-balance")));
        mvc.perform(get("/actuator/metrics/payflow.transfers.rejected")
                        .queryParam("tag", "reason:insufficient-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", greaterThan(0.0)));
        mvc.perform(get("/actuator/metrics/payflow.transfers.duration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTags[?(@.tag == 'outcome')]").exists());
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
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson("5b99802c-24c0-4462-8260-6317a984da20", ownAccount, "10.00")))
                .andExpect(status().isNotFound());
    }

    @Test
    void replaysSameTransferAndRejectsDifferentPayloadForSameKey() throws Exception {
        String token = registerAndLogin("idempotency@example.com", "Idempotency User");
        java.util.List<String> accounts = accountIds(token);
        UUID key = UUID.randomUUID();
        String request = transferJson(accounts.get(0), accounts.get(1), "100.00");

        String firstResponse = mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String replayResponse = mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(replayResponse, "$.id"))
                .isEqualTo(JsonPath.read(firstResponse, "$.id"));
        mvc.perform(get("/api/v1/accounts/" + accounts.get(0)).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.balance").value(2400.00));

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accounts.get(0), accounts.get(1), "101.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("A Idempotency-Key já foi utilizada com dados diferentes."));

        String otherToken = registerAndLogin("other-idempotency@example.com", "Other User");
        java.util.List<String> otherAccounts = accountIds(otherToken);
        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + otherToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(otherAccounts.get(0), otherAccounts.get(1), "25.00")))
                .andExpect(status().isCreated());
    }

    @Test
    void preventsConcurrentTransfersFromSpendingTheSameBalanceTwice() throws Exception {
        String token = registerAndLogin("concurrent-balance@example.com", "Concurrent Balance");
        java.util.List<String> accounts = accountIds(token);
        String request = transferJson(accounts.get(0), accounts.get(1), "2000.00");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> performTransferAfter(ready, start, token, UUID.randomUUID(), request));
            var second = executor.submit(() -> performTransferAfter(ready, start, token, UUID.randomUUID(), request));
            org.assertj.core.api.Assertions.assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            java.util.List<Integer> statuses = java.util.List.of(
                    first.get(10, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(10, TimeUnit.SECONDS).getResponse().getStatus()
            );
            org.assertj.core.api.Assertions.assertThat(statuses)
                    .containsExactlyInAnyOrder(201, 422);
        }

        mvc.perform(get("/api/v1/accounts/" + accounts.get(0)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
        mvc.perform(get("/api/v1/accounts/" + accounts.get(1)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3000.00));
        mvc.perform(get("/api/v1/transfers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void replaysConcurrentRequestsWithTheSameIdempotencyKey() throws Exception {
        String token = registerAndLogin("concurrent-key@example.com", "Concurrent Key");
        java.util.List<String> accounts = accountIds(token);
        String request = transferJson(accounts.get(0), accounts.get(1), "100.00");
        UUID idempotencyKey = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> performTransferAfter(ready, start, token, idempotencyKey, request));
            var second = executor.submit(() -> performTransferAfter(ready, start, token, idempotencyKey, request));
            org.assertj.core.api.Assertions.assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            MvcResult firstResult = first.get(10, TimeUnit.SECONDS);
            MvcResult secondResult = second.get(10, TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(firstResult.getResponse().getStatus()).isEqualTo(201);
            org.assertj.core.api.Assertions.assertThat(secondResult.getResponse().getStatus()).isEqualTo(201);
            org.assertj.core.api.Assertions.assertThat(
                    JsonPath.<String>read(firstResult.getResponse().getContentAsString(), "$.id")
            ).isEqualTo(JsonPath.read(secondResult.getResponse().getContentAsString(), "$.id"));
        }

        mvc.perform(get("/api/v1/accounts/" + accounts.get(0)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2400.00));
        mvc.perform(get("/api/v1/transfers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void requiresIdempotencyKeyToCreateTransfer() throws Exception {
        String token = registerAndLogin("missing-key@example.com", "Missing Key User");
        java.util.List<String> accounts = accountIds(token);

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accounts.get(0), accounts.get(1), "10.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginatesTransferHistoryFromNewestToOldest() throws Exception {
        String token = registerAndLogin("pagination@example.com", "Pagination User");
        java.util.List<String> accounts = accountIds(token);
        for (int index = 1; index <= 6; index++) {
            mvc.perform(post("/api/v1/transfers")
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(transferJson(accounts.get(0), accounts.get(1), index + ".00")))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/v1/transfers?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].amount").value(6.00))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mvc.perform(get("/api/v1/transfers?page=1&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(1.00))
                .andExpect(jsonPath("$.last").value(true));

        mvc.perform(get("/api/v1/transfers")
                        .queryParam("status", "COMPLETED")
                        .queryParam("from", "2020-01-01T00:00:00Z")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void rejectsInvalidTransferHistoryPeriod() throws Exception {
        String token = registerAndLogin("period@example.com", "Period User");

        mvc.perform(get("/api/v1/transfers")
                        .queryParam("from", "2026-09-30T00:00:00Z")
                        .queryParam("to", "2026-09-01T23:59:59Z")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("A data inicial não pode ser posterior à data final."));
    }

    @Test
    void generatesAndPropagatesCorrelationId() throws Exception {
        String suppliedCorrelationId = UUID.randomUUID().toString();

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));

        mvc.perform(post("/api/v1/auth/register")
                        .header("X-Correlation-ID", suppliedCorrelationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"invalid","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-ID", suppliedCorrelationId))
                .andExpect(jsonPath("$.correlationId").value(suppliedCorrelationId));

        mvc.perform(get("/actuator/health").header("X-Correlation-ID", "invalid log value\nforged"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID",
                        org.hamcrest.Matchers.not("invalid log value\nforged")));
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
                .andExpect(jsonPath("$.paths['/api/v1/transfers'].post.parameters[?(@.name == 'Idempotency-Key')]")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/transfers'].post.parameters[?(@.name == 'X-Correlation-ID')]")
                        .exists())
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

    private MvcResult performTransferAfter(CountDownLatch ready, CountDownLatch start, String token,
                                           UUID idempotencyKey, String request) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent transfer start was not released.");
        }
        return mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn();
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
