package com.lframework.xingyun.sc.service;

import java.util.Collection;
import java.util.Map;

public interface ProductHotnessService {

    void increment(Collection<String> productIds);

    Map<String, Integer> getHotLevels(Collection<String> productIds);
}
