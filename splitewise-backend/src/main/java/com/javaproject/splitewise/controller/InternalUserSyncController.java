package com.javaproject.splitewise.controller;

import com.javaproject.splitewise.service.UserReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserSyncController {

    private final UserReconciliationService reconciliationService;

    @PostMapping("/reconcile")
    public ResponseEntity<Map<String, Object>> reconcile() {
        int synced = reconciliationService.reconcile();
        return ResponseEntity.ok(Map.of(
                "synced", synced,
                "message", synced > 0
                        ? synced + " user(s) synced from authdb"
                        : "No differences found — tables are in sync"
        ));
    }
}
