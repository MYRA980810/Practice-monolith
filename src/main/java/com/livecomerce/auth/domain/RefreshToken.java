package com.livecomerce.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Creates a new RefreshToken. ttlMs is the time-to-live in milliseconds.
     */
    public static RefreshToken issue(UUID userId, UUID familyId, String tokenHash, long ttlMs) {
        var token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.userId = userId;
        token.familyId = familyId;
        token.tokenHash = tokenHash;
        token.createdAt = Instant.now();
        token.expiresAt = token.createdAt.plusMillis(ttlMs);
        token.revoked = false;
        return token;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return usedAt != null;
    }

    public boolean isActive(Instant now) {
        return !revoked && !isExpired(now);
    }

    public void revoke() {
        this.revoked = true;
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    /**
     * Marks this token as rotated: sets replacedBy, records usedAt, and revokes it.
     */
    public void rotate(UUID replacementId) {
        this.replacedBy = replacementId;
        this.usedAt = Instant.now();
        this.revoked = true;
    }
}
