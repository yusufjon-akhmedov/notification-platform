package uz.yusufjon.notificationplatform.integration;

import org.junit.jupiter.api.Test;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateCreateRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationTemplateApiIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    void managerShouldCreateTemplateAndOperatorShouldListAndGetIt() throws Exception {
        String managerEmail = uniqueEmail("manager");
        String managerToken = createAndLoginUser("Manager User", managerEmail, "secret123", RoleName.MANAGER);
        String templateName = "Welcome Template " + System.nanoTime();

        var createResult = mockMvc.perform(authorizedPost("/api/templates", managerToken)
                        .contentType(APPLICATION_JSON)
                        .content(toJson(new NotificationTemplateCreateRequest(
                                templateName,
                                "Welcome",
                                "Hello from template",
                                NotificationChannel.EMAIL,
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(templateName))
                .andExpect(jsonPath("$.subject").value("Welcome"))
                .andExpect(jsonPath("$.body").value("Hello from template"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        long templateId = readBody(createResult).path("id").asLong();
        String operatorToken = registerAndLoginOperator("Template Reader", uniqueEmail("operator-reader"), "secret123");

        mockMvc.perform(authorizedGet("/api/templates", operatorToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(templateId))
                .andExpect(jsonPath("$.content[0].name").value(templateName));

        mockMvc.perform(authorizedGet("/api/templates/{id}", operatorToken, templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(templateId))
                .andExpect(jsonPath("$.name").value(templateName))
                .andExpect(jsonPath("$.channel").value("EMAIL"));
    }

    @Test
    void operatorShouldBeForbiddenToCreateTemplate() throws Exception {
        String operatorToken = registerAndLoginOperator("Template Operator", uniqueEmail("template-operator"), "secret123");

        mockMvc.perform(authorizedPost("/api/templates", operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(toJson(new NotificationTemplateCreateRequest(
                                "Operator Template " + System.nanoTime(),
                                "Denied",
                                "Should not be created",
                                NotificationChannel.EMAIL,
                                true
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied"))
                .andExpect(jsonPath("$.path").value("/api/templates"));
    }
}
