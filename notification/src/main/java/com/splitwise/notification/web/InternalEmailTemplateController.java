package com.splitwise.notification.web;

import com.splitwise.notification.domain.NotificationEventType;
import com.splitwise.notification.service.email.EmailTemplateAdminService;
import com.splitwise.notification.dto.request.EmailTemplateUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/email-templates")
public class InternalEmailTemplateController {
    private final EmailTemplateAdminService templateAdminService;

    public InternalEmailTemplateController(EmailTemplateAdminService templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsert(@Valid @RequestBody EmailTemplateUpsertRequest request) {
        templateAdminService.upsertTemplate(request);
    }

    @PostMapping("/{eventType}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable NotificationEventType eventType) {
        templateAdminService.activate(eventType);
    }

    @PostMapping("/{eventType}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable NotificationEventType eventType) {
        templateAdminService.deactivate(eventType);
    }

    @PostMapping("/cache/evict-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evictAll() {
        templateAdminService.evictAll();
    }
}
