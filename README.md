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
