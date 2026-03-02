package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.demo.component.ItemComponent;
import com.example.demo.exception.ItemNotFoundException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.item.v1.CreateItemRequest;
import com.example.item.v1.DeleteItemRequest;
import com.example.item.v1.DeleteItemResponse;
import com.example.item.v1.GetItemRequest;
import com.example.item.v1.Item;
import com.example.item.v1.ListItemsRequest;
import com.example.item.v1.ListItemsResponse;
import com.example.item.v1.UpdateItemRequest;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
public class ItemGrpcServiceTest {

    @Mock
    private ItemComponent itemComponent;

    @InjectMocks
    private ItemGrpcService itemGrpcService;

    @Mock
    private StreamObserver<Item> itemResponseObserver;

    @Mock
    private StreamObserver<DeleteItemResponse> deleteResponseObserver;

    @Mock
    private StreamObserver<ListItemsResponse> listResponseObserver;

    @Captor
    private ArgumentCaptor<Item> itemCaptor;

    @Captor
    private ArgumentCaptor<DeleteItemResponse> deleteCaptor;

    @Captor
    private ArgumentCaptor<Throwable> throwableCaptor;

    private Item stubItem;

    @BeforeEach
    void setUp() {
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

        when(itemComponent.createItem(request)).thenReturn(stubItem);

        itemGrpcService.createItem(request, itemResponseObserver);

        verify(itemComponent).createItem(request);
        verify(itemResponseObserver).onNext(itemCaptor.capture());
        verify(itemResponseObserver).onCompleted();

        assertEquals("test-id", itemCaptor.getValue().getId());
    }

    @Test
    void testGetItem_Success() {
        GetItemRequest request = GetItemRequest.newBuilder().setId("test-id").build();

        when(itemComponent.getItem("test-id")).thenReturn(stubItem);

        itemGrpcService.getItem(request, itemResponseObserver);

        verify(itemComponent).getItem("test-id");
        verify(itemResponseObserver).onNext(itemCaptor.capture());
        verify(itemResponseObserver).onCompleted();

        assertEquals("test-id", itemCaptor.getValue().getId());
    }

    @Test
    void testGetItem_NotFound() {
        GetItemRequest request = GetItemRequest.newBuilder().setId("missing-id").build();

        when(itemComponent.getItem("missing-id")).thenThrow(new ItemNotFoundException("Item not found"));

        itemGrpcService.getItem(request, itemResponseObserver);

        verify(itemComponent).getItem("missing-id");
        verify(itemResponseObserver).onError(throwableCaptor.capture());

        assertTrue(throwableCaptor.getValue() instanceof StatusRuntimeException);
        StatusRuntimeException exception = (StatusRuntimeException) throwableCaptor.getValue();
        assertEquals(io.grpc.Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    @Test
    void testUpdateItem_Success() {
        UpdateItemRequest request = UpdateItemRequest.newBuilder()
                .setId("test-id")
                .setName("Updated Item")
                .setDescription("Updated Description")
                .setPrice(109.99)
                .build();

        Item updatedItem = Item.newBuilder()
                .setId("test-id")
                .setName("Updated Item")
                .setDescription("Updated Description")
                .setPrice(109.99)
                .build();

        when(itemComponent.updateItem(request)).thenReturn(updatedItem);

        itemGrpcService.updateItem(request, itemResponseObserver);

        verify(itemComponent).updateItem(request);
        verify(itemResponseObserver).onNext(itemCaptor.capture());
        verify(itemResponseObserver).onCompleted();

        assertEquals("Updated Item", itemCaptor.getValue().getName());
    }

    @Test
    void testUpdateItem_NotFound() {
        UpdateItemRequest request = UpdateItemRequest.newBuilder().setId("missing-id").build();

        when(itemComponent.updateItem(request)).thenThrow(new ItemNotFoundException("Item not found"));

        itemGrpcService.updateItem(request, itemResponseObserver);

        verify(itemComponent).updateItem(request);
        verify(itemResponseObserver).onError(throwableCaptor.capture());

        assertTrue(throwableCaptor.getValue() instanceof StatusRuntimeException);
        assertEquals(io.grpc.Status.Code.NOT_FOUND, ((StatusRuntimeException) throwableCaptor.getValue()).getStatus().getCode());
    }

    @Test
    void testDeleteItem_Success() {
        DeleteItemRequest request = DeleteItemRequest.newBuilder().setId("test-id").build();

        when(itemComponent.deleteItem("test-id")).thenReturn(DeleteItemResponse.newBuilder().setSuccess(true).build());

        itemGrpcService.deleteItem(request, deleteResponseObserver);

        verify(itemComponent).deleteItem("test-id");
        verify(deleteResponseObserver).onNext(deleteCaptor.capture());
        verify(deleteResponseObserver).onCompleted();

        assertTrue(deleteCaptor.getValue().getSuccess());
    }

    @Test
    void testDeleteItem_NotFound() {
        DeleteItemRequest request = DeleteItemRequest.newBuilder().setId("missing-id").build();

        when(itemComponent.deleteItem("missing-id")).thenThrow(new ItemNotFoundException("Item not found"));

        itemGrpcService.deleteItem(request, deleteResponseObserver);

        verify(itemComponent).deleteItem("missing-id");
        verify(deleteResponseObserver).onError(throwableCaptor.capture());

        assertTrue(throwableCaptor.getValue() instanceof StatusRuntimeException);
        assertEquals(io.grpc.Status.Code.NOT_FOUND, ((StatusRuntimeException) throwableCaptor.getValue()).getStatus().getCode());
    }

    @Test
    void testListItems() {
        ListItemsRequest request = ListItemsRequest.newBuilder().setNameContains("Test").build();
        ListItemsResponse response = ListItemsResponse.newBuilder().addItems(stubItem).build();

        when(itemComponent.listItems(request)).thenReturn(response);

        itemGrpcService.listItems(request, listResponseObserver);

        verify(itemComponent).listItems(request);
        verify(listResponseObserver).onNext(response);
        verify(listResponseObserver).onCompleted();
    }
}
