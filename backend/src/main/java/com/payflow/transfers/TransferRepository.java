package com.payflow.transfers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Transfer> {
    java.util.Optional<Transfer> findByOwnerIdAndIdempotencyKey(UUID ownerId, UUID idempotencyKey);

    @org.springframework.data.jpa.repository.Query("""
            select t from Transfer t
            where t.id = :id and (
                t.sourceAccountId in (select a.id from Account a where a.ownerId = :ownerId)
                or t.destinationAccountId in (select a.id from Account a where a.ownerId = :ownerId)
            )
            """)
    java.util.Optional<Transfer> findVisibleById(
            @org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("ownerId") UUID ownerId
    );
}
