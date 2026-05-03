package com.splitmoney.ai.cassandra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("splity_conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplityMessage {

    @PrimaryKeyColumn(name = "group_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String groupId;

    @PrimaryKeyColumn(name = "user_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private String userId;

    @PrimaryKeyColumn(name = "message_id", ordinal = 0, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private UUID messageId;

    private String role;   // "user" | "assistant"
    private String content;

    @Column("created_at")
    private Instant createdAt;
}
