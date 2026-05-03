package com.splitmoney.chat.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table("messages_by_group")
public class Message {

    @PrimaryKeyColumn(name = "group_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private String groupId;

    @PrimaryKeyColumn(name = "bucket", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private String bucket;

    @PrimaryKeyColumn(name = "message_id", type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING, ordinal = 0)
    private UUID messageId;

    @Column("sender_id")
    private String senderId;

    @Column("sender_email")
    private String senderEmail;

    @Column("content")
    private String content;

    @Column("msg_type")
    private String msgType;

    @Column("created_at")
    private Instant createdAt;
}
