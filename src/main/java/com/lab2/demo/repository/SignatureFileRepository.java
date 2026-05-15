package com.lab2.demo.repository;

import com.lab2.demo.model.SignatureFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignatureFileRepository extends JpaRepository<SignatureFile, UUID> {

    Optional<SignatureFile> findBySignatureId(UUID signatureId);

    List<SignatureFile> findBySignatureIdIn(Collection<UUID> signatureIds);

    boolean existsByObjectKey(String objectKey);
}