package com.splitmoney.document.repository;

import com.splitmoney.document.domain.Document;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByGroupIdOrderByCreatedAtDesc(String groupId, Pageable pageable);
}
