package com.livecomerce.live;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadSellerNamesPort {

    Map<UUID, String> loadNames(Collection<UUID> sellerIds);
}
