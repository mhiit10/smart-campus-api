# Smart Campus API

A RESTful API built with JAX-RS (Jersey) and Grizzly HTTP server for managing campus rooms, sensors, and sensor readings. Data is stored in-memory using ConcurrentHashMap.

## API Overview

Base URL: `http://localhost:8080/api/v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1 | Discovery endpoint |
| GET | /api/v1/rooms | Get all rooms |
| POST | /api/v1/rooms | Create a room |
| GET | /api/v1/rooms/{id} | Get a room by ID |
| DELETE | /api/v1/rooms/{id} | Delete a room |
| GET | /api/v1/sensors | Get all sensors (optional ?type= filter) |
| POST | /api/v1/sensors | Create a sensor |
| GET | /api/v1/sensors/{id}/readings | Get all readings for a sensor |
| POST | /api/v1/sensors/{id}/readings | Add a reading to a sensor |

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Step 1 - Build
```bash
mvn clean package
```

### Step 2 - Run
```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The API will be available at: `http://localhost:8080/api/v1`

Press ENTER in the terminal to stop the server.

## Sample curl Commands

```bash
# 1. Discovery endpoint
curl http://localhost:8080/api/v1

# 2. Create a room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":30}'

# 3. Get all rooms
curl http://localhost:8080/api/v1/rooms

# 4. Create a sensor
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":400,"roomId":"LIB-301"}'

# 5. Filter sensors by type
curl "http://localhost:8080/api/v1/sensors?type=CO2"

# 6. Add a sensor reading
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":415.5}'

# 7. Get readings for a sensor
curl http://localhost:8080/api/v1/sensors/CO2-001/readings

# 8. Try to delete a room that has sensors (returns 409 Conflict)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

## Part 1 - Questions

### JAX-RS Resource Lifecycle
By default, JAX-RS creates a new instance of a resource class for every incoming HTTP request (per-request lifecycle). This means instance variables are reset on every request and cannot be used to store shared data. To safely manage in-memory data across requests, this project uses a singleton DataStore class backed by ConcurrentHashMap. ConcurrentHashMap is thread-safe, which prevents race conditions when multiple requests read or write data at the same time.

### HATEOAS
Hypermedia as the Engine of Application State (HATEOAS) means embedding navigation links inside API responses so clients can discover available actions dynamically. Instead of relying on static external documentation, clients follow links returned in responses. This reduces coupling between client and server — if a URL changes, clients adapt by following the new link rather than breaking. It makes the API self-documenting and easier for developers to explore and integrate with.

---

## Part 2 - Questions

### Returning IDs vs Full Objects
Returning full room objects in a list gives the client all information in a single request but uses more bandwidth and may transfer unnecessary data. Returning only IDs is lightweight but forces the client to make one additional request per room to fetch its details — this is known as the N+1 problem and increases network round-trips. The best approach depends on the use case; returning full objects is preferred when clients typically need all the data at once.

### DELETE Idempotency
Yes, DELETE is idempotent in this implementation. The first DELETE call successfully removes the room and returns 204 No Content. Any subsequent DELETE call for the same roomId returns 404 Not Found because the room no longer exists. The end state — the room does not exist in the system — is identical regardless of how many times the request is sent, which satisfies the definition of idempotency.

---

## Part 3 - Questions

### @Consumes and Media Type Mismatch
When a method is annotated with @Consumes(MediaType.APPLICATION_JSON), JAX-RS checks the Content-Type header of every incoming request. If a client sends data as text/plain or application/xml instead of application/json, JAX-RS automatically rejects the request with HTTP 415 Unsupported Media Type before the method is even invoked. This protects the endpoint from receiving data it cannot deserialise and keeps validation at the framework level.

### @QueryParam vs Path-Based Filtering
Query parameters such as ?type=CO2 are designed for optional filtering, sorting, and searching of a resource collection. The collection itself (/sensors) remains the resource; the query parameter is simply a modifier on how results are returned. A path-based approach such as /sensors/type/CO2 incorrectly implies that type/CO2 is a distinct resource with its own identity, which is semantically wrong. Query parameters are also more flexible — multiple filters can be combined easily, for example ?type=CO2&status=ACTIVE.

---

## Part 4 - Questions

### Sub-Resource Locator Pattern
The Sub-Resource Locator pattern delegates request handling for nested paths to a separate, focused class. Instead of one massive resource class handling every endpoint, each class has a single responsibility. SensorReadingResource handles all reading logic independently of SensorResource, making each class smaller, easier to read, and easier to test individually. In large APIs with deep nesting, this pattern prevents any single class from growing unmanageably large and keeps the codebase maintainable.

---

## Part 5 - Questions

### HTTP 422 vs 404
A 404 Not Found means the requested URL or endpoint does not exist on the server. A 422 Unprocessable Entity means the server understood the request and the endpoint is valid, but the data inside the request body is semantically invalid. When a client sends a valid POST to /sensors but includes a roomId that references a non-existent room, the endpoint itself exists — the problem is with the content of the payload. HTTP 422 more precisely communicates that the request was understood but could not be processed due to invalid references in the data.

### Stack Trace Security Risks
Exposing Java stack traces to external users reveals sensitive internal details including class names, package structure, library names and versions, and exact line numbers. Attackers can use this information to identify specific framework versions with known security vulnerabilities, understand the internal application architecture to craft targeted attacks, and locate potential injection points. The GlobalExceptionMapper in this project prevents exposure by catching all unhandled exceptions and returning a safe, generic error message instead of the raw stack trace.

### Filters vs Manual Logging
Using JAX-RS filters implements logging as a cross-cutting concern in one single place, following the DRY (Don't Repeat Yourself) principle. Manually adding Logger.info() calls inside every resource method is error-prone — developers may forget to add them to new methods, and any change to the log format requires editing every single method across the codebase. Filters are also more powerful — they can be applied selectively, extended with request IDs for tracing, or replaced entirely without touching any resource class.