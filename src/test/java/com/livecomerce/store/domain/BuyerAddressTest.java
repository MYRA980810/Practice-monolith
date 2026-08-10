package com.livecomerce.store.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BuyerAddressTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private BuyerAddress buildAddress() {
        return BuyerAddress.create(
                USER_ID,
                "Av. Corrientes", "1234", null,
                "San Nicolás", "Buenos Aires", "CABA",
                "C1043", "AR",
                -34.6037, -58.3816,
                AddressType.APARTMENT
        );
    }

    @Test
    void create_setsFieldsCorrectly() {
        var address = buildAddress();

        assertThat(address.getId()).isNotNull();
        assertThat(address.getUserId()).isEqualTo(USER_ID);
        assertThat(address.getStreet()).isEqualTo("Av. Corrientes");
        assertThat(address.getExtNumber()).isEqualTo("1234");
        assertThat(address.getIntNumber()).isNull();
        assertThat(address.getNeighborhood()).isEqualTo("San Nicolás");
        assertThat(address.getCity()).isEqualTo("Buenos Aires");
        assertThat(address.getState()).isEqualTo("CABA");
        assertThat(address.getZipCode()).isEqualTo("C1043");
        assertThat(address.getCountry()).isEqualTo("AR");
        assertThat(address.getLatitude()).isEqualTo(-34.6037);
        assertThat(address.getLongitude()).isEqualTo(-58.3816);
        assertThat(address.isDefault()).isFalse();
        assertThat(address.getCreatedAt()).isNotNull();
    }

    @Test
    void create_withoutCoordinates_leavesLatitudeAndLongitudeNull() {
        var address = BuyerAddress.create(
                USER_ID,
                "Av. Corrientes", "1234", null,
                "San Nicolás", "Buenos Aires", "CABA",
                "C1043", "AR",
                null, null,
                AddressType.OTHER
        );

        assertThat(address.getLatitude()).isNull();
        assertThat(address.getLongitude()).isNull();
    }

    @Test
    void create_isNotDefaultByDefault() {
        var address = buildAddress();
        assertThat(address.isDefault()).isFalse();
    }

    @Test
    void setAsDefault_setsIsDefaultTrue() {
        var address = buildAddress();
        address.setAsDefault();
        assertThat(address.isDefault()).isTrue();
    }

    @Test
    void removeDefault_setsIsDefaultFalse() {
        var address = buildAddress();
        address.setAsDefault();
        assertThat(address.isDefault()).isTrue();

        address.removeDefault();
        assertThat(address.isDefault()).isFalse();
    }

    @Test
    void updateDetails_mutatesContentFieldsButLeavesIdentityFieldsUnchanged() {
        var address = buildAddress();
        var originalId        = address.getId();
        var originalUserId    = address.getUserId();
        var originalIsDefault = address.isDefault();
        var originalCreatedAt = address.getCreatedAt();

        address.updateDetails(
                "Av. Nueva", "5678", "Depto 2", "Palermo", "Rosario", "Santa Fe",
                "S2000", "AR", -32.9468, -60.6393, AddressType.OTHER
        );

        assertThat(address.getStreet()).isEqualTo("Av. Nueva");
        assertThat(address.getExtNumber()).isEqualTo("5678");
        assertThat(address.getIntNumber()).isEqualTo("Depto 2");
        assertThat(address.getNeighborhood()).isEqualTo("Palermo");
        assertThat(address.getCity()).isEqualTo("Rosario");
        assertThat(address.getState()).isEqualTo("Santa Fe");
        assertThat(address.getZipCode()).isEqualTo("S2000");
        assertThat(address.getCountry()).isEqualTo("AR");
        assertThat(address.getLatitude()).isEqualTo(-32.9468);
        assertThat(address.getLongitude()).isEqualTo(-60.6393);
        assertThat(address.getAddressType()).isEqualTo(AddressType.OTHER);

        assertThat(address.getId()).isEqualTo(originalId);
        assertThat(address.getUserId()).isEqualTo(originalUserId);
        assertThat(address.isDefault()).isEqualTo(originalIsDefault);
        assertThat(address.getCreatedAt()).isEqualTo(originalCreatedAt);
    }
}
