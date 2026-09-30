package com.payflow.transfers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Transfer> {
    java.util.Optional<Transfer> findByOwnerIdAndIdempotencyKey(UUID ownerId, UUID idempotencyKey);

    java.util.Optional<Transfer> findByOriginalTransferId(UUID originalTransferId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transfer t where t.id = :id and t.ownerId = :ownerId")
    java.util.Optional<Transfer> findOwnedByIdForUpdate(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId
    );

    @Query("""
            select t from Transfer t
            where t.id = :id and (
                t.sourceAccountId in (select a.id from Account a where a.ownerId = :ownerId)
                or t.destinationAccountId in (select a.id from Account a where a.ownerId = :ownerId)
            )
            """)
    java.util.Optional<Transfer> findVisibleById(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId
    );
}
