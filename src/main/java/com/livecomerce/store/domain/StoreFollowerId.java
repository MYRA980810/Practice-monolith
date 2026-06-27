package com.livecomerce.store.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StoreFollowerId implements Serializable {

    private UUID storeId;
    private UUID userId;
}
