# 🏫 Smart Campus Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures  
**Student:** H.K.S.Wijetunge | 20232213 / w2121124  

---

## 📌 API Overview

This is a RESTful API built with **JAX-RS (Jersey)** and an embedded **Grizzly HTTP server**.  
It manages university campus Rooms and Sensors with full CRUD operations, sub-resource nesting, 
and advanced error handling. All data is stored in-memory using `ConcurrentHashMap`.

## Resource Hierarchy

/api/v1/
├── rooms/
│   ├── GET    - List all rooms
│   ├── POST   - Create a room
│   └── {roomId}/
│       ├── GET    - Get a specific room
│       └── DELETE - Delete a room (blocked if sensors exist)
└── sensors/
├── GET    - List all sensors (filterable by ?type=)
├── POST   - Register a sensor
└── {sensorId}/
├── GET    - Get a specific sensor
└── readings/
├── GET  - Get reading history
└── POST - Add a new reading

## 🛠️ Tech Stack

- Java 11
- JAX-RS 2.1 (Jersey 2.41)
- Grizzly HTTP Server (embedded)
- Jackson (JSON serialisation)
- Maven (build tool)

---

## 🚀 How to Build & Run

### Prerequisites
- Java 11+ installed
- Maven 3.6+ installed

### Build
```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
mvn clean package
```

### Run
```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The server will start at: **http://localhost:8080/api/v1/**

Press `ENTER` in the terminal to stop the server.

---

## 🧪 Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### 3. Get All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Create a Sensor (linked to LIB-301)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

### 5. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 6. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'
```

### 7. Get Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/CO2-001/readings
```

### 8. Try Deleting a Room with Sensors (expects 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 9. Try Creating a Sensor with Invalid Room (expects 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

### 10. Post Reading to MAINTENANCE Sensor (expects 403)
```bash
# First create and set a sensor to MAINTENANCE, then:
curl -X POST http://localhost:8080/api/v1/sensors/MAINT-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":25.0}'
```

---

## 📝 Report — Question Answers

### Part 1.1 — JAX-RS Resource Class Lifecycle

By default, JAX-RS creates a **new instance of each Resource class for every incoming HTTP request** (per-request scope). This means the resource class is **not** a singleton. 

This has a critical implication: if data were stored as instance variables inside the resource class, it would be lost after each request. To solve this, a **singleton `DataStore` class** is used — a single shared instance (via `DataStore.getInstance()`) that holds all `ConcurrentHashMap` collections. `ConcurrentHashMap` is used instead of a plain `HashMap` because multiple requests can arrive simultaneously. Regular `HashMap` is not thread-safe and could cause data corruption or `ConcurrentModificationException` under concurrent access. `ConcurrentHashMap` provides thread-safe reads and writes without requiring explicit `synchronized` blocks.

---

### Part 1.2 — HATEOAS (Hypermedia as the Engine of Application State)

HATEOAS is considered the highest maturity level of REST (Richardson Maturity Level 3). Instead of clients needing to hard-code URLs, the API itself returns **navigational links** within each response — telling the client what actions are available next and where to find related resources.

**Benefits over static documentation:**
- Clients can discover endpoints dynamically at runtime, reducing coupling between client and server.
- When the API changes (e.g., a URL is restructured), clients following links automatically adapt without code changes.
- It makes the API self-documenting — a developer can start at `/api/v1/` and navigate to every resource just from the responses.
- Static docs go stale; hypermedia responses are always accurate by definition.

---

### Part 2.1 — Returning IDs vs Full Room Objects

| Approach | Pros | Cons |
|---|---|---|
| **IDs only** | Tiny response payload, fast transfer | Client must make N additional requests to get room details (N+1 problem) |
| **Full objects** | Single request gets all data, less client logic | Larger payload, more bandwidth, may include unnecessary fields |

For a list endpoint (`GET /rooms`), returning **full objects** is usually preferred because facilities managers need to see room names and capacities at a glance. However, for embedded references (e.g., `sensorIds` inside a Room), storing only IDs avoids deeply nested JSON and circular references.

---

### Part 2.2 — Is DELETE Idempotent?

Yes, DELETE is idempotent in this implementation. Idempotency means that making the **same request multiple times produces the same server state** as making it once.

- **First DELETE on `LIB-301`**: Room exists and has no sensors → room is removed → returns `204 No Content`.
- **Second DELETE on `LIB-301`**: Room no longer exists → the code detects `room == null` and returns `204 No Content` again.

The server state after both calls is identical (room is gone). The response code is the same. This satisfies idempotency. This is the correct RESTful behaviour — the client should not be penalised (with a 404) for retrying a DELETE.

---

### Part 3.1 — @Consumes(APPLICATION_JSON) Mismatch

When a client sends a request with `Content-Type: text/plain` or `application/xml` to a method annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS cannot find a matching resource method for that media type. The runtime automatically returns an **HTTP 415 Unsupported Media Type** response before the method body is ever executed. No manual check is needed — this is handled entirely by the JAX-RS content negotiation mechanism. This protects the API from receiving malformed or unexpected data formats.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

| Design | Example | Assessment |
|---|---|---|
| Query parameter | `/sensors?type=CO2` | ✅ Preferred for filtering |
| Path segment | `/sensors/type/CO2` | ❌ Implies a separate resource |

Query parameters are semantically correct for **filtering, sorting, and searching** an existing collection because:
- The base resource (`/sensors`) remains the same regardless of filters.
- Filters are **optional** — `@QueryParam` naturally handles the absence of the parameter (returns `null`), while a path segment would require a completely separate route.
- Multiple filters compose naturally: `/sensors?type=CO2&status=ACTIVE`.
- Path segments imply a unique resource identity — `/sensors/type/CO2` falsely implies "CO2" is a sensor ID or a distinct sub-resource, which is semantically incorrect.

---

### Part 4.1 — Sub-Resource Locator Pattern Benefits

The Sub-Resource Locator pattern delegates handling of nested paths to a dedicated class. In this project, `/sensors/{sensorId}/readings` is handled by `SensorReadingResource`, not by `SensorResource` itself.

**Benefits:**
- **Separation of concerns**: Each class handles one resource type, making code easier to read, test, and maintain.
- **Scalability**: As the API grows (e.g., adding `/readings/{readingId}/alerts`), new sub-resource classes can be added without modifying existing ones.
- **Avoids "God class" problem**: Putting every nested path in one massive class creates thousands of lines that are impossible to navigate.
- **Reusability**: A sub-resource class can potentially be reused across different parent paths.
- **JAX-RS flexibility**: The locator method can inject contextual data (the `sensorId`) into the sub-resource constructor, keeping the child class context-aware without global state.

---

### Part 5.2 — Why 422 is More Accurate than 404

- **404 Not Found** means the requested URL/resource endpoint itself does not exist.
- **422 Unprocessable Entity** means the request URL is valid and the JSON payload is syntactically correct, but the **semantic content is invalid** — in this case, the `roomId` field references a room that doesn't exist in the system.

Using 404 would be misleading because the `/sensors` endpoint does exist. The problem is not a missing endpoint — it's a broken reference inside a valid request body. HTTP 422 communicates precisely: "I understood your request, I can parse it, but I cannot process it because of a logical error in the data."

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to API consumers is a serious security risk:

1. **Technology fingerprinting**: The trace reveals the exact framework, library versions, and language (e.g., `jersey-server-2.41`, `java.lang`), allowing attackers to look up known CVEs for those exact versions.
2. **Internal path disclosure**: Stack traces show the full package structure and class names (e.g., `com.smartcampus.store.DataStore`), revealing the application's architecture.
3. **Logic exposure**: The sequence of method calls in a trace reveals business logic and data flow, helping attackers understand how to craft malicious inputs.
4. **Line number targeting**: Exact file names and line numbers help attackers identify where vulnerabilities exist in the source code.

The `GlobalExceptionMapper` mitigates this by logging the trace **server-side only** and returning a generic, uninformative 500 message to the client.

---

## 📁 Project Structure

smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
├── Main.java
├── SmartCampusApplication.java
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── store/
│   └── DataStore.java
├── resource/
│   ├── DiscoveryResource.java
│   ├── RoomResource.java
│   ├── SensorResource.java
│   └── SensorReadingResource.java
└── exception/
├── RoomNotEmptyException.java
├── RoomNotEmptyExceptionMapper.java
├── LinkedResourceNotFoundException.java
├── LinkedResourceNotFoundExceptionMapper.java
├── SensorUnavailableException.java
├── SensorUnavailableExceptionMapper.java
└── GlobalExceptionMapper.java
