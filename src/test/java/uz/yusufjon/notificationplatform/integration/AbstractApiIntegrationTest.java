package uz.yusufjon.notificationplatform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import uz.yusufjon.notificationplatform.auth.dto.LoginRequest;
import uz.yusufjon.notificationplatform.auth.dto.RegisterRequest;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.notification.repository.NotificationDeliveryAttemptRepository;
import uz.yusufjon.notificationplatform.notification.repository.NotificationRepository;
import uz.yusufjon.notificationplatform.template.repository.NotificationTemplateRepository;
import uz.yusufjon.notificationplatform.user.entity.Role;
import uz.yusufjon.notificationplatform.user.entity.User;
import uz.yusufjon.notificationplatform.user.repository.RoleRepository;
import uz.yusufjon.notificationplatform.user.repository.UserRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("notification_platform_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.flyway.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.flyway.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @BeforeEach
    void cleanDatabase() {
        notificationDeliveryAttemptRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        notificationTemplateRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected void registerOperator(String fullName, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RegisterRequest(fullName, email, password))))
                .andExpect(status().isCreated());
    }

    protected String registerAndLoginOperator(String fullName, String email, String password) throws Exception {
        registerOperator(fullName, email, password);
        return loginAndGetToken(email, password);
    }

    protected User createUserWithRole(String fullName, String email, String password, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRole(role);

        return userRepository.save(user);
    }

    protected String createAndLoginUser(String fullName, String email, String password, RoleName roleName) throws Exception {
        createUserWithRole(fullName, email, password, roleName);
        return loginAndGetToken(email, password);
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return readBody(result).path("accessToken").asText();
    }

    protected MockHttpServletRequestBuilder authorizedGet(String url, String token, Object... uriVariables) {
        return get(url, uriVariables).header(HttpHeaders.AUTHORIZATION, bearerToken(token));
    }

    protected MockHttpServletRequestBuilder authorizedPost(String url, String token, Object... uriVariables) {
        return post(url, uriVariables).header(HttpHeaders.AUTHORIZATION, bearerToken(token));
    }

    protected MockHttpServletRequestBuilder authorizedPatch(String url, String token, Object... uriVariables) {
        return patch(url, uriVariables).header(HttpHeaders.AUTHORIZATION, bearerToken(token));
    }

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected String bearerToken(String token) {
        return "Bearer " + token;
    }
}
