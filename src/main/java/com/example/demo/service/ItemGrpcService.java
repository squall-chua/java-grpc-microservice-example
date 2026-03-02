package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.component.ItemComponent;
import com.example.demo.exception.ItemNotFoundException;
import com.example.item.v1.CreateItemRequest;
import com.example.item.v1.DeleteItemRequest;
import com.example.item.v1.DeleteItemResponse;
import com.example.item.v1.GetItemRequest;
import com.example.item.v1.Item;
import com.example.item.v1.ItemServiceGrpc;
import com.example.item.v1.ListItemsRequest;
import com.example.item.v1.ListItemsResponse;
import com.example.item.v1.UpdateItemRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class ItemGrpcService extends ItemServiceGrpc.ItemServiceImplBase {

    private final ItemComponent itemComponent;

    @Autowired
    public ItemGrpcService(ItemComponent itemComponent) {
        this.itemComponent = itemComponent;
    }

    @Override
    public void createItem(CreateItemRequest request, StreamObserver<Item> responseObserver) {
        responseObserver.onNext(itemComponent.createItem(request));
        responseObserver.onCompleted();
    }

    @Override
    public void getItem(GetItemRequest request, StreamObserver<Item> responseObserver) {
        try {
            responseObserver.onNext(itemComponent.getItem(request.getId()));
            responseObserver.onCompleted();
        } catch (ItemNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void updateItem(UpdateItemRequest request, StreamObserver<Item> responseObserver) {
        try {
            responseObserver.onNext(itemComponent.updateItem(request));
            responseObserver.onCompleted();
        } catch (ItemNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deleteItem(DeleteItemRequest request, StreamObserver<DeleteItemResponse> responseObserver) {
        try {
            responseObserver.onNext(itemComponent.deleteItem(request.getId()));
            responseObserver.onCompleted();
        } catch (ItemNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listItems(ListItemsRequest request, StreamObserver<ListItemsResponse> responseObserver) {
        responseObserver.onNext(itemComponent.listItems(request));
        responseObserver.onCompleted();
    }
}
