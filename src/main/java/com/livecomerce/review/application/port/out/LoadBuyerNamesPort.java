package com.livecomerce.review.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadBuyerNamesPort {

    Map<UUID, String> loadNames(Collection<UUID> buyerIds);
}
