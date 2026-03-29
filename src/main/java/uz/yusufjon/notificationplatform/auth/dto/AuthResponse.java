package uz.yusufjon.notificationplatform.auth.dto;

public record AuthResponse(
        String message,
        String accessToken,
        String tokenType,
        String email,
        String role
) {
}
