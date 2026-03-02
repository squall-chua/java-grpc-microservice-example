package com.example.demo.repository;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "items")
public class ItemDocument {
    @Id
    private String id;
    private String name;
    private String description;
    private double price;
}
