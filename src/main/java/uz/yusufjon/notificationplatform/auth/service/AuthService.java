package uz.yusufjon.notificationplatform.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.notificationplatform.auth.dto.AuthResponse;
import uz.yusufjon.notificationplatform.auth.dto.LoginRequest;
import uz.yusufjon.notificationplatform.auth.dto.RegisterRequest;
import uz.yusufjon.notificationplatform.auth.security.JwtService;
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
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
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

        return new AuthResponse(
                "User registered successfully",
                null,
                null,
                user.getEmail(),
                user.getRole().getName().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));

            String accessToken = jwtService.generateToken(userDetails);

            return new AuthResponse(
                    "Login successful",
                    accessToken,
                    "Bearer",
                    user.getEmail(),
                    user.getRole().getName().name()
            );
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid email or password");
        } catch (DisabledException ex) {
            throw new BadRequestException("User is disabled");
        }
    }
}
