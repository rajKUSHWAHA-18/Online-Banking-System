package com.onlinebanking.transaction;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findAllByAccountCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
}