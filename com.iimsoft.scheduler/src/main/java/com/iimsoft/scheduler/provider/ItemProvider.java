package com.iimsoft.scheduler.provider;


import com.iimsoft.mes.model.I_mom_item;
import com.iimsofttech.model.InterfaceWrapperHelper;

import java.util.HashMap;
import java.util.Map;

public class ItemProvider {
    private final Map<Integer, I_mom_item> itemCache = new HashMap<>();

    public I_mom_item getItemById(int itemId) {
        return itemCache.computeIfAbsent(itemId, id -> InterfaceWrapperHelper.load(id,I_mom_item.class));
    }
}
