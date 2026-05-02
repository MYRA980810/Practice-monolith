package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.domain.Product;

public interface SaveProductPort {

    Product save(Product product);
}
