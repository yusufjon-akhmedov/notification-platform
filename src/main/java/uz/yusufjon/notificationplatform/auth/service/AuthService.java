package uz.yusufjon.notificationplatform.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.yusufjon.notificationplatform.auth.dto.AuthResponse;
import uz.yusufjon.notificationplatform.auth.dto.LoginRequest;
import uz.yusufjon.notificationplatform.auth.dto.RegisterRequest;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.exception.ResourceNotFoundException;
import uz.yusufjon.notificationplatform.user.entity.Role;
import uz.yusufjon.notificationplatform.user.entity.User;
import uz.yusufjon.notificationplatform.user.repository.RoleRepository;
import uz.yusufjon.notificationplatform.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        Role role = roleRepository.findByName(RoleName.OPERATOR)
                .orElseThrow(() -> new ResourceNotFoundException("Default role OPERATOR not found"));

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(role);

        userRepository.save(user);

        return new AuthResponse("User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        boolean matches = passwordEncoder.matches(request.password(), user.getPassword());

        if (!matches) {
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new BadRequestException("User is disabled");
        }

        return new AuthResponse("Login successful");
    }
}