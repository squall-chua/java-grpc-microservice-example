package com.example.demo.repository;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.example.item.v1.ListItemsRequest;
import com.example.item.v1.ListItemsResponse;
import com.example.item.v1.PageRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataMongoTest
@Import(CustomItemRepositoryImplTest.Config.class)
public class CustomItemRepositoryImplTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class Config {
        @Bean
        public ItemMapper itemMapper() {
            return Mappers.getMapper(ItemMapper.class);
        }
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CustomItemRepositoryImpl customItemRepository;

    @BeforeEach
    void setUp() {
        seedData();
    }

    @AfterEach
    void tearDown() {
        itemRepository.deleteAll();
    }

    private void seedData() {
        // Create 5 test items
        ItemDocument t1 = new ItemDocument();
        t1.setName("Apple iPhone 14");
        t1.setDescription("Latest smartphone");
        t1.setPrice(999.0);

        ItemDocument t2 = new ItemDocument();
        t2.setName("Samsung Galaxy S23");
        t2.setDescription("Android smartphone");
        t2.setPrice(899.0);

        ItemDocument t3 = new ItemDocument();
        t3.setName("Apple MacBook Pro");
        t3.setDescription("Powerful laptop");
        t3.setPrice(1999.0);

        ItemDocument t4 = new ItemDocument();
        t4.setName("Sony Headphones");
        t4.setDescription("Noise cancelling");
        t4.setPrice(349.0);

        ItemDocument t5 = new ItemDocument();
        t5.setName("Generic Case");
        t5.setDescription("Silicone case for phones");
        t5.setPrice(19.0);

        itemRepository.saveAll(List.of(t1, t2, t3, t4, t5));
    }

    @Test
    void testSearchItems_EmptyRequest_ReturnsAllPaginated() {
        ListItemsRequest request = ListItemsRequest.newBuilder().build();

        ListItemsResponse response = customItemRepository.searchItems(request);

        assertEquals(5, response.getItemsCount());
        assertEquals(5, response.getPageInfo().getTotalItems());
        assertEquals(1, response.getPageInfo().getTotalPages());
        assertEquals(1, response.getPageInfo().getPageNumber());
        assertEquals(10, response.getPageInfo().getPageSize()); // Default page size
    }

    @Test
    void testSearchItems_NameContains_CaseInsensitive() {
        ListItemsRequest request = ListItemsRequest.newBuilder()
                .setNameContains("apple")
                .build();

        ListItemsResponse response = customItemRepository.searchItems(request);

        assertEquals(2, response.getItemsCount());
        assertEquals(2, response.getPageInfo().getTotalItems());
        
        boolean allApples = response.getItemsList().stream()
                 .allMatch(item -> item.getName().toLowerCase().contains("apple"));
        assertTrue(allApples);
    }

    @Test
    void testSearchItems_PriceRange() {
        ListItemsRequest request = ListItemsRequest.newBuilder()
                .setMinPrice(500.0)
                .setMaxPrice(1500.0)
                .build();

        ListItemsResponse response = customItemRepository.searchItems(request);

        assertEquals(2, response.getItemsCount()); // Apple iPhone 14 and Samsung Galaxy S23
        assertEquals(2, response.getPageInfo().getTotalItems());
    }

    @Test
    void testSearchItems_CombinedFilters() {
        ListItemsRequest request = ListItemsRequest.newBuilder()
                .setNameContains("Phone")
                .setMaxPrice(900.0)
                .build();

        ListItemsResponse response = customItemRepository.searchItems(request);

        // Should return ONLY "Samsung Galaxy S23" since "Sony Headphones" isn't filtered
        // WAIT: "Sony Headphones" contains "phone"
        // Let's check expectations:
        // Samsung Galaxy S23 (899.0) -> No "phone" in name? Wait name is "Samsung Galaxy S23", no "phone" there.
        // Wait, "Apple iPhone 14" (999.0) contains "Phone", but > 900.0.
        // What contains "Phone"? "Apple iPhone 14" and "Sony Headphones".
        // "Apple iPhone 14" price is 999.0 (exceeds max 900.0).
        // "Sony Headphones" price is 349.0 (within max 900.0).
        assertEquals(1, response.getItemsCount());
        assertEquals("Sony Headphones", response.getItems(0).getName());
    }

    @Test
    void testSearchItems_CustomPagination() {
        ListItemsRequest request = ListItemsRequest.newBuilder()
                .setPageRequest(PageRequest.newBuilder()
                        .setPageNumber(1)
                        .setPageSize(2)
                        .build())
                .build();

        ListItemsResponse response = customItemRepository.searchItems(request);

        assertEquals(2, response.getItemsCount());
        assertEquals(5, response.getPageInfo().getTotalItems());
        assertEquals(3, response.getPageInfo().getTotalPages()); // 5 / 2 = 2.5 -> ceil -> 3

        // Page 2
        ListItemsRequest request2 = ListItemsRequest.newBuilder()
                .setPageRequest(PageRequest.newBuilder()
                        .setPageNumber(2)
                        .setPageSize(2)
                        .build())
                .build();

        ListItemsResponse response2 = customItemRepository.searchItems(request2);

        assertEquals(2, response2.getItemsCount());

        // Page 3
        ListItemsRequest request3 = ListItemsRequest.newBuilder()
                .setPageRequest(PageRequest.newBuilder()
                        .setPageNumber(3)
                        .setPageSize(2)
                        .build())
                .build();

        ListItemsResponse response3 = customItemRepository.searchItems(request3);

        assertEquals(1, response3.getItemsCount());
    }

    @Test
    void testSearchItems_NoResultsMatch() {
        ListItemsRequest request = ListItemsRequest.newBuilder()
                .setMinPrice(5000.0)
                .build();

        ListItemsResponse response = customItemRepository.searchItems(request);

        assertEquals(0, response.getItemsCount());
        assertEquals(0, response.getPageInfo().getTotalItems());
        assertEquals(0, response.getPageInfo().getTotalPages());
    }
}
