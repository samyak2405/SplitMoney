package com.javaproject.splitewise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserReconciliationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Finds every row in authdb.users that is either absent from splitmoney.users
     * or differs on email / phone / is_active, then upserts it.
     *
     * Returns the number of rows inserted or updated.
     *
     * Cross-schema access works because both schemas live in the same PostgreSQL
     * database (postgres). The DataSource's currentSchema=splitmoney, so the
     * unqualified "users" table resolves to splitmoney.users.
     */
    public int reconcile() {
        String sql = """
                INSERT INTO users (id, email, phone, is_active, created_at, updated_at)
                SELECT a.id,
                       a.email,
                       a.mobile,
                       a.is_active,
                       a.created_at,
                       a.updated_at
                FROM authdb.users a
                LEFT JOIN users s ON s.id = a.id
                WHERE s.id IS NULL
                   OR s.email      IS DISTINCT FROM a.email
                   OR s.phone      IS DISTINCT FROM a.mobile
                   OR s.is_active  IS DISTINCT FROM a.is_active
                ON CONFLICT (id) DO UPDATE SET
                    email      = EXCLUDED.email,
                    phone      = EXCLUDED.phone,
                    is_active  = EXCLUDED.is_active,
                    updated_at = EXCLUDED.updated_at
                """;

        int synced = jdbcTemplate.update(sql);
        if (synced > 0) {
            log.info("user-reconciliation.synced count={}", synced);
        } else {
            log.debug("user-reconciliation.no-diff");
        }
        return synced;
    }
}
