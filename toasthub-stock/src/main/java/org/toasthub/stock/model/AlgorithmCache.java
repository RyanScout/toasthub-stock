package org.toasthub.stock.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class AlgorithmCache {

    Object latestGoldenCross;

    private Map<String , customAlgs > map = new ConcurrentHashMap<String,List<PrefFormFieldValue>>();
 
    map.get(userId).getCustomGoldenCross
    map.get(userId).getCustombollingerban


}
