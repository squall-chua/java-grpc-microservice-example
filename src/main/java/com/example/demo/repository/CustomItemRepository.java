package com.example.demo.repository;

import com.example.item.v1.ListItemsRequest;
import com.example.item.v1.ListItemsResponse;

public interface CustomItemRepository {
    ListItemsResponse searchItems(ListItemsRequest request);
}
