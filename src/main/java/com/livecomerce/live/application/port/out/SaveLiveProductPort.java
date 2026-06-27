package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.LiveProduct;

import java.util.List;

public interface SaveLiveProductPort {

    LiveProduct save(LiveProduct product);

    List<LiveProduct> saveAll(List<LiveProduct> products);
}
