package uz.yusufjon.notificationplatform.integration;

import org.junit.jupiter.api.Test;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.notification.dto.NotificationCreateRequest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationApiIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    void operatorShouldCreateListGetAndCancelNotification() throws Exception {
        String operatorToken = registerAndLoginOperator("Notification Operator", uniqueEmail("notification"), "secret123");

        var createResult = mockMvc.perform(authorizedPost("/api/notifications", operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(toJson(new NotificationCreateRequest(
                                "recipient@example.com",
                                "Welcome",
                                "Welcome to Notification Platform",
                                NotificationChannel.EMAIL,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipient").value("recipient@example.com"))
                .andExpect(jsonPath("$.subject").value("Welcome"))
                .andExpect(jsonPath("$.content").value("Welcome to Notification Platform"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        long notificationId = readBody(createResult).path("id").asLong();

        mockMvc.perform(authorizedGet("/api/notifications", operatorToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(notificationId))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        mockMvc.perform(authorizedGet("/api/notifications/{id}", operatorToken, notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.recipient").value("recipient@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(authorizedPatch("/api/notifications/{id}/cancel", operatorToken, notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void operatorShouldBeForbiddenFromAccessingAnotherUsersNotificationWhileAdminCanAccessIt() throws Exception {
        String ownerToken = registerAndLoginOperator("Owner Operator", uniqueEmail("owner"), "secret123");
        String otherOperatorToken = registerAndLoginOperator("Other Operator", uniqueEmail("other"), "secret123");
        String adminToken = createAndLoginUser("Admin User", uniqueEmail("admin"), "secret123", RoleName.ADMIN);

        var createResult = mockMvc.perform(authorizedPost("/api/notifications", ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content(toJson(new NotificationCreateRequest(
                                "private@example.com",
                                "Private",
                                "Operator private notification",
                                NotificationChannel.EMAIL,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        long notificationId = readBody(createResult).path("id").asLong();

        mockMvc.perform(authorizedGet("/api/notifications/{id}", otherOperatorToken, notificationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this notification"))
                .andExpect(jsonPath("$.path").value("/api/notifications/" + notificationId));

        mockMvc.perform(authorizedGet("/api/notifications", otherOperatorToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(authorizedGet("/api/notifications/{id}", adminToken, notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.recipient").value("private@example.com"));

        mockMvc.perform(authorizedGet("/api/notifications", adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(notificationId));
    }

    @Test
    void dispatchShouldMarkNotificationAsSentPersistAttemptAndRejectLaterCancel() throws Exception {
        String operatorToken = registerAndLoginOperator("Dispatch Operator", uniqueEmail("dispatch"), "secret123");

        var createResult = mockMvc.perform(authorizedPost("/api/notifications", operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(toJson(new NotificationCreateRequest(
                                "dispatch@example.com",
                                "Dispatch Test",
                                "Dispatch body",
                                NotificationChannel.EMAIL,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        long notificationId = readBody(createResult).path("id").asLong();

        mockMvc.perform(authorizedPost("/api/notifications/{id}/dispatch", operatorToken, notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").isNotEmpty());

        mockMvc.perform(authorizedGet("/api/notifications/{id}/attempts", operatorToken, notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attemptNumber").value(1))
                .andExpect(jsonPath("$[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$[0].status").value("SENT"))
                .andExpect(jsonPath("$[0].providerResponse", containsString("Mock EMAIL delivery accepted")));

        mockMvc.perform(authorizedPatch("/api/notifications/{id}/cancel", operatorToken, notificationId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only pending notifications can be cancelled"))
                .andExpect(jsonPath("$.path").value("/api/notifications/" + notificationId + "/cancel"));
    }
}
