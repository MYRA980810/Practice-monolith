package com.livecomerce.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(length = 10)
    private Role role;

    @Column(length = 20)
    private String provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public static User create(String email, String passwordHash, String firstName,
                              String lastName, String phone, Role role) {
        if (email == null && phone == null) {
            throw new IllegalArgumentException("Either email or phone must be provided");
        }
        var user = new User();
        user.id = UUID.randomUUID();
        user.isNew = true;
        user.email = email;
        user.passwordHash = passwordHash;
        user.firstName = firstName;
        user.lastName = lastName;
        user.phone = phone;
        user.role = role;
        user.active = true;
        user.createdAt = OffsetDateTime.now();
        user.updatedAt = OffsetDateTime.now();
        return user;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public static User createFromOAuth(String email, String firstName, String lastName,
                                       String provider, String providerId, String avatarUrl) {
        var user = new User();
        user.id = UUID.randomUUID();
        user.isNew = true;
        user.email = email;
        user.firstName = firstName != null ? firstName : "";
        user.lastName  = lastName  != null ? lastName  : "";
        user.provider   = provider;
        user.providerId = providerId;
        user.avatarUrl  = avatarUrl;
        user.active   = true;
        user.verified = true;
        user.createdAt = OffsetDateTime.now();
        user.updatedAt = OffsetDateTime.now();
        return user;
    }

    public void verify() {
        this.verified = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void assignRole(Role role) {
        this.role = role;
        this.updatedAt = OffsetDateTime.now();
    }

    public VerificationChannel resolveChannel() {
        return phone != null ? VerificationChannel.SMS : VerificationChannel.EMAIL;
    }

    public String resolveRecipient() {
        return phone != null ? phone : email;
    }
}
