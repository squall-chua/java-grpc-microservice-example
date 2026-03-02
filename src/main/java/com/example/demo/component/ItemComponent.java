package com.example.demo.component;

import com.example.demo.exception.ItemNotFoundException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.repository.ItemDocument;
import com.example.demo.repository.ItemMapper;
import com.example.demo.repository.ItemRepository;
import com.example.item.v1.CreateItemRequest;
import com.example.item.v1.DeleteItemResponse;
import com.example.item.v1.Item;
import com.example.item.v1.ListItemsRequest;
import com.example.item.v1.ListItemsResponse;
import com.example.item.v1.UpdateItemRequest;

@Component
public class ItemComponent {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Autowired
    public ItemComponent(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    public Item createItem(CreateItemRequest request) {
        ItemDocument document = new ItemDocument();
        document.setName(request.getName());
        document.setDescription(request.getDescription());
        document.setPrice(request.getPrice());

        document = itemRepository.save(document);
        return itemMapper.toProto(document);
    }

    public Item getItem(String id) {
        Optional<ItemDocument> document = itemRepository.findById(id);
        if (document.isPresent()) {
            return itemMapper.toProto(document.get());
        } else {
            throw new ItemNotFoundException("Item not found");
        }
    }

    public Item updateItem(UpdateItemRequest request) {
        Optional<ItemDocument> existing = itemRepository.findById(request.getId());
        if (existing.isPresent()) {
            ItemDocument document = existing.get();
            document.setName(request.getName());
            document.setDescription(request.getDescription());
            document.setPrice(request.getPrice());

            document = itemRepository.save(document);
            return itemMapper.toProto(document);
        } else {
            throw new ItemNotFoundException("Item not found");
        }
    }

    public DeleteItemResponse deleteItem(String id) {
        if (itemRepository.existsById(id)) {
            itemRepository.deleteById(id);
            return DeleteItemResponse.newBuilder().setSuccess(true).build();
        } else {
            throw new ItemNotFoundException("Item not found");
        }
    }

    public ListItemsResponse listItems(ListItemsRequest request) {
        return itemRepository.searchItems(request);
    }
}
