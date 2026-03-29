package uz.yusufjon.notificationplatform.notification.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import uz.yusufjon.notificationplatform.common.dto.PageResponse;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.notification.dto.NotificationCreateRequest;
import uz.yusufjon.notificationplatform.notification.dto.NotificationDeliveryAttemptResponse;
import uz.yusufjon.notificationplatform.notification.dto.NotificationResponse;
import uz.yusufjon.notificationplatform.notification.service.NotificationDeliveryAttemptService;
import uz.yusufjon.notificationplatform.notification.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Operations for creating and managing notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDeliveryAttemptService notificationDeliveryAttemptService;

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Create a notification")
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody NotificationCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(request, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List notifications with pagination, sorting, and optional filters")
    public ResponseEntity<PageResponse<NotificationResponse>> getAll(
            @Parameter(description = "Filter by notification status")
            @RequestParam(required = false) NotificationStatus status,
            @Parameter(description = "Filter by notification channel")
            @RequestParam(required = false) NotificationChannel channel,
            @Parameter(description = "Filter by recipient keyword")
            @RequestParam(required = false) String recipient,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                notificationService.getAll(status, channel, recipient, pageable, authentication.getName())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Get a notification by id")
    public ResponseEntity<NotificationResponse> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.getById(id, authentication.getName()));
    }

    @GetMapping("/{id}/attempts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List delivery attempts for a notification")
    public ResponseEntity<List<NotificationDeliveryAttemptResponse>> getAttempts(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                notificationDeliveryAttemptService.getByNotificationId(id, authentication.getName())
        );
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Dispatch a pending notification using the mock sender flow")
    public ResponseEntity<NotificationResponse> dispatch(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.dispatch(id, authentication.getName()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Cancel a pending notification")
    public ResponseEntity<NotificationResponse> cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.cancel(id, authentication.getName()));
    }
}
