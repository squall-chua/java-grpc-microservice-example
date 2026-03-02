package com.example.demo.component;

import com.example.demo.exception.ItemNotFoundException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.repository.ItemDocument;
import com.example.demo.repository.ItemMapper;
import com.example.demo.repository.ItemRepository;
import com.example.item.v1.CreateItemRequest;
import com.example.item.v1.DeleteItemResponse;
import com.example.item.v1.Item;
import com.example.item.v1.ListItemsRequest;
import com.example.item.v1.ListItemsResponse;
import com.example.item.v1.UpdateItemRequest;

@ExtendWith(MockitoExtension.class)
public class ItemComponentTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemComponent itemComponent;

    private ItemDocument stubDocument;
    private Item stubItem;

    @BeforeEach
    void setUp() {
        stubDocument = new ItemDocument();
        stubDocument.setId("test-id");
        stubDocument.setName("Test Item");
        stubDocument.setDescription("Test Description");
        stubDocument.setPrice(99.99);

        stubItem = Item.newBuilder()
                .setId("test-id")
                .setName("Test Item")
                .setDescription("Test Description")
                .setPrice(99.99)
                .build();
    }

    @Test
    void testCreateItem() {
        CreateItemRequest request = CreateItemRequest.newBuilder()
                .setName("Test Item")
                .setDescription("Test Description")
                .setPrice(99.99)
                .build();

        when(itemRepository.save(any(ItemDocument.class))).thenReturn(stubDocument);
        when(itemMapper.toProto(any(ItemDocument.class))).thenReturn(stubItem);

        Item response = itemComponent.createItem(request);

        verify(itemRepository).save(any(ItemDocument.class));
        assertEquals("test-id", response.getId());
        assertEquals("Test Item", response.getName());
    }

    @Test
    void testGetItem_Success() {
        when(itemRepository.findById("test-id")).thenReturn(Optional.of(stubDocument));
        when(itemMapper.toProto(stubDocument)).thenReturn(stubItem);

        Item response = itemComponent.getItem("test-id");

        verify(itemRepository).findById("test-id");
        assertEquals("test-id", response.getId());
    }

    @Test
    void testGetItem_NotFound() {
        when(itemRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemComponent.getItem("missing-id"));
        verify(itemRepository).findById("missing-id");
    }

    @Test
    void testUpdateItem_Success() {
        UpdateItemRequest request = UpdateItemRequest.newBuilder()
                .setId("test-id")
                .setName("Updated Item")
                .setDescription("Updated Description")
                .setPrice(109.99)
                .build();

        when(itemRepository.findById("test-id")).thenReturn(Optional.of(stubDocument));

        ItemDocument updatedDocument = new ItemDocument();
        updatedDocument.setId("test-id");
        updatedDocument.setName("Updated Item");
        updatedDocument.setDescription("Updated Description");
        updatedDocument.setPrice(109.99);

        Item updatedItem = Item.newBuilder()
                .setId("test-id")
                .setName("Updated Item")
                .setDescription("Updated Description")
                .setPrice(109.99)
                .build();

        when(itemRepository.save(any(ItemDocument.class))).thenReturn(updatedDocument);
        when(itemMapper.toProto(any(ItemDocument.class))).thenReturn(updatedItem);

        Item response = itemComponent.updateItem(request);

        verify(itemRepository).findById("test-id");
        verify(itemRepository).save(any(ItemDocument.class));

        assertEquals("Updated Item", response.getName());
        assertEquals(109.99, response.getPrice());
    }

    @Test
    void testUpdateItem_NotFound() {
        UpdateItemRequest request = UpdateItemRequest.newBuilder().setId("missing-id").build();

        when(itemRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> itemComponent.updateItem(request));

        verify(itemRepository).findById("missing-id");
        verify(itemRepository, never()).save(any(ItemDocument.class));
    }

    @Test
    void testDeleteItem_Success() {
        when(itemRepository.existsById("test-id")).thenReturn(true);

        DeleteItemResponse response = itemComponent.deleteItem("test-id");

        verify(itemRepository).existsById("test-id");
        verify(itemRepository).deleteById("test-id");

        assertTrue(response.getSuccess());
    }

    @Test
    void testDeleteItem_NotFound() {
        when(itemRepository.existsById("missing-id")).thenReturn(false);

        assertThrows(ItemNotFoundException.class, () -> itemComponent.deleteItem("missing-id"));

        verify(itemRepository).existsById("missing-id");
        verify(itemRepository, never()).deleteById(anyString());
    }

    @Test
    void testListItems() {
        ListItemsRequest request = ListItemsRequest.newBuilder().setNameContains("Test").build();
        ListItemsResponse response = ListItemsResponse.newBuilder().addItems(stubItem).build();

        when(itemRepository.searchItems(request)).thenReturn(response);

        ListItemsResponse actualResp = itemComponent.listItems(request);

        verify(itemRepository).searchItems(request);
        assertEquals(1, actualResp.getItemsCount());
    }
}
