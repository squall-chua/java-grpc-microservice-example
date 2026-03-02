package com.example.demo.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import com.example.item.v1.Item;
import com.example.item.v1.ListItemsRequest;
import com.example.item.v1.ListItemsResponse;
import com.example.item.v1.PageInfo;

@Repository
public class CustomItemRepositoryImpl implements CustomItemRepository {

    private final MongoTemplate mongoTemplate;
    private final ItemMapper itemMapper;

    @Autowired
    public CustomItemRepositoryImpl(MongoTemplate mongoTemplate, ItemMapper itemMapper) {
        this.mongoTemplate = mongoTemplate;
        this.itemMapper = itemMapper;
    }

    @Override
    public ListItemsResponse searchItems(ListItemsRequest request) {
        List<Criteria> criteriaList = new java.util.ArrayList<>();
        if (!request.getNameContains().isEmpty()) {
            criteriaList.add(Criteria.where("name").regex(".*" + request.getNameContains() + ".*", "i"));
        }
        if (request.getMinPrice() != 0.0 && request.getMaxPrice() != 0.0) {
             criteriaList.add(Criteria.where("price").gte(request.getMinPrice()).lte(request.getMaxPrice()));
        } else if (request.getMinPrice() != 0.0) {
             criteriaList.add(Criteria.where("price").gte(request.getMinPrice()));
        } else if (request.getMaxPrice() != 0.0) {
             criteriaList.add(Criteria.where("price").lte(request.getMaxPrice()));
        }

        Criteria criteria = new Criteria();
        if (!criteriaList.isEmpty()) {
             criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // Pagination
        int pageNumber = request.hasPageRequest() && request.getPageRequest().getPageNumber() > 0
                ? request.getPageRequest().getPageNumber()
                : 1;
        int pageSize = request.hasPageRequest() && request.getPageRequest().getPageSize() > 0
                ? request.getPageRequest().getPageSize()
                : 10;
        long skip = (long) (pageNumber - 1) * pageSize;

        // Build the facet
        FacetOperation facet = Aggregation.facet()
                .and(
                        Aggregation.skip(skip),
                        Aggregation.limit(pageSize))
                .as("paginatedResults")
                .and(
                        Aggregation.count().as("totalCount"))
                .as("totalCount");

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                facet);

        AggregationResults<FacetResult> results = mongoTemplate.aggregate(aggregation, "items", FacetResult.class);

        ListItemsResponse.Builder responseBuilder = ListItemsResponse.newBuilder();

        if (!results.getMappedResults().isEmpty()) {
            FacetResult facetResult = results.getMappedResults().get(0);

            List<ItemDocument> paginatedResults = facetResult.getPaginatedResults();
            long totalCount = 0;
            if (facetResult.getTotalCount() != null && !facetResult.getTotalCount().isEmpty()) {
                totalCount = facetResult.getTotalCount().get(0).getTotalCount();
            }

            List<Item> items = paginatedResults.stream()
                    .map(itemMapper::toProto)
                    .collect(Collectors.toList());

            responseBuilder.addAllItems(items);

            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            responseBuilder.setPageInfo(PageInfo.newBuilder()
                    .setPageNumber(pageNumber)
                    .setPageSize(pageSize)
                    .setTotalPages(totalPages)
                    .setTotalItems((int) totalCount)
                    .build());
        } else {
            responseBuilder.setPageInfo(PageInfo.newBuilder()
                    .setPageNumber(pageNumber)
                    .setPageSize(pageSize)
                    .setTotalPages(0)
                    .setTotalItems(0)
                    .build());
        }

        return responseBuilder.build();
    }

    // Helper classes for parsing MongoDB Facet results
    public static class FacetResult {
        private List<ItemDocument> paginatedResults = new ArrayList<>();
        private List<CountResult> totalCount = new ArrayList<>();

        public List<ItemDocument> getPaginatedResults() {
            return paginatedResults;
        }

        public void setPaginatedResults(List<ItemDocument> paginatedResults) {
            this.paginatedResults = paginatedResults;
        }

        public List<CountResult> getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(List<CountResult> totalCount) {
            this.totalCount = totalCount;
        }
    }

    public static class CountResult {
        private long totalCount;

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }
    }
}
