package com.example.demo.repository;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import com.example.item.v1.Item;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemMapper {
    ItemDocument toDocument(Item item);

    Item toProto(ItemDocument document);
}
