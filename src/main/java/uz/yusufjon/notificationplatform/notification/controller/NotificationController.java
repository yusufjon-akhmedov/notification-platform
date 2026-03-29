package uz.yusufjon.notificationplatform.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.notification.dto.NotificationCreateRequest;
import uz.yusufjon.notificationplatform.notification.dto.NotificationResponse;
import uz.yusufjon.notificationplatform.notification.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody NotificationCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(request, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                notificationService.getAll(status, channel, authentication.getName())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<NotificationResponse> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.getById(id, authentication.getName()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<NotificationResponse> cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.cancel(id, authentication.getName()));
    }
}
