package uz.yusufjon.notificationplatform.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    void registerShouldCreateOperatorUser() throws Exception {
        String email = uniqueEmail("register");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(toJson(new uz.yusufjon.notificationplatform.auth.dto.RegisterRequest(
                                "Integration Operator",
                                email,
                                "secret123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("OPERATOR"));

        assertThat(userRepository.findByEmail(email)).isPresent();
        assertThat(userRepository.findByEmail(email).orElseThrow().getRole().getName().name()).isEqualTo("OPERATOR");
    }

    @Test
    void loginShouldReturnJwtForRegisteredUser() throws Exception {
        String email = uniqueEmail("login");
        registerOperator("Login Operator", email, "secret123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(toJson(new uz.yusufjon.notificationplatform.auth.dto.LoginRequest(email, "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("OPERATOR"));
    }

    @Test
    void loginShouldRejectInvalidCredentials() throws Exception {
        String email = uniqueEmail("invalid-login");
        registerOperator("Invalid Login Operator", email, "secret123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(toJson(new uz.yusufjon.notificationplatform.auth.dto.LoginRequest(email, "wrong-password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void protectedEndpointWithoutTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/templates"));
    }
}
