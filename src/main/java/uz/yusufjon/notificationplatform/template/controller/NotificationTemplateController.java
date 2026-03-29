package uz.yusufjon.notificationplatform.template.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateCreateRequest;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateResponse;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateUpdateRequest;
import uz.yusufjon.notificationplatform.template.service.NotificationTemplateService;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Tag(name = "Notification Templates", description = "Operations for managing notification templates")
@SecurityRequirement(name = "bearerAuth")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a notification template")
    public ResponseEntity<NotificationTemplateResponse> create(
            @Valid @RequestBody NotificationTemplateCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationTemplateService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "List notification templates")
    public ResponseEntity<List<NotificationTemplateResponse>> getAll() {
        return ResponseEntity.ok(notificationTemplateService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(summary = "Get a notification template by id")
    public ResponseEntity<NotificationTemplateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationTemplateService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a notification template")
    public ResponseEntity<NotificationTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateUpdateRequest request
    ) {
        return ResponseEntity.ok(notificationTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a notification template")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
