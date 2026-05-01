# Smart Campus API

A RESTful API built with JAX-RS (Jersey) and an embedded Grizzly HTTP server for managing university campus Rooms, Sensors, and Sensor Readings. Built for module **5COSC022W — Client-Server Architectures**.

---

## API Design Overview

The Smart Campus API follows REST principles and is organised around three core resources:

- **Rooms** — Physical spaces on campus (labs, lecture halls, study rooms)
- **Sensors** — Devices installed in rooms (temperature, CO2, occupancy)
- **Sensor Readings** — Time-stamped measurements recorded by sensors

All data is stored in-memory using HashMap and ArrayList. The API uses JSON for all request and response bodies. Every endpoint lives under the base path /api/v1.

### Architecture
- **Framework:** JAX-RS 2.x with Jersey 2.41
- **Server:** Grizzly embedded HTTP server (no external server needed)
- **JSON:** Jackson via jersey-media-json-jackson
- **Build:** Maven with maven-shade-plugin for fat JAR
- **Storage:** In-memory singleton DataStore class

---

## Build and Run Instructions

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Step 1 — Clone the repository
git clone https://github.com/mathushihanratnatheepan/smart-campus-api.git
cd smart-campus-api

### Step 2 — Build the fat JAR
mvn clean package

### Step 3 — Start the server
java -jar target/smart-campus-api.jar

### Step 4 — Test the API
Open browser at http://localhost:8080/api/v1

### Step 5 — Stop the server
Press ENTER in the terminal where the server is running.

---

## Sample curl Commands

### 1. Discovery
curl -X GET http://localhost:8080/api/v1

### 2. Get all rooms
curl -X GET http://localhost:8080/api/v1/rooms

### 3. Create a room
curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d '{"id":"CONF-01","name":"Conference Room","capacity":20}'

### 4. Get a single room
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301

### 5. Filter sensors by type
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"

### 6. Register a new sensor
curl -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d '{"id":"HUM-001","type":"Humidity","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'

### 7. Post a sensor reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -d '{"value":23.5}'

### 8. Get all readings for a sensor
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings

### 9. Trigger 403 - Post reading to MAINTENANCE sensor
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings -H "Content-Type: application/json" -d '{"value":45.0}'

### 10. Trigger 409 - Delete room with sensors
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

---

## Report Questions

### Part 1.1 — JAX-RS Resource Lifecycle and Thread Safety

JAX-RS resources are request-scoped by default — a new instance of each resource class is created for every incoming HTTP request. This means instance variables are not shared between requests, which avoids race conditions on those fields. However, our DataStore uses static HashMap fields shared across all requests and all resource instances. HashMap is not thread-safe, so concurrent POST requests could corrupt the map. In a production system this would require ConcurrentHashMap or synchronised blocks. For this coursework the risk is acceptable since the API is single-user and not under concurrent load. If @Singleton were used on resource classes, all requests would share one instance, making instance-variable thread safety critical.

### Part 1.2 — HATEOAS and Its Benefits

HATEOAS (Hypermedia As The Engine Of Application State) means the API response itself contains links to related resources, rather than forcing clients to construct URLs from external documentation. Our discovery endpoint returns links to rooms and sensors so a client never needs to hard-code paths. Benefits include reduced coupling, self-documenting API exploration, and resilience to URL changes. Static documentation goes stale; HATEOAS links are always current because they come from the live API.

### Part 2.1 — Returning Only IDs vs Full Objects in a List

Returning only IDs reduces payload size when there are many rooms — the client fetches details on demand. This saves bandwidth but requires multiple round trips, increasing latency. Returning full objects (our approach) costs more bandwidth upfront but allows the client to render a complete table in a single request. For a campus API with few rooms, the full-object approach is more practical. For thousands of records, pagination with ID-only lists would be preferable.

### Part 2.2 — Is DELETE Idempotent?

Yes, DELETE is idempotent in our implementation — the server state is the same after repeated calls (the room is deleted). The HTTP response differs: the first DELETE returns 204 No Content, subsequent calls return 404 Not Found. RFC 7231 defines DELETE as idempotent — the resource ends up deleted regardless of how many times you call it. Our 404 on repeated calls is correct because idempotency refers to side effects, not response codes.

### Part 3.1 — What Happens When a Client Sends text/plain to a JSON Endpoint

When a client sends Content-Type text/plain to an endpoint with @Consumes(APPLICATION_JSON), Jersey checks the content type before invoking the method. Since text/plain does not match application/json, Jersey rejects the request with 415 Unsupported Media Type and never calls the resource method. This is handled by Jersey's content negotiation layer. The client should always set Content-Type application/json when sending a JSON body.

### Part 3.2 — Query Parameter vs Path Segment for Filtering

Using ?type=CO2 as a query parameter is semantically correct because filtering narrows a collection rather than identifying a unique resource. The path /sensors identifies the collection; ?type=CO2 is a modifier on it. Using /sensors/type/CO2 implies type/CO2 is itself a resource, which is misleading. Query parameters are optional by nature, making it trivial to return all sensors when no filter is provided. Query parameters are the REST convention for filtering, sorting, and pagination.

### Part 4.1 — Sub-Resource Locator Pattern vs One Giant Controller

The sub-resource locator pattern delegates routing to a separate class (SensorReadingResource) by returning an instance from a method without an HTTP verb annotation. Benefits include separation of concerns, single responsibility, scalability for new sub-resources, and readability. A giant controller with dozens of methods becomes hard to navigate; separate classes are self-contained. The locator pattern is the JAX-RS-idiomatic way to model hierarchical resources.

### Part 5.1 — HTTP 422 vs 404 When a roomId Reference Is Missing

A 404 means the requested URL does not exist. But when a client POSTs valid JSON to /sensors with a roomId that does not exist, the URL is valid — the problem is inside the request body. The JSON is syntactically correct but semantically invalid. 422 Unprocessable Entity is more accurate because it signals the server parsed the JSON but the data failed business logic validation. Using 404 would mislead clients into thinking the endpoint itself does not exist.

### Part 5.2 — What Attackers Can Extract From a Java Stack Trace

A stack trace exposes class names, package structure, method names, line numbers, framework versions (which can be cross-referenced with known CVEs), server technology, and absolute file paths. This dramatically reduces effort needed to craft targeted exploits. Our GlobalExceptionMapper catches all unhandled Throwable instances and returns a generic 500 Internal Server Error without internal detail, ensuring attackers learn nothing from error responses.

### Part 5.3 — Why Use JAX-RS Filters for Logging Instead of Logger.info() in Every Method

Placing Logger.info() in every method violates DRY — if the log format changes, every method must be updated. It also violates separation of concerns. A JAX-RS ContainerRequestFilter and ContainerResponseFilter are cross-cutting concerns applied automatically to every request and response without touching resource code. Logging is guaranteed, the format is consistent, resource classes stay clean, and the filter can be enabled or disabled centrally. This is the same principle behind Aspect-Oriented Programming.

---

## Author

**Mathushihan Ratnatheepan**
Module: 5COSC022W — Client-Server Architectures
