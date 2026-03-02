package com.example.demo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.example.item.v1.Item;

public class ItemMapperTest {

    private final ItemMapper mapper = Mappers.getMapper(ItemMapper.class);

    @Test
    void testToDocument() {
        Item item = Item.newBuilder()
                .setId("12345")
                .setName("Test Item")
                .setDescription("A test description")
                .setPrice(19.99)
                .build();

        ItemDocument document = mapper.toDocument(item);

        assertNotNull(document);
        assertEquals("12345", document.getId());
        assertEquals("Test Item", document.getName());
        assertEquals("A test description", document.getDescription());
        assertEquals(19.99, document.getPrice());
    }

    @Test
    void testToProto() {
        ItemDocument document = new ItemDocument();
        document.setId("67890");
        document.setName("Mapped Item");
        document.setDescription("Another test description");
        document.setPrice(49.50);

        Item item = mapper.toProto(document);

        assertNotNull(item);
        assertEquals("67890", item.getId());
        assertEquals("Mapped Item", item.getName());
        assertEquals("Another test description", item.getDescription());
        assertEquals(49.50, item.getPrice());
    }

    @Test
    void testNullMapping() {
        ItemDocument document = mapper.toDocument(null);
        assertNull(document);

        Item item = mapper.toProto(null);
        assertNull(item);
    }
}
