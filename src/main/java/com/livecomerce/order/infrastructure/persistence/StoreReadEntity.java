package com.livecomerce.order.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only view over the shared {@code stores} table, scoped to the columns
 * needed to resolve a seller's storeId from their userId — never importing
 * {@code store.application} (see {@link com.livecomerce.order.application.port.out.LoadStoreIdPort}
 * for the module-cycle rationale).
 */
@Entity
@Immutable
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StoreReadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;
}
