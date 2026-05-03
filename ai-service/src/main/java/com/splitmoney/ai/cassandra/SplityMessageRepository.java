package com.splitmoney.ai.cassandra;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SplityMessageRepository extends CassandraRepository<SplityMessage, UUID> {

    @Query("SELECT * FROM splity_conversations WHERE group_id = ?0 AND user_id = ?1 LIMIT ?2")
    List<SplityMessage> findHistory(String groupId, String userId, int limit);
}
