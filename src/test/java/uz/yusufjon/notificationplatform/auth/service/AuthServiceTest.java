package uz.yusufjon.notificationplatform.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import uz.yusufjon.notificationplatform.auth.dto.AuthResponse;
import uz.yusufjon.notificationplatform.auth.dto.LoginRequest;
import uz.yusufjon.notificationplatform.auth.dto.RegisterRequest;
import uz.yusufjon.notificationplatform.auth.security.JwtService;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.user.entity.Role;
import uz.yusufjon.notificationplatform.user.repository.RoleRepository;
import uz.yusufjon.notificationplatform.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateEnabledOperatorUserWhenEmailIsUnique() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "secret123");
        Role operatorRole = role(RoleName.OPERATOR);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName(RoleName.OPERATOR)).thenReturn(Optional.of(operatorRole));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(uz.yusufjon.notificationplatform.user.entity.User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        ArgumentCaptor<uz.yusufjon.notificationplatform.user.entity.User> userCaptor =
                ArgumentCaptor.forClass(uz.yusufjon.notificationplatform.user.entity.User.class);
        verify(userRepository).save(userCaptor.capture());

        uz.yusufjon.notificationplatform.user.entity.User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo(request.fullName());
        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getRole().getName()).isEqualTo(RoleName.OPERATOR);

        assertThat(response.message()).isEqualTo("User registered successfully");
        assertThat(response.accessToken()).isNull();
        assertThat(response.tokenType()).isNull();
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.role()).isEqualTo(RoleName.OPERATOR.name());
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "secret123");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldAssignOperatorRoleByDefault() {
        RegisterRequest request = new RegisterRequest("Another User", "operator@example.com", "secret123");
        Role operatorRole = role(RoleName.OPERATOR);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName(RoleName.OPERATOR)).thenReturn(Optional.of(operatorRole));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(uz.yusufjon.notificationplatform.user.entity.User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<uz.yusufjon.notificationplatform.user.entity.User> userCaptor =
                ArgumentCaptor.forClass(uz.yusufjon.notificationplatform.user.entity.User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isSameAs(operatorRole);
        verify(roleRepository).findByName(RoleName.OPERATOR);
    }

    @Test
    void loginShouldReturnJwtPayloadForValidCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "secret123");
        UserDetails userDetails = User.withUsername(request.email())
                .password("encoded-password")
                .authorities("ROLE_OPERATOR")
                .build();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        uz.yusufjon.notificationplatform.user.entity.User user = user(request.email(), RoleName.OPERATOR, 10L);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.message()).isEqualTo("Login successful");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.role()).isEqualTo(RoleName.OPERATOR.name());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(eq(userDetails));
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginShouldRejectDisabledUser() {
        LoginRequest request = new LoginRequest("disabled@example.com", "secret123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User is disabled");

        verify(jwtService, never()).generateToken(any());
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    private uz.yusufjon.notificationplatform.user.entity.User user(String email, RoleName roleName, Long id) {
        uz.yusufjon.notificationplatform.user.entity.User user =
                new uz.yusufjon.notificationplatform.user.entity.User();
        user.setId(id);
        user.setEmail(email);
        user.setEnabled(true);
        user.setRole(role(roleName));
        return user;
    }
}
