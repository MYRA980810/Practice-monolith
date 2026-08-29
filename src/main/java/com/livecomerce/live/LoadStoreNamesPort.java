package com.livecomerce.live;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadStoreNamesPort {

    Map<UUID, String> loadNames(Collection<UUID> storeIds);
}
