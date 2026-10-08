# RoadRescue Server

RoadRescue Server is the backend application for a real-time roadside assistance platform.

It connects customers who need roadside help with nearby available mechanics, supports mechanic onboarding and approval, manages the complete service-request lifecycle, and provides real-time assignment and location updates through WebSockets.

The backend is built using the custom Sentinal framework stack:

- `rest_fw`
- `persistence_fw`
- `security_fw`

---

## Overview

The backend handles:

- Auth0-based authentication
- local user provisioning
- role-based authorization
- mechanic onboarding and approval
- mechanic availability
- customer service requests
- nearest-mechanic dispatch
- assignment offer timeout
- assignment accept/reject flow
- service lifecycle transitions
- live mechanic GPS updates
- WebSocket broadcasting
- customer request tracking
- notifications
- background jobs
- rate limiting
- object-storage abstraction for mechanic documents

---

## Architecture

```text
Customer React App
Mechanic React App
Admin React App
        |
        v
      Auth0
        |
        v
Sentinal Security Framework
        |
        v
Sentinal REST Framework
        |
        v
RoadRescue Server
        |
        v
Sentinal Persistence Framework
        |
        v
       MySQL
```

Real-time tracking:

```text
Mechanic Browser
      |
      | GPS updates
      | WebSocket
      v
RoadRescue Server
      |
      | WebSocket broadcast
      v
Customer Browser
      |
      v
Leaflet / OpenStreetMap
```

---

## Technology Stack

### Backend

- Java 17
- Gradle
- Tomcat 9
- Javax Servlet API
- Javax WebSocket API
- MySQL
- Jackson
- Auth0
- Java JWT
- JWKS RSA

### Frontend Integration

The backend is designed to work with:

- React
- Vite
- Auth0 React SDK
- Leaflet
- OpenStreetMap
- Browser Geolocation API
- WebSocket API

---

## Project Structure

A typical project layout is:

```text
road_rescue_server/
├── README.md
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       └── java/
└── ...
```

---

## Custom Framework Stack

RoadRescue Server uses three custom frameworks.

```text
security_fw
     |
     v
rest_fw
     |
     v
road_rescue_server
     |
     v
persistence_fw
     |
     v
MySQL
```

### `security_fw`

Responsible for:

- authentication pipeline
- authorization
- request security context
- authenticated principal
- rate limiting hooks

### `rest_fw`

Responsible for:

- HTTP route configuration
- parameter extraction
- type conversion
- Java method invocation

### `persistence_fw`

Responsible for:

- query construction
- criteria
- joins
- row management
- inserts
- updates
- generated ID hydration

### `road_rescue_server`

Responsible for:

- business logic
- domain validation
- user workflows
- mechanic workflows
- service request workflows
- dispatch
- real-time tracking
- notifications
- application-specific security integration

---

## Requirements

- Java 17
- Gradle
- Tomcat 9
- MySQL
- Auth0 tenant and applications
- Sentinal framework dependencies

---

## Build

Run:

```bash
./gradlew clean build
```

The generated application artifact will be available under the configured Gradle build output.

---

## Authentication

RoadRescue uses Auth0 for identity authentication.

The backend validates:

- Bearer access token
- RS256 signing algorithm
- Auth0 signing key through JWKS
- issuer
- API audience
- token subject

Authentication flow:

```text
Frontend
   |
   v
Auth0 Login
   |
   v
Access Token
   |
   v
Auth0AuthenticationProvider
   |
   v
UserIdentity
   |
   v
Users
   |
   v
AuthenticatedPrincipal
```

---

## Local User Identity

External Auth0 identities are linked to local application users through the `UserIdentity` table.

Conceptually:

```text
Auth0 subject
     |
     v
UserIdentity.ExternalSubject
     |
     v
Users.UserID
```

This keeps external authentication identity separate from application-specific user data.

---

## User Provisioning

When an authenticated identity is seen for the first time:

```text
Auth0 subject lookup
       |
       +---- identity exists ----> existing local user
       |
       v
verified email lookup
       |
       +---- local user exists ----> create UserIdentity
       |
       v
create CUSTOMER user
       |
       v
create UserIdentity
```

A newly provisioned normal user starts as:

```text
CUSTOMER
```

---

## Roles

The application currently supports:

```text
CUSTOMER       = 1
MECHANIC       = 2
PLATFORM_ADMIN = 3
```

Role information is stored in the local user model and exposed through the authenticated principal.

---

## Authorization

The backend uses application-specific authorization rules on top of `security_fw`.

Typical categories include:

```text
/api/admin/*
    -> PLATFORM_ADMIN

/api/mechanic/*
    -> MECHANIC where applicable

/api/mechanic/application
    -> authenticated user

/api/me
    -> authenticated user
```

Resource ownership checks are performed in application logic where access depends on specific database rows.

---

## Current User

The current authenticated user can be exposed through:

```text
GET /api/me
```

A typical response can include:

```json
{
  "user_id": 42,
  "name": "Example User",
  "email": "user@example.com",
  "role": "mechanic",
  "role_formatted": "Mechanic",
  "mechanic_id": 8,
  "mechanic_application_status": "approved",
  "mechanic_application_status_formatted": "Approved",
  "availability_status": "available",
  "availability_status_formatted": "Available"
}
```

The server derives identity from the authenticated security context rather than trusting `user_id` from the client.

---

## Mechanic Onboarding

Authentication and mechanic approval are separate concerns.

A user can authenticate successfully without being an approved mechanic.

Mechanic application states:

```text
DRAFT
PENDING
APPROVED
REJECTED
```

Typical flow:

```text
Authenticated User
      |
      v
Create Mechanic Application
      |
      v
DRAFT
      |
      v
Submit Application
      |
      v
PENDING
      |
      +---- REJECTED
      |
      v
APPROVED
      |
      v
Users.UserType = MECHANIC
      |
      v
Mechanic row created
```

---

## Mechanic Documents

Mechanic documents are stored using an object-storage abstraction.

```java
public interface ObjectStorageProvider
{
    void upload(
        String objectKey,
        InputStream inputStream,
        long contentLength,
        String contentType
    ) throws Exception;

    InputStream download(
        String objectKey
    ) throws Exception;

    void delete(
        String objectKey
    ) throws Exception;

    boolean exists(
        String objectKey
    ) throws Exception;
}
```

This allows the application to start with local storage and later move to S3-compatible storage such as MinIO or AWS S3.

Sensitive object keys should not be exposed directly to the frontend.

---

## Mechanic Availability

Mechanics use availability states such as:

```text
AVAILABLE
BUSY
OFFLINE
RESERVED
```

### AVAILABLE

The mechanic can receive a new assignment.

### RESERVED

An assignment offer has been created and is waiting for the mechanic's response.

### BUSY

The mechanic accepted a service request and is handling it.

### OFFLINE

The mechanic is not available for dispatch.

---

## Service Request Lifecycle

A service request moves through the following states:

```text
PENDING
   |
   v
ASSIGNED
   |
   v
ACCEPTED
   |
   v
ON_THE_WAY
   |
   v
IN_PROGRESS
   |
   v
COMPLETED
```

A request may also become:

```text
CANCELLED
```

---

## Mechanic Assignment Lifecycle

A mechanic assignment represents one offer of a service request to a mechanic.

```text
PENDING
   |
   +--------> REJECTED
   |
   +--------> CANCELLED
   |
   v
ACCEPTED
   |
   v
COMPLETED
```

A single service request can have multiple assignment attempts.

Example:

```text
ServiceRequest 100

Assignment 1 -> REJECTED
Assignment 2 -> CANCELLED
Assignment 3 -> ACCEPTED
```

This keeps the overall service-request lifecycle separate from individual mechanic offers.

---

## Creating a Service Request

A customer creates a request through:

```text
POST /api/service-requests
```

The request contains information such as:

```text
vehicle type
service type
description
latitude
longitude
```

The authenticated customer ID is derived from the security context.

The client does not need to submit a trusted `user_id`.

---

## Automatic Mechanic Assignment

After a service request is created, the backend attempts automatic assignment.

The current dispatch strategy is:

```text
ServiceRequest
      |
      v
Find all AVAILABLE mechanics
      |
      v
Ignore mechanics without GPS coordinates
      |
      v
Calculate distance from customer
      |
      v
Ignore mechanics outside configured radius
      |
      v
Choose nearest mechanic
      |
      v
Create MechanicAssignment
      |
      v
Mechanic = RESERVED
      |
      v
ServiceRequest = ASSIGNED
      |
      v
Send WebSocket assignment offer
```

The current implementation intentionally does not filter mechanics through `MechanicService`.

`VehicleType` and `ServiceType` remain on the service request for:

- UI display
- request history
- reporting
- future mechanic specialization matching

---

## Distance Calculation

The dispatcher calculates geographic distance between:

```text
ServiceRequest.Latitude / Longitude
```

and:

```text
Mechanic.Latitude / Longitude
```

using the Haversine formula.

The nearest available mechanic within the configured radius is selected.

---

## Assignment Radius

The assignment radius can be configured using:

```text
ROAD_RESCUE_ASSIGNMENT_RADIUS_KM
```

Example:

```text
25
```

means:

```text
25 km
```

---

## Continuous Dispatch

Immediate assignment alone is not enough because a mechanic may become available after the customer request has already been created.

The backend therefore supports recurring pending-request dispatch.

```text
Customer creates request
      |
      v
No mechanic currently available
      |
      v
ServiceRequest remains PENDING
      |
      v
Recurring dispatcher runs
      |
      v
AVAILABLE mechanic appears
      |
      v
Nearest mechanic selected
      |
      v
Assignment created
```

This allows pending requests to be picked up later without the customer creating a second request.

---

## Assignment Offer Timeout

Assignment offers can expire.

Typical flow:

```text
MechanicAssignment = PENDING
Mechanic           = RESERVED
ServiceRequest     = ASSIGNED
```

If the mechanic does not respond before the configured timeout:

```text
MechanicAssignment -> CANCELLED
Mechanic           -> AVAILABLE
ServiceRequest     -> PENDING
```

The dispatcher can then search for another mechanic.

Configuration:

```text
ROAD_RESCUE_ASSIGNMENT_TIMEOUT_MS
```

Example:

```text
30000
```

means:

```text
30 seconds
```

---

## Reject and Reassignment

If a mechanic rejects an assignment:

```text
Assignment -> REJECTED
Request    -> PENDING
Mechanic   -> AVAILABLE
```

Previously offered mechanics can be excluded from the next assignment attempt for the same request.

This helps the dispatcher move to another candidate instead of immediately selecting the same mechanic again.

---

## Accept Assignment

When a mechanic accepts:

```text
MechanicAssignment -> ACCEPTED
ServiceRequest     -> ACCEPTED
Mechanic           -> BUSY
```

The accepted timestamp is recorded.

---

## Start Journey

The mechanic starts travelling toward the customer.

```text
ServiceRequest
ACCEPTED -> ON_THE_WAY
```

This action is exposed through the mechanic workflow API.

---

## Start Service

When the mechanic reaches the customer and begins work:

```text
ServiceRequest
ON_THE_WAY -> IN_PROGRESS
```

---

## Complete Service

When the mechanic finishes the service:

```text
MechanicAssignment -> COMPLETED
ServiceRequest     -> COMPLETED
Mechanic           -> AVAILABLE
```

The completion timestamp is recorded.

---

## Mechanic Workflow

The overall mechanic workflow is:

```text
Receive Assignment
       |
       v
Accept / Reject
       |
       v
Start Journey
       |
       v
ON_THE_WAY
       |
       v
Start Service
       |
       v
IN_PROGRESS
       |
       v
Complete
       |
       v
COMPLETED
       |
       v
Mechanic AVAILABLE again
```

---

## Real-Time WebSocket Architecture

WebSockets are used for low-latency assignment, location, and request-status events.

The database remains the source of truth.

```text
Database/API
    =
authoritative state

WebSocket
    =
realtime notification and UI updates
```

---

## WebSocket Endpoint

The real-time endpoint is exposed under:

```text
/ws/location
```

When deployed under the `/app` context, the effective path is:

```text
/app/ws/location
```

---

## WebSocket Authentication

The Auth0 access token is not placed directly into the WebSocket URL.

Instead:

```text
Authenticated HTTP Request
        |
        v
POST /api/ws-ticket
        |
        v
Short-lived one-time ticket
        |
        v
WebSocket connection
```

Example connection:

```text
/app/ws/location?ticket=<temporary-ticket>
```

The ticket represents:

- authenticated user ID
- principal type
- roles
- short expiration

---

## WebSocket Ticket

A client first requests:

```text
POST /api/ws-ticket
```

A typical response includes:

```json
{
  "ticket": "temporary-ticket-value",
  "expires_in_ms": 60000
}
```

The ticket is short-lived and intended for one WebSocket connection.

---

## WebSocket Message Format

Realtime messages use key/value pairs through `RealtimeHub.message(...)`.

Example:

```java
RealtimeHub.message(
    "type",
    "assignment_offer",
    "mechanic_assignment_id",
    assignmentID,
    "service_request_id",
    serviceRequestID
);
```

This produces:

```json
{
  "type": "assignment_offer",
  "mechanic_assignment_id": 15,
  "service_request_id": 31
}
```

The first pair should always include the event type:

```text
"type" -> "<event-name>"
```

---

## Assignment Offer Event

Example:

```json
{
  "type": "assignment_offer",
  "mechanic_assignment_id": 15,
  "service_request_id": 31,
  "expires_time": 1770000000000
}
```

This is sent to the selected mechanic.

---

## Request Status Event

Example:

```json
{
  "type": "request_status",
  "service_request_id": 31,
  "status": "on_the_way"
}
```

Status events can be broadcast to clients subscribed to the request.

---

## Mechanic Location Event

The mechanic client can send:

```json
{
  "type": "mechanic_location",
  "service_request_id": 31,
  "latitude": 11.1085,
  "longitude": 77.3411
}
```

The backend validates the mechanic's access to the request before accepting the update.

---

## Location Processing

The backend can then:

```text
validate mechanic
      |
      v
validate assignment ownership
      |
      v
validate coordinates
      |
      v
update Mechanic.Latitude
      |
      v
update Mechanic.Longitude
      |
      v
update LocationUpdatedTime
      |
      v
broadcast mechanic_location
```

---

## Customer Tracking

The customer can subscribe to a request.

Conceptually:

```text
Customer WebSocket
      |
      v
subscribe_request
      |
      v
Authorization Check
      |
      v
Register Session for Request
      |
      v
Receive:
    snapshot
    mechanic_location
    request_status
```

---

## Live Tracking Snapshot

A tracking snapshot can include:

```text
service request status
customer location
current mechanic location
mechanic information
last location update time
```

This allows the frontend to restore the current state after reconnecting.

---

## Database as Source of Truth

WebSocket events can be missed when a client disconnects.

Therefore the authoritative state remains in MySQL.

Example:

```text
WebSocket assignment_offer missed
        |
        v
MechanicAssignment still exists in DB
        |
        v
Mechanic client reloads assignments through REST
```

This makes WebSocket delivery an optimization rather than the only source of application state.

---

## Real-Time Session Registry

The Phase-1 implementation keeps active WebSocket sessions in memory.

Conceptually:

```text
userID
  ->
WebSocket sessions
```

and:

```text
serviceRequestID
  ->
subscribed WebSocket sessions
```

This is suitable for a single backend instance.

---

## Notifications

The backend supports application notifications.

Typical use cases include:

```text
mechanic application approved
mechanic application rejected
service workflow notification
assignment-related notification
```

Notifications can be stored in the database and retrieved by authenticated users.

---

## Background Jobs

RoadRescue includes background job infrastructure.

Background processing can be used for:

- mechanic application notifications
- recurring pending-request dispatch
- assignment timeout processing
- future asynchronous operations

The Phase-1 implementation runs background jobs inside the backend JVM.

---

## Rate Limiting

The application integrates with the `security_fw` rate-limiter interface.

Rate limiting can protect endpoints such as:

```text
general APIs
service request creation
admin APIs
WebSocket ticket creation
```

Rate-limit failures return:

```text
HTTP 429
```

The Phase-1 implementation can use in-memory state.

A distributed deployment should move rate limiting to shared infrastructure such as Redis or an API gateway.

---

## Object Storage

Mechanic documents are accessed through the `ObjectStorageProvider` abstraction.

This allows storage implementations to be changed without changing application business logic.

Potential providers include:

```text
local filesystem
MinIO
AWS S3
other S3-compatible object storage
```

---

## Main Database Tables

Core application tables include:

```text
Users
Vehicle
Mechanic
ServiceRequest
MechanicService
MechanicAssignment
MechanicShop
UserIdentity

MechanicApplication
MechanicDocument

Notification
BackgroundJob
```

---

## Users

Important fields include:

```text
UserID
Name
Email
Phone
UserType
Status
LastModifiedTime
```

---

## UserIdentity

Important fields include:

```text
UserIdentityID
UserID
Provider
ExternalSubject
LastModifiedTime
```

This table links external authentication identities to local users.

---

## Mechanic

Important fields include:

```text
MechanicID
UserID
MechanicShopID
AvailabilityStatus
Latitude
Longitude
LocationUpdatedTime
LastModifiedTime
```

---

## ServiceRequest

Important fields include:

```text
ServiceRequestID
UserID
VehicleType
ServiceType
Description
Latitude
Longitude
Status
CreatedTime
LastModifiedTime
```

---

## MechanicAssignment

Important fields include:

```text
MechanicAssignmentID
ServiceRequestID
MechanicID
Status
AssignedTime
ExpiresTime
AcceptedTime
CompletedTime
LastModifiedTime
```

---

## MechanicApplication

Application data includes fields such as:

```text
MechanicApplicationID
UserID
Status
ExperienceYears
Address
City
State
PostalCode
SubmittedTime
ReviewedTime
ReviewedBy
RejectionReason
LastModifiedTime
```

---

## MechanicDocument

Document records include fields such as:

```text
MechanicDocumentID
MechanicApplicationID
ObjectKey
OriginalFileName
ContentType
SizeBytes
LastModifiedTime
```

---

## Notification

Notification data includes fields such as:

```text
NotificationID
UserID
NotificationType
Title
Message
Status
CreatedTime
ReadTime
LastModifiedTime
```

---

## REST API Overview

### Current User

```text
GET /api/me
```

---

### Customer APIs

```text
GET  /api/service-requests

POST /api/service-requests

GET  /api/service-requests/{id}

GET  /api/service-requests/{id}/tracking

GET  /api/notifications

POST /api/notifications/{id}/read

POST /api/ws-ticket
```

---

### Mechanic APIs

```text
POST /api/mechanic/availability

GET /api/mechanic/assignments

POST /api/mechanic/assignments/{id}/accept

POST /api/mechanic/assignments/{id}/reject

POST /api/mechanic/assignments/{id}/start-journey

POST /api/mechanic/assignments/{id}/start-service

POST /api/mechanic/assignments/{id}/complete
```

---

### Mechanic Application APIs

```text
GET /api/mechanic/application

POST /api/mechanic/application/create

POST /api/mechanic/application/submit
```

---

### Admin APIs

```text
GET /api/admin/mechanics

GET /api/admin/service-requests

GET /api/admin/users

GET /api/admin/mechanic-applications

GET /api/admin/mechanic-applications/{id}

POST /api/admin/mechanic-applications/{id}/approve

POST /api/admin/mechanic-applications/{id}/reject
```

Exact route configuration is controlled by the application's REST request XML files.

---

## Environment Variables

Typical backend configuration includes:

```env
AUTH0_DOMAIN=your-tenant.auth0.com

AUTH0_AUDIENCE=https://roadrescue-api

ROAD_RESCUE_ADMIN_EMAIL=admin@example.com

ROAD_RESCUE_ASSIGNMENT_RADIUS_KM=25

ROAD_RESCUE_ASSIGNMENT_TIMEOUT_MS=30000
```

Database configuration should also be provided securely.

Example:

```env
DB_URL=jdbc:mysql://localhost:3306/road_rescue

DB_USERNAME=roadrescue

DB_PASSWORD=change-me
```

Do not commit real credentials.

---

## Auth0 Setup

The backend expects:

```text
one Auth0 tenant
one RoadRescue API audience
separate frontend SPA applications where needed
```

Example API audience:

```text
https://roadrescue-api
```

The backend validates access tokens against the configured domain and audience.

---

## Tomcat

The backend currently targets:

```text
Tomcat 9
Java 17
```

Application context:

```text
/app
```

REST prefix:

```text
/app/api
```

WebSocket endpoint:

```text
/app/ws/location
```

---

## Running Locally

Build the application:

```bash
./gradlew clean build
```

Deploy the generated artifact through the configured Tomcat-based local runtime.

Make sure the required environment variables and database configuration are available before startup.

---


---
---

## Phase-1 Architecture

The current implementation is designed primarily for a single backend instance.

The following components are JVM-local or in-memory:

```text
WebSocket session registry
WebSocket ticket store
dispatch synchronization
rate-limit state
background scheduler state
```

This keeps the Phase-1 system simple and understandable.

---

## Scaling Considerations

A horizontally scaled version can evolve toward:

```text
Load Balancer
      |
      v
Multiple Backend Instances
      |
      +---- Redis
      |
      +---- Message Queue
      |
      +---- Distributed WebSocket Pub/Sub
      |
      +---- Dispatch Workers
      |
      v
MySQL
```

Potential improvements include:

- Redis-backed rate limiting
- distributed mechanic reservation
- database row locking or atomic reservation
- distributed WebSocket presence
- durable message queues
- geographically partitioned dispatch
- event versioning
- idempotency keys
- automatic socket reconnection
- GPS filtering
- route calculation
- ETA calculation
- push notifications
- S3/MinIO document storage

---

## Assignment Concurrency

The Phase-1 implementation can protect assignment selection within one JVM.

In a multi-instance deployment, two servers could otherwise select the same available mechanic at the same time.

A distributed version should use one or more of:

```text
atomic conditional database update
database row locking
Redis distributed lock
serialized dispatch worker
partitioned dispatch ownership
```

---

## GPS Tracking Scope

The current tracking model focuses on the mechanic's latest location.

It does not require a complete location-history table.

The mechanic browser sends updated coordinates and the backend stores the latest known location.

Future versions can add:

- GPS history
- accuracy checks
- outlier filtering
- route matching
- adaptive sampling
- battery-aware update intervals

---

## Map Scope

The client applications can display customer and mechanic coordinates on Leaflet/OpenStreetMap.

The current architecture does not require production-grade road routing.

A future routing provider could add:

- road-following route lines
- ETA
- travel distance
- traffic-aware estimates

Potential routing providers include:

```text
OSRM
Mapbox
Google Directions
other routing services
```

---

## Database as Authoritative State

One important design principle is:

```text
WebSocket
    ->
fast realtime communication

Database
    ->
authoritative business state
```

This allows clients to recover state after connection loss.

For example:

```text
mechanic misses WebSocket offer
      |
      v
assignment remains in DB
      |
      v
mechanic reloads assignments through REST
```

---

## Design Goals

RoadRescue Server was designed to demonstrate:

- custom Java framework integration
- authentication
- authorization
- database persistence
- background processing
- real-time WebSocket communication
- GPS tracking
- domain state machines
- automatic dispatch
- frontend/backend integration
- clean separation between framework and application logic

---

## Current Scope

The current backend focuses on Phase-1 roadside assistance with independent mechanics.

Mechanic-shop-specific operational workflows are outside the main Phase-1 dispatch flow.

The current dispatch model selects the nearest available mechanic without filtering through mechanic service specialization.

---

## Future Improvements

Possible future improvements include:

- distributed dispatch
- distributed rate limiting
- Redis WebSocket presence
- durable message queues
- automatic WebSocket reconnect coordination
- event sequence/version numbers
- stronger idempotency
- atomic mechanic reservation
- route and ETA services
- GPS smoothing
- mechanic location history
- service capability matching
- mechanic ratings
- customer ratings
- push notifications
- payment integration
- mechanic shop support
- S3/MinIO object storage
- metrics and monitoring
- distributed tracing
- structured audit logging
- retryable durable background jobs

---

---

## Related Projects

### Sentinal REST Framework

Repository:

```text
rest_fw
```

Provides:

- HTTP routing
- parameter extraction
- type conversion
- Java method invocation

### Sentinal Persistence Framework

Repository:

```text
persistence_fw
```

Provides:

- query construction
- criteria
- joins
- rows
- inserts
- updates

### Sentinal Security Framework

Repository:

```text
security_fw
```

Provides:

- authentication pipeline
- authorization
- request security context
- principal handling
- rate limiting integration

### Customer Frontend

Repository:

```text
road_rescue_ui
```

### Mechanic Frontend

Repository:

```text
road_rescue_mechanic_ui
```

### Admin Frontend

Repository:

```text
road_rescue_admin_ui
```

---

## Status

RoadRescue Server is under active development as a Phase-1 real-time roadside assistance backend and full-stack engineering project.

---

## Business transactions

Each database-backed API is a facade over a business interface and implementation. Its getter returns one cached `TransactionProxy` instance; for example, `MechanicApplicationAPI.getMechanicImpl().createApplication(...)` invokes `MechanicApplicationOperations` implemented by `MechanicApplicationImpl`. REST bean names and public API methods stay the same, and each facade method delegates to its proxy. Handlers and collaborating services also use the proxy getters. Health and WebSocket ticket issuance do not need a database transaction.

All interface business methods run in a transaction without annotations. The owning method commits on successful completion and rolls back on failure or timeout. Nested proxies and persistence calls share the current connection and deadline. Configure `ROAD_RESCUE_TRANSACTION_TIMEOUT_SECONDS` or `transaction.timeout.seconds` in database.properties once at startup (default 30 seconds, positive integer). Timeout checks are cooperative; Java processing is not forcibly interrupted.

Background job attempts also use an interface proxy. Job submission and realtime broadcasts wait for commit; job tracking updates remain outside the attempt transaction so failures can be recorded. Call the facade or proxy getter when using a service; constructing an implementation directly bypasses transaction interception. Optional annotations support propagation, isolation and read-only overrides.

For the external-framework server, rebuild `../persistence_fw` with `./gradlew jar` before compiling the application. The combined server contains the matching persistence sources. Run `./gradlew test` to verify the integration.
