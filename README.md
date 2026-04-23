# Smart Campus Sensor & Room Management API
A RESTful API built using JAX-RS (Jersey). This API manages Rooms and Sensors across campus buildings.
\
**Technology:** JAX-RS (Jersey 2.35), Jakarta EE 8, Apache Tomcat 9, Maven

---

## API Overview
The API is based on the principles of REST and has a versioned base path of /api/v1. It handles three fundamental resources which include: Rooms, Sensors and Sensor Readings. In-memory data structures (ConcurrentHashMap) are used as the storage medium in the system.

---

## How to Build and Run
### Prerequisites
- Java JDK 11 or higher
- Apache Maven 3.6+
- Apache Tomcat 9.0
- NetBeans IDE (recommended)

### Steps

1. Clone the repository:
git clone [https://github.com/YOURUSERNAME/SmartCampusAPI.git](https://github.com/Ilman-Fazny/SmartCampusAPI)

2. Open the project in NetBeans:
   - File → Open Project → select the cloned folder

3. Build the project:
   - Right-click project → Clean and Build

4. Run the project:
   - Right-click project → Run
   - Tomcat will start automatically

5. The API will be available at:
http://localhost:8080/SmartCampusAPI/api/v1

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl http://localhost:8080/SmartCampusAPI/api/v1
```

### 2. Get All Rooms
```bash
curl http://localhost:8080/SmartCampusAPI/api/v1/rooms
```

### 3. Create a New Room
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"HALL-01\",\"name\":\"Main Hall\",\"capacity\":200}"
```

### 4. Create a Sensor
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"CO2-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":400,\"roomId\":\"LIB-301\"}"
```

### 5. Filter Sensors by Type
```bash
curl http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=CO2
```

### 6. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":450.5}"
```

### 7. Get All Readings for a Sensor
```bash
curl http://localhost:8080/SmartCampusAPI/api/v1/sensors/CO2-001/readings
```

### 8. Try Deleting a Room with Sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```

### 9. Try Creating Sensor with Non-existent Room (422 Error)
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"TEMP-999\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":0,\"roomId\":\"FAKE-999\"}"
```

## Report — Question Answers


### 10. Delete a Room Without Sensors (200 OK)
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/HALL-01
```

---

## Report — Question Answers

### Part 1.2 — Resource Lifecycle 
The default is to have JAX-RS make a new instance of a resource class each time there is a request (per-request lifecycle). This implies that instance variables cannot be utilized to hold any shared state, which is created a new object each request. A Singleton pattern is employed to handle in-memory data shared among all requests safely, through the DataStore class, which contains ConcurrentHashMap structures. ConcurrentHashMap is thread safe, and it would not cause race conditions when two or more requests are accessing and writing simultaneously, no data would be lost.

### Part 1.2 — HATEOAS
HATEOAS (Hypermedia as the Engine of Application State) is that API responses not only contain data, but also links to other related resources and actions available. This is said to be advanced REST design since the clients do not require to hard-code or memorise the URLs but find their way around through responses. This minimizes dependency between clients and servers, i.e. the server can alter URL structures without breaking clients and new developers can learn the API by just following links in responses without having to read disjointed static documentation.

### Part 2.1 Returning IDs vs Full Objects
Returning IDs alone consumes less bandwidth over the network and is quicker when large collections are involved, but requires a client to make more requests to get information about each room which adds up to more round-trips. Sending full room objects also makes the payload size bigger, yet it decreases client-side processing and the number of HTTP calls required. Depending on the use case, the most appropriate response is to return the full object — when the dashboards require all the details; to have very large collections, pagination with full objects or return IDs and allow the clients to demand the details are better.

### Part 2.2 — DELETE Idempotency
Yes, in this implementation, DELETE is an idempent operation. The initial DELETE request will delete the room and will respond with 200 OK. Further DELETE requests with the same room ID will give a 404 Not Found as the room is already deleted. Idempotency implies that the state of the server becomes the same after either one or multiple identical requests- in both instances the room is not in the system. The code of the response can be different (200 vs 404), yet the result is the same, which meets the idempotency requirement of the HTTP specification.

### Part 3.1 — @Consumes Mismatch
When a client sends a request with Content-Type: text/plain or application/xml to the endpoint with the annotation of @Consumes(Mediatype.APPLICATION_JSON), JAX-RS will automatically respond with an HTTP 415 Unsupported Media Type. The request is never passed to the resource method - the JAX-RS runtime refuses it at the framework level before any application code is run. This prevents a situation where the API is subjected to unforeseen input formats and prevents errors during JSON parsing.

### Part 3.2 — Query Param vs Path Param to Filter
Query parameters (/sensors?type=CO2) can be used to optionally filter and search a collection - they do not alter the identity of the resource being accessed. Path parameters (/sensors/type/CO2) suggest that CO2 is a separate sub-resource, which is not semantically correct as we are filtering the same sensors collection. Parameters may also be optional and easily combined (e.g. ?type=CO2&status=ACTIVE) and thus much more adaptable to search and filter actions.

### Part 4.1.1 — Sub-Resource Locator Pattern
The Sub-Resource Locator pattern transfers the work of dealing with nested paths to specialized classes. Each class has only one responsibility, rather than defining all the paths in a huge controller. This simplifies the process of maintaining, testing and extending the codebase. An example is SensorReadingResource which does not deal with sensor management at all because it only deals with reading logic, whereas sensor management is considered in SensorResource. When APIs are frequently nested and have many resources, this pattern can help controllers avoid becoming uncontrollably large and breaking the Single Responsibility Principle.

### Part 5.2 — HTTP 422 vs 404
A 404 Not Found indicates that the URL/resource is not available on the server. Nonetheless, in the case of a client POSTing a new sensor with a roomId that does not exist, the URL (/api/v1/sensors) remains valid - the issue lies in the body of the request itself where there is a reference to a resource (roomId) that is not in existence. The semantic reason is that HTTP 422 Unprocessable Entity is more appropriate since it indicates that the request was correct and reached the correct endpoint, yet the content was semantically invalid as it contained a broken reference.

### Part 5.4 - Stack Trace Security Risk
Disclosing Java stack traces to external API consumers is a severe security threat since it discloses: internal package and class names (assisting attackers to map the application structure), framework and library versions (enabling targeted known-vulnerability attacks), file names and line numbers (revealing where to find the weaknesses precisely), and business logic flow (by revealing how the application processes data). This is prevented by a global exception mapper which intercepts all the unexpected errors and sends a generic 500 message only.

### Part 5.5 - Filters vs Inline Logging
It is based on the DRY (Don't Repeat Yourself) principle to use JAX-RS filters to cross-cut across concerns, such as logging. Assuming that logging were manually added to each resource method, a change to the log formatting would involve changes to dozens of methods. The filters are automatically executed on each individual request and response and on any subsequent endpoint, making them consistently and fully observable with no chance of forgetting to add tracing to a new method. This also maintains resource methods clean and business logic-only.


---

## Project Structure
```
SmartCampusAPI/
├── src/main/java/com/smartcampus/
│   ├── application/SmartCampusApplication.java
│   ├── model/
│   │   ├── Room.java
│   │   ├── Sensor.java
│   │   └── SensorReading.java
│   ├── store/DataStore.java
│   ├── resource/
│   │   ├── DiscoveryResource.java
│   │   ├── RoomResource.java
│   │   ├── SensorResource.java
│   │   └── SensorReadingResource.java
│   ├── exception/
│   │   ├── RoomNotEmptyException.java
│   │   ├── LinkedResourceNotFoundException.java
│   │   ├── SensorUnavailableException.java
│   │   ├── RoomNotEmptyExceptionMapper.java
│   │   ├── LinkedResourceNotFoundExceptionMapper.java
│   │   ├── SensorUnavailableExceptionMapper.java
│   │   └── GlobalExceptionMapper.java
│   └── filter/LoggingFilter.java
└── src/main/webapp/WEB-INF/
└── web.xml
```


