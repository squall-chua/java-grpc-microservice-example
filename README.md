# Java gRPC Microservice Example

A complete Spring Boot Java application demonstrating modern microservice architecture utilizing gRPC, Protocol Buffers, and robust backend technologies. 

## 🚀 Features

*   **gRPC Core**: High-performance RPC handling leveraging compiled Protobuf models for strong typing and binary efficiency.
*   **gRPC-JSON Transcoding**: Automatically serves traditional RESTful JSON endpoints routed natively directly to their gRPC counterparts through protocol-buffer HTTP annotations.
*   **Spring Boot**: Framework for rapid Dependency Injection and enterprise-grade component management.
*   **MongoDB & Spring Data MongoDB**: Persistent NoSQL data layer efficiently tracking document state.
*   **MapStruct**: High-performance mapping library connecting Protobuf generated classes effortlessly to inner-domain Entity classes (and vice-versa).
*   **Security (JWT & RBAC)**: Intercepts unauthenticated payload requests requiring Authorization Bearer Tokens validating scope and role-based access. 
*   **Solid Architecture Principles**: Maintains single responsibility patterns separating pure delegation endpoint layers (`ItemGrpcService`) from the embedded domain logic layer (`ItemComponent`).
*   **Unit & Integration Tested**: Asserts edge-case validations through robust isolated unit testing (`JUnit 5`, `Mockito`) intercepting dependencies, alongside database mapping assertion using Embedded MongoDB (`Flapdoodle`).

## 🛠️ Tech Stack

*   **Java**: 17+ 
*   **Framework**: Spring Boot 3+
*   **RPC Framework**: gRPC (`grpc-spring-boot-starter`)
*   **Database**: MongoDB (`spring-boot-starter-data-mongodb`)
*   **Mappers**: MapStruct
*   **Build Tool**: Gradle 
*   **Testing**: JUnit 5, Mockito, Flapdoodle Embedded MongoDB

## 📂 Architecture

```text
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── component/           # Isolated business logic and operations (ItemComponent)
│   │   ├── exception/           # Custom boundary exceptions mapping to gRPC statuses
│   │   ├── repository/          # MongoDB interfaces, mapping logic (ItemMapper), and Entities (ItemDocument)
│   │   ├── security/            # gRPC interceptors parsing scopes and valid JWT headers
│   │   ├── service/             # Protocol Buffer generated delegation endpoints (ItemGrpcService)
│   │   └── DemoApplication.java # Spring Boot entry-point
│   └── proto/                   # .proto schemas describing endpoints and DTOs 
└── test/
    └── java/com/example/demo/   # Parallel test architecture encompassing unit and integration coverage.
```

## ⚙️ Getting Started

### Prerequisites
*   JDK 17 or higher
*   A running MongoDB instance on localhost (`mongodb://localhost:27017/grpc-demo`), or configured accordingly.

### Building the Project

Compile the source, generate the Protobuf stubs, and run tests via Gradle wrapper:
```bash
./gradlew build
```

### Running Tests

Execute the complete unit and integration testing suite utilizing the embedded MongoDB and Mockito context:
```bash
./gradlew test
```

### Running the Application

To construct the underlying `.proto` files dynamically and boot the application cleanly:
```bash
./gradlew bootRun
```

The application will launch on its designated server ports handling concurrent gRPC communication.

### Manual Testing with cURL

The application provides a gRPC-JSON transcoding API on HTTP port `8080`. To access these endpoints, you must provide a valid JWT Bearer token in the `Authorization` header.

#### 1. Generate a JWT Token

You can generate a test token at [jwt.io](https://jwt.io/):
*   **Algorithm:** HS256
*   **Payload:**
    ```json
    {
      "sub": "testuser",
      "scopes": "read:items write:items",
      "roles": "admin editor"
    }
    ```
*   **Secret (from `application.yml`):** `super-secret-key-that-is-at-least-32-bytes`

Export this token to an environment variable in your terminal:
```bash
export TOKEN="your.jwt.token"
```

#### 2. cURL Examples

**Create an Item** (Requires `write:items` scope and `admin` or `editor` role):
```bash
curl -X POST http://localhost:8080/v1/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sample Item",
    "description": "A very nice sample item",
    "price": 19.99
  }'
```

**Get an Item** (Requires `read:items` scope):
```bash
curl http://localhost:8080/v1/items/<item-id> \
  -H "Authorization: Bearer $TOKEN"
```

**List Items** (Requires `read:items` scope):
```bash
curl "http://localhost:8080/v1/items?page_request.page_number=1&page_request.page_size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Update an Item** (Requires `write:items` scope and `admin` or `editor` role):
```bash
curl -X PUT http://localhost:8080/v1/items/<item-id> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "<item-id>",
    "name": "Updated Sample Item",
    "description": "An updated description",
    "price": 25.00
  }'
```

**Delete an Item** (Requires `write:items` scope and `admin` role):
```bash
curl -X DELETE http://localhost:8080/v1/items/<item-id> \
  -H "Authorization: Bearer $TOKEN"
```
