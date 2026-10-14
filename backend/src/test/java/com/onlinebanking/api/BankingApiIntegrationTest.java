package com.onlinebanking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BankingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void depositsAndTransfersUpdateBalancesAndRecordBothSidesAtomically() throws Exception {
        TestSession session = registerCustomer();

        mockMvc.perform(get("/api/accounts").cookie(session.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.totalBalance").value(0));

        JsonNode initialAccounts = getJson("/api/accounts", session);
        String sourceAccount = initialAccounts.path("accounts").get(0).path("accountNumber").asText();
        MvcResult destinationResponse = mockMvc.perform(post("/api/accounts")
                        .cookie(session.cookies()).header("X-XSRF-TOKEN", session.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SAVINGS","name":"Rainy day"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String destinationAccount = objectMapper.readTree(destinationResponse.getResponse().getContentAsString())
                .path("accountNumber").asText();

        mockMvc.perform(post("/api/accounts/{accountNumber}/deposits", sourceAccount)
                        .cookie(session.cookies()).header("X-XSRF-TOKEN", session.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000.00,"description":"Payday"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000));

        MvcResult transferResult = mockMvc.perform(post("/api/transfers")
                        .cookie(session.cookies()).header("X-XSRF-TOKEN", session.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountNumber":"%s","toAccountNumber":"%s","amount":250.00}
                                """.formatted(sourceAccount, destinationAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$[1].type").value("TRANSFER_IN"))
                .andReturn();
        JsonNode transferEntries = objectMapper.readTree(transferResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(transferEntries.path(0).path("referenceId").asText(),
                transferEntries.path(1).path("referenceId").asText());

        mockMvc.perform(post("/api/transfers")
                        .cookie(session.cookies()).header("X-XSRF-TOKEN", session.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountNumber":"%s","toAccountNumber":"%s","amount":1000.00}
                                """.formatted(sourceAccount, destinationAccount)))
                .andExpect(status().isConflict());

        JsonNode finalAccounts = getJson("/api/accounts", session);
        org.junit.jupiter.api.Assertions.assertEquals("750.00",
                finalAccounts.path("accounts").get(1).path("balance").decimalValue().toPlainString());
        org.junit.jupiter.api.Assertions.assertEquals("250.00",
                finalAccounts.path("accounts").get(0).path("balance").decimalValue().toPlainString());
        mockMvc.perform(get("/api/transactions").cookie(session.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void accountDataRequiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    private TestSession registerCustomer() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .path("token").asText();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        String email = "customer-%s@example.test".formatted(UUID.randomUUID());

        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie).header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Customer","email":"%s","password":"correct horse battery","accountType":"CHECKING"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie sessionCookie = registration.getResponse().getCookie("JSESSIONID");
        org.junit.jupiter.api.Assertions.assertNotNull(sessionCookie);
        return new TestSession(csrfCookie, sessionCookie, csrfToken);
    }

    private JsonNode getJson(String path, TestSession session) throws Exception {
        MvcResult result = mockMvc.perform(get(path).cookie(session.sessionCookie()))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record TestSession(Cookie csrfCookie, Cookie sessionCookie, String csrfToken) {
        Cookie[] cookies() {
            return new Cookie[] {csrfCookie, sessionCookie};
        }
    }
}