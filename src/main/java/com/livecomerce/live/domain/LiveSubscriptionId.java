package com.livecomerce.live.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LiveSubscriptionId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "live_id")
    private UUID liveId;

    @Column(name = "buyer_id")
    private UUID buyerId;
}
