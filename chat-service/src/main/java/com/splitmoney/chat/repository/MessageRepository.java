package com.splitmoney.chat.repository;

import com.splitmoney.chat.domain.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends CassandraRepository<Message, UUID> {

    @Query("SELECT * FROM messages_by_group WHERE group_id = ?0 AND bucket = ?1 ORDER BY message_id DESC LIMIT ?2")
    List<Message> findByGroupIdAndBucket(String groupId, String bucket, int limit);

    @Query("SELECT * FROM messages_by_group WHERE group_id = ?0 AND bucket = ?1 AND message_id < ?2 ORDER BY message_id DESC LIMIT ?3")
    List<Message> findByGroupIdAndBucketAndMessageIdBefore(String groupId, String bucket, UUID cursorId, int limit);
}
