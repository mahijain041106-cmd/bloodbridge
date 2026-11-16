
package backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.sql.*;

public class ApiServer {

    private static final int PORT = 8081;

    private static final ConcurrentHashMap<String, User> sessions =
            new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0
        );

        // AUTHENTICATION APIs
        server.createContext("/api/auth/login", ApiServer::login);
        server.createContext("/api/auth/register", ApiServer::register);

     // REQUEST APIs
        server.createContext("/api/requests/my", ApiServer::myRequests);
        server.createContext("/api/requests/all", ApiServer::allRequests);
        server.createContext("/api/requests", ApiServer::submitRequest);
        
        server.createContext("/api/dashboard",ApiServer::dashboard);
        // NOTIFICATION APIs
        server.createContext("/api/notifications/read",
                ApiServer::markNotificationRead);

        server.createContext("/api/notifications/delete",
                ApiServer::deleteNotification);

        server.createContext("/api/notifications",
                ApiServer::notifications);
        
     // ---------------- INVENTORY APIs ----------------

        server.createContext("/api/inventory/count",
                ApiServer::inventoryCount);

        server.createContext("/api/inventory/expire",
                ApiServer::markInventoryExpired);

        server.createContext("/api/inventory",
                ApiServer::inventory);
        server.createContext("/api/reservations/all", ApiServer::allReservations);
        server.createContext("/api/reservations", ApiServer::manageReservations);

        // HEALTH CHECK
        server.createContext("/api/health", exchange -> {

            addCors(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            sendJson(exchange, 200,
                    "{\"message\":\"BloodBridge API is running\"}");
        });

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        RequestManager.start();

        System.out.println("================================");
        System.out.println(" BLOODBRIDGE API SERVER STARTED");
        System.out.println("================================");
        System.out.println("Server: http://localhost:" + PORT);
        System.out.println("Login API: /api/auth/login");
        System.out.println("Register API: /api/auth/register");
        System.out.println("Submit Request API: /api/requests");
        System.out.println("My Requests API: /api/requests/my");
        System.out.println("Notifications API: /api/notifications");
    }

    // ---------------- LOGIN ----------------

    private static void login(HttpExchange exchange) throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String username = getJsonString(body, "username");
            String password = getJsonString(body, "password");
            String requestedRole = getJsonString(body, "role");

            if (username == null || password == null ||
                    requestedRole == null) {

                sendJson(exchange, 400,
                        "{\"message\":\"All fields are required\"}");
                return;
            }

            if (!requestedRole.equals("REQUESTER") &&
                    !requestedRole.equals("INVENTORY_OPERATOR") &&
                    !requestedRole.equals("ADMIN")) {

                sendJson(exchange, 400,
                        "{\"message\":\"Invalid login portal\"}");
                return;
            }

            AuthManager auth = new AuthManager();
            User user = auth.login(username, password);

            if (!requestedRole.equals(user.getRole())) {

                sendJson(exchange, 403,
                        "{\"message\":\"You are not authorized for this login portal\"}");
                return;
            }

            String token = UUID.randomUUID().toString();
            sessions.put(token, user);

            String response =
                    "{"
                    + "\"message\":\"Login successful\","
                    + "\"token\":\"" + token + "\","
                    + "\"user\":{"
                    + "\"userId\":" + user.getUserId() + ","
                    + "\"fullName\":\"" + escape(user.getFullName()) + "\","
                    + "\"username\":\"" + escape(user.getUsername()) + "\","
                    + "\"email\":\"" + escape(user.getEmail()) + "\","
                    + "\"role\":\"" + escape(user.getRole()) + "\""
                    + "}"
                    + "}";

            sendJson(exchange, 200, response);

            System.out.println(
                    user.getUsername() + " logged in as " + user.getRole()
            );

        } catch (IllegalArgumentException e) {

            sendJson(exchange, 401,
                    "{\"message\":\"" + escape(e.getMessage()) + "\"}");

        } catch (IllegalStateException e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Login service or database error\"}");

            System.out.println("Login error: " + e.getMessage());

        } catch (Exception e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Internal server error\"}");

            System.out.println("API error: " + e.getMessage());
        }
    }

    // ---------------- REQUESTER REGISTRATION ----------------

    private static void register(HttpExchange exchange) throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String fullName = getJsonString(body, "fullName");
            String username = getJsonString(body, "username");
            String email = getJsonString(body, "email");
            String password = getJsonString(body, "password");

            if (fullName == null || username == null ||
                    email == null || password == null) {

                sendJson(exchange, 400,
                        "{\"message\":\"All registration fields are required\"}");
                return;
            }

            AuthManager auth = new AuthManager();

            auth.register(
                    fullName,
                    username,
                    email,
                    password,
                    "REQUESTER"
            );

            sendJson(exchange, 201,
                    "{\"message\":\"Requester account created successfully. Please log in.\"}");

            System.out.println(
                    "New requester registered: " + username
            );

        } catch (IllegalArgumentException e) {

            sendJson(exchange, 400,
                    "{\"message\":\"" + escape(e.getMessage()) + "\"}");

        } catch (IllegalStateException e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Registration service or database error\"}");

            System.out.println("Registration error: " + e.getMessage());

        } catch (Exception e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Internal server error\"}");

            System.out.println("API error: " + e.getMessage());
        }
    }

    // ---------------- SUBMIT BLOOD REQUEST ----------------

    private static void submitRequest(HttpExchange exchange)
            throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {

            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"REQUESTER".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Requester access required\"}");
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String bloodGroup = getJsonString(body, "requiredGroupCode");
            int componentId = getJsonInt(body, "componentId");
            int quantity = getJsonInt(body, "quantity");
            int priority = getJsonInt(body, "priorityLevel");
            String note = getJsonString(body, "requestNote");

            request req = new request(
                    Math.toIntExact(user.getUserId()),
                    bloodGroup,
                    componentId,
                    quantity,
                    priority,
                    note
            );

            RequestService service = new RequestService();
            long requestId = service.submitRequest(req);
            
            RequestProcessingDAO processingDAO =
                    new RequestProcessingDAO();

            boolean queued = processingDAO.queueRequest(
                    Math.toIntExact(requestId)
            );

            if (queued) {
                RequestQueue.addRequest(Math.toIntExact(requestId));

                System.out.println(
                    "Request " + requestId +
                    " automatically added to processing queue."
                );
            }
            sendJson(exchange, 201,
                    "{\"message\":\"Blood request submitted successfully\","
                    + "\"requestId\":" + requestId + "}");

            System.out.println(
                    "Blood request submitted. ID: " + requestId
            );

        } catch (IllegalArgumentException e) {

            sendJson(exchange, 400,
                    "{\"message\":\"" + escape(e.getMessage()) + "\"}");

        } catch (Exception e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to submit blood request\"}");

            System.out.println("Request API error: " + e.getMessage());
        }
    }

    // ---------------- VIEW MY REQUESTS ----------------

    private static void myRequests(HttpExchange exchange)
            throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {

            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"REQUESTER".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Requester access required\"}");
                return;
            }

            RequestService service = new RequestService();

            java.util.List<RequestRecord> records =
                    service.getUserRequests(
                            Math.toIntExact(user.getUserId())
                    );

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < records.size(); i++) {

                RequestRecord r = records.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"requestId\":").append(r.getRequestId()).append(",")
                    .append("\"requestCode\":\"").append(escape(r.getRequestCode())).append("\",")
                    .append("\"bloodGroup\":\"").append(escape(r.getBloodGroup())).append("\",")
                    .append("\"componentId\":").append(r.getComponentId()).append(",")
                    .append("\"quantity\":").append(r.getQuantity()).append(",")
                    .append("\"priority\":").append(r.getPriority()).append(",")
                    .append("\"validationStatus\":\"").append(escape(r.getValidationStatus())).append("\",")
                    .append("\"requestStatus\":\"").append(escape(r.getRequestStatus())).append("\",")
                    .append("\"requestNote\":\"").append(escape(r.getRequestNote())).append("\",")
                    .append("\"submittedAt\":\"")
                    .append(r.getSubmittedAt() == null
                            ? "" : r.getSubmittedAt().toString())
                    .append("\"")
                    .append("}");
            }

            json.append("]");

            sendJson(exchange, 200, json.toString());

        } catch (Exception e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to retrieve requests\"}");

            System.out.println("My requests API error: " + e.getMessage());
        }
    }

    // ---------------- VIEW USER NOTIFICATIONS ----------------

    private static void notifications(HttpExchange exchange)
            throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {

            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"REQUESTER".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Requester access required\"}");
                return;
            }

            NotificationController controller =
                    new NotificationController();

            java.util.List<NotificationRecord> records =
                    controller.getUserNotifications(
                            Long.valueOf(user.getUserId())
                    );

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < records.size(); i++) {

                NotificationRecord n = records.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"notificationId\":")
                    .append(n.getNotificationId()).append(",")

                    .append("\"requestId\":")
                    .append(n.getRequestId() == null
                            ? "null" : n.getRequestId()).append(",")

                    .append("\"reservationId\":")
                    .append(n.getReservationId() == null
                            ? "null" : n.getReservationId()).append(",")

                    .append("\"notificationType\":\"")
                    .append(escape(n.getNotificationType())).append("\",")

                    .append("\"title\":\"")
                    .append(escape(n.getTitle())).append("\",")

                    .append("\"message\":\"")
                    .append(escape(n.getMessage())).append("\",")

                    .append("\"isRead\":")
                    .append(n.isRead()).append(",")

                    .append("\"createdAt\":\"")
                    .append(n.getCreatedAt() == null
                            ? "" : n.getCreatedAt()).append("\"")

                    .append("}");
            }

            json.append("]");

            sendJson(exchange, 200, json.toString());

        } catch (Exception e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to retrieve notifications\"}");

            System.out.println(
                    "Notification API error: " + e.getMessage()
            );
        }
    }

    // ---------------- MARK NOTIFICATION AS READ ----------------

    private static void markNotificationRead(HttpExchange exchange)
            throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {

            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"REQUESTER".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Requester access required\"}");
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            int notificationId = getJsonInt(body, "notificationId");

            // Accepts JSON boolean true or false.
            String readPattern = "\"isRead\"\\s*:\\s*true";
            boolean isRead = java.util.regex.Pattern
                    .compile(readPattern)
                    .matcher(body)
                    .find();

            NotificationController controller =
                    new NotificationController();

            boolean updated = controller.markAsRead(
                    Long.valueOf(notificationId),
                    Long.valueOf(user.getUserId()),
                    isRead
            );

            if (updated) {
                sendJson(exchange, 200,
                        "{\"message\":\"Notification updated successfully\"}");
            } else {
                sendJson(exchange, 404,
                        "{\"message\":\"Notification not found\"}");
            }

        } catch (IllegalArgumentException e) {

            sendJson(exchange, 400,
                    "{\"message\":\"Invalid notification data\"}");

        } catch (Exception e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to update notification\"}");

            System.out.println(
                    "Mark notification error: " + e.getMessage()
            );
        }
    }

 // ---------------- DELETE NOTIFICATION ----------------

 private static void deleteNotification(HttpExchange exchange)
         throws IOException {

     addCors(exchange);

     if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
         exchange.sendResponseHeaders(204, -1);
         return;
     }

     if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
         sendJson(exchange, 405,
                 "{\"message\":\"Method not allowed\"}");
         return;
     }

     try {

         User user = getAuthenticatedUser(exchange);

         if (user == null) {
             sendJson(exchange, 401,
                     "{\"message\":\"Please log in again\"}");
             return;
         }

         if (!"REQUESTER".equals(user.getRole())) {
             sendJson(exchange, 403,
                     "{\"message\":\"Requester access required\"}");
             return;
         }

         String body = new String(
                 exchange.getRequestBody().readAllBytes(),
                 StandardCharsets.UTF_8
         );

         int notificationId = getJsonInt(body, "notificationId");

         NotificationController controller =
                 new NotificationController();

         boolean deleted = controller.deleteNotification(
                 Long.valueOf(notificationId),
                 Long.valueOf(user.getUserId())
         );

         if (deleted) {
             sendJson(exchange, 200,
                     "{\"message\":\"Notification deleted successfully\"}");
         } else {
             sendJson(exchange, 404,
                     "{\"message\":\"Notification not found\"}");
         }

     } catch (IllegalArgumentException e) {

         sendJson(exchange, 400,
                 "{\"message\":\"Invalid notification data\"}");

     } catch (Exception e) {

         sendJson(exchange, 500,
                 "{\"message\":\"Unable to delete notification\"}");

         System.out.println(
                 "Delete notification error: " + e.getMessage()
         );
     }
 }
	

	//---------------- INVENTORY MANAGEMENT API ----------------
	
	private static void inventory(HttpExchange exchange)
	      throws IOException {
	
	  addCors(exchange);
	
	  if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
	      exchange.sendResponseHeaders(204, -1);
	      return;
	  }
	
	  try {
	
	      User user = getAuthenticatedUser(exchange);
	
	      if (user == null) {
	          sendJson(exchange, 401,
	                  "{\"message\":\"Please log in again\"}");
	          return;
	      }
	
	      if (!"INVENTORY_OPERATOR".equals(user.getRole())) {
	          sendJson(exchange, 403,
	                  "{\"message\":\"Inventory operator access required\"}");
	          return;
	      }
	
	      InventoryController controller = new InventoryController();
	
	      String method = exchange.getRequestMethod();
	
	      // VIEW INVENTORY
	      if ("GET".equalsIgnoreCase(method)) {
	
	          String query = exchange.getRequestURI().getQuery();
	
	          java.util.List<java.util.Map<String, Object>> records;
	
	          if (query != null && query.startsWith("status=")) {
	              records = controller.getByStatus(query.substring(7));
	
	          } else if (query != null && query.startsWith("group=")) {
	              records = controller.getByCategory(query.substring(6));
	
	          } else {
	              records = controller.getAll();
	          }
	
	          sendJson(exchange, 200, inventoryToJson(records));
	          return;
	      }
	
	      String body = new String(
	              exchange.getRequestBody().readAllBytes(),
	              StandardCharsets.UTF_8
	      );
	
	      // ADD BLOOD UNIT
	      if ("POST".equalsIgnoreCase(method)) {
	
	          String unitCode = getJsonString(body, "unitCode");
	          String bloodGroup = getJsonString(body, "bloodGroupCode");
	          int componentId = getJsonInt(body, "componentId");
	
	          String collection = getJsonString(body, "collectionDate");
	          String expiry = getJsonString(body, "expiryDate");
	
	          if (unitCode == null || bloodGroup == null
	                  || collection == null || expiry == null) {
	              throw new IllegalArgumentException("Missing inventory details");
	          }
	
	          java.time.LocalDate collectionDate =
	                  java.time.LocalDate.parse(collection);
	
	          java.time.LocalDate expiryDate =
	                  java.time.LocalDate.parse(expiry);
	
	          java.util.Map<String, Object> result = controller.add(
	                  unitCode,
	                  bloodGroup,
	                  componentId,
	                  collectionDate,
	                  expiryDate,
	                  user.getUserId()
	          );
	
	          sendJson(exchange,
	                  Boolean.TRUE.equals(result.get("success")) ? 201 : 400,
	                  inventoryToJson(java.util.List.of(result)));
	
	          return;
	      }
	
	      // UPDATE BLOOD UNIT
	      if ("PUT".equalsIgnoreCase(method)) {
	
	          long unitId = getJsonInt(body, "unitId");
	
	          String unitCode = getJsonString(body, "unitCode");
	          String bloodGroup = getJsonString(body, "bloodGroupCode");
	
	          int componentId = getJsonInt(body, "componentId");
	
	          String collection = getJsonString(body, "collectionDate");
	          String expiry = getJsonString(body, "expiryDate");
	
	          if (unitCode == null || bloodGroup == null
	                  || collection == null || expiry == null) {
	              throw new IllegalArgumentException("Missing inventory details");
	          }
	
	          java.util.Map<String, Object> result = controller.update(
	                  unitId,
	                  unitCode,
	                  bloodGroup,
	                  componentId,
	                  java.time.LocalDate.parse(collection),
	                  java.time.LocalDate.parse(expiry)
	          );
	
	          sendJson(exchange,
	                  Boolean.TRUE.equals(result.get("success")) ? 200 : 400,
	                  inventoryToJson(java.util.List.of(result)));
	
	          return;
	      }
	
	      // DELETE BLOOD UNIT
	      if ("DELETE".equalsIgnoreCase(method)) {
	
	          long unitId = getJsonInt(body, "unitId");
	
	          java.util.Map<String, String> result =
	                  controller.delete(unitId);
	
	          boolean deleted = result.get("message")
	                  .startsWith("Inventory unit deleted.");
	
	          sendJson(exchange, deleted ? 200 : 400,
	                  "{\"message\":\""
	                  + escape(result.get("message")) + "\"}");
	
	          return;
	      }
	
	      sendJson(exchange, 405,
	              "{\"message\":\"Method not allowed\"}");
	
	  } catch (IllegalArgumentException e) {
	
	      sendJson(exchange, 400,
	              "{\"message\":\"Invalid inventory details or date format\"}");
	
	  } catch (Exception e) {
	
	      sendJson(exchange, 500,
	              "{\"message\":\"Inventory operation failed\"}");
	
	      System.out.println("Inventory API error: " + e.getMessage());
	  }
	}

 
	// ---------------- INVENTORY STATISTICS ----------------

	private static void inventoryCount(HttpExchange exchange)
	        throws IOException {

	    addCors(exchange);

	    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
	        exchange.sendResponseHeaders(204, -1);
	        return;
	    }

	    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
	        sendJson(exchange, 405,
	                "{\"message\":\"Method not allowed\"}");
	        return;
	    }

	    try {

	        User user = getAuthenticatedUser(exchange);

	        if (user == null) {
	            sendJson(exchange, 401,
	                    "{\"message\":\"Please log in again\"}");
	            return;
	        }

	        if (!"INVENTORY_OPERATOR".equals(user.getRole())) {
	            sendJson(exchange, 403,
	                    "{\"message\":\"Inventory operator access required\"}");
	            return;
	        }

	        InventoryController controller = new InventoryController();

	        java.util.Map<String, Long> counts = controller.count();

	        sendJson(exchange, 200, inventoryCountsToJson(counts));

	    } catch (Exception e) {

	        sendJson(exchange, 500,
	                "{\"message\":\"Unable to retrieve inventory statistics\"}");

	        System.out.println("Inventory count API error: " + e.getMessage());
	    }
	}

	// ---------------- INVENTORY COUNTS JSON ----------------

	private static String inventoryCountsToJson(
	        java.util.Map<String, Long> counts) {

	    StringBuilder json = new StringBuilder("{");

	    int i = 0;

	    for (java.util.Map.Entry<String, Long> entry : counts.entrySet()) {

	        if (i > 0) {
	            json.append(",");
	        }

	        json.append("\"")
	            .append(escape(entry.getKey()))
	            .append("\":")
	            .append(entry.getValue());

	        i++;
	    }

	    json.append("}");

	    return json.toString();
	}
	
	// ---------------- MARK EXPIRED INVENTORY ----------------

	private static void markInventoryExpired(HttpExchange exchange)
	        throws IOException {

	    addCors(exchange);

	    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
	        exchange.sendResponseHeaders(204, -1);
	        return;
	    }

	    if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
	        sendJson(exchange, 405,
	                "{\"message\":\"Method not allowed\"}");
	        return;
	    }

	    try {

	        User user = getAuthenticatedUser(exchange);

	        if (user == null) {
	            sendJson(exchange, 401,
	                    "{\"message\":\"Please log in again\"}");
	            return;
	        }

	        if (!"INVENTORY_OPERATOR".equals(user.getRole())) {
	            sendJson(exchange, 403,
	                    "{\"message\":\"Inventory operator access required\"}");
	            return;
	        }

	        InventoryController controller = new InventoryController();

	        java.util.Map<String, Integer> result = controller.markExpired();

	        int updated = result.get("updatedUnits");

	        sendJson(exchange, 200,
	                "{\"message\":\"Expiry check completed\","
	                + "\"updatedUnits\":" + updated + "}");

	    } catch (Exception e) {

	        sendJson(exchange, 500,
	                "{\"message\":\"Unable to update expired inventory\"}");

	        System.out.println("Expiry API error: " + e.getMessage());
	    }
	}
    // ---------------- AUTHENTICATED USER ----------------

    private static User getAuthenticatedUser(HttpExchange exchange) {

        String authorization =
                exchange.getRequestHeaders().getFirst("Authorization");

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            return null;
        }

        String token = authorization.substring(7).trim();

        return sessions.get(token);
    }

    // ---------------- JSON INTEGER READER ----------------

    private static int getJsonInt(String json, String key) {

        String search = "\"" + key + "\"";

        int position = json.indexOf(search);

        if (position < 0) {
            throw new IllegalArgumentException(key + " is required");
        }

        position = json.indexOf(':', position + search.length());

        if (position < 0) {
            throw new IllegalArgumentException("Invalid " + key);
        }

        position++;

        while (position < json.length()
                && Character.isWhitespace(json.charAt(position))) {
            position++;
        }

        int start = position;

        while (position < json.length()
                && (Character.isDigit(json.charAt(position))
                || json.charAt(position) == '-')) {
            position++;
        }

        if (start == position) {
            throw new IllegalArgumentException("Invalid " + key);
        }

        try {
            return Integer.parseInt(json.substring(start, position));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + key);
        }
    }

    // ---------------- CORS ----------------

    private static void addCors(HttpExchange exchange) {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin",
                "http://localhost:5173"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization"
        );
    }

    // ---------------- JSON RESPONSE ----------------

    private static void sendJson(
            HttpExchange exchange, int status, String response
    ) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    // ---------------- JSON ESCAPING ----------------

    private static String escape(String value) {

        if (value == null) return "";

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ---------------- JSON STRING READER ----------------

    private static String getJsonString(String json, String key) {

        String search = "\"" + key + "\"";

        int position = json.indexOf(search);

        if (position < 0) return null;

        position = json.indexOf(':', position + search.length());

        if (position < 0) return null;

        position++;

        while (position < json.length() &&
                Character.isWhitespace(json.charAt(position))) {
            position++;
        }

        if (position >= json.length() ||
                json.charAt(position) != '"') {
            return null;
        }

        position++;

        StringBuilder result = new StringBuilder();

        while (position < json.length()) {

            char c = json.charAt(position++);

            if (c == '"') return result.toString();

            if (c == '\\') {

                if (position >= json.length()) return null;

                char escaped = json.charAt(position++);

                switch (escaped) {

                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;

                    case 'u':

                        if (position + 4 > json.length()) return null;

                        try {
                            result.append((char) Integer.parseInt(
                                    json.substring(position, position + 4), 16
                            ));

                            position += 4;

                        } catch (NumberFormatException e) {
                            return null;
                        }

                        break;

                    default:
                        return null;
                }

            } else {
                result.append(c);
            }
        }

        return null;
    }
 // ---------------- CONVERT INVENTORY TO JSON ----------------

    private static String inventoryToJson(
            java.util.List<java.util.Map<String, Object>> records) {

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < records.size(); i++) {

            if (i > 0) {
                json.append(",");
            }

            java.util.Map<String, Object> row = records.get(i);

            json.append("{");

            int count = 0;

            for (java.util.Map.Entry<String, Object> entry : row.entrySet()) {

                if (count > 0) {
                    json.append(",");
                }

                json.append("\"")
                    .append(escape(entry.getKey()))
                    .append("\":");

                Object value = entry.getValue();

                if (value == null) {
                    json.append("null");
                } else if (value instanceof Number || value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"")
                        .append(escape(value.toString()))
                        .append("\"");
                }

                count++;
            }

            json.append("}");
        }

        json.append("]");

        return json.toString();
    }

    // ---------------- ADMIN: VIEW ALL BLOOD REQUESTS ----------------

    private static void allRequests(HttpExchange exchange)
            throws IOException {
    	System.out.println("ALL REQUESTS API CALLED");

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {

            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"ADMIN".equals(user.getRole()) &&
                    !"INVENTORY_OPERATOR".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Management access required\"}");
                return;
            }

            RequestService service = new RequestService();

            java.util.List<RequestRecord> records =
                    service.getAllRequests();
            System.out.println("ADMIN REQUESTS FOUND: " + records.size());

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < records.size(); i++) {

                RequestRecord r = records.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"requestId\":").append(r.getRequestId()).append(",")
                    .append("\"requestCode\":\"").append(escape(r.getRequestCode())).append("\",")
                    .append("\"requesterUserId\":").append(r.getRequesterUserId()).append(",")
                    .append("\"requesterName\":\"").append(escape(r.getRequesterName())).append("\",")
                    .append("\"bloodGroup\":\"").append(escape(r.getBloodGroup())).append("\",")
                    .append("\"componentId\":").append(r.getComponentId()).append(",")
                    .append("\"quantity\":").append(r.getQuantity()).append(",")
                    .append("\"priority\":").append(r.getPriority()).append(",")
                    .append("\"validationStatus\":\"").append(escape(r.getValidationStatus())).append("\",")
                    .append("\"requestStatus\":\"").append(escape(r.getRequestStatus())).append("\",")
                    .append("\"requestNote\":\"").append(escape(r.getRequestNote())).append("\",")
                    .append("\"submittedAt\":\"")
                    .append(r.getSubmittedAt() == null
                            ? "" : r.getSubmittedAt().toString())
                    .append("\"")
                    .append("}");
            }

            json.append("]");

            sendJson(exchange, 200, json.toString());

        } catch (Exception e) {

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to retrieve all blood requests\"}");

            System.out.println(
                    "Admin requests API error: " + e.getMessage()
            );
        }
    }

 // ---------------- ADMIN: VIEW ALL RESERVATIONS ----------------
    private static void allReservations(HttpExchange exchange)
            throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {
            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"ADMIN".equals(user.getRole()) &&
                    !"INVENTORY_OPERATOR".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Management access required\"}");
                return;
            }

            ReservationService service = new ReservationService();
            service.expireReservations();

            java.util.List<java.util.Map<String, Object>> records =
                    service.getAllReservations();

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < records.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }

                java.util.Map<String, Object> r = records.get(i);

                json.append("{")
                    .append("\"reservationId\":").append(r.get("reservationId")).append(",")
                    .append("\"reservationCode\":\"").append(escape(String.valueOf(r.get("reservationCode")))).append("\",")
                    .append("\"requestId\":").append(r.get("requestId")).append(",")
                    .append("\"requestCode\":\"").append(escape(String.valueOf(r.get("requestCode")))).append("\",")
                    .append("\"requesterName\":\"").append(escape(String.valueOf(r.get("requesterName")))).append("\",")
                    .append("\"unitCode\":\"").append(escape(String.valueOf(r.get("unitCode")))).append("\",")
                    .append("\"bloodGroup\":\"").append(escape(String.valueOf(r.get("bloodGroup")))).append("\",")
                    .append("\"componentId\":").append(r.get("componentId")).append(",")
                    .append("\"quantity\":").append(r.get("quantity")).append(",")
                    .append("\"status\":\"").append(escape(String.valueOf(r.get("status")))).append("\",")
                    .append("\"reservedAt\":\"").append(r.get("reservedAt") == null ? "" : r.get("reservedAt")).append("\",")
                    .append("\"expiresAt\":\"").append(r.get("expiresAt") == null ? "" : r.get("expiresAt")).append("\",")
                    .append("\"confirmedAt\":\"").append(r.get("confirmedAt") == null ? "" : r.get("confirmedAt")).append("\",")
                    .append("\"releasedAt\":\"").append(r.get("releasedAt") == null ? "" : r.get("releasedAt")).append("\"")
                    .append("}");
            }

            json.append("]");

            sendJson(exchange, 200, json.toString());

        } catch (Exception e) {
            System.out.println("Admin reservations API error: " + e.getMessage());

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to retrieve reservations\"}");
        }
    }
 // ---------------- ADMIN: MANAGE RESERVATIONS ----------------
    private static void manageReservations(HttpExchange exchange)
            throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405,
                    "{\"message\":\"Method not allowed\"}");
            return;
        }

        try {
            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"ADMIN".equals(user.getRole()) &&
                    !"INVENTORY_OPERATOR".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Management access required\"}");
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            java.util.regex.Matcher idMatcher =
                    java.util.regex.Pattern
                    .compile("\"reservationId\"\\s*:\\s*(\\d+)")
                    .matcher(body);

            java.util.regex.Matcher actionMatcher =
                    java.util.regex.Pattern
                    .compile("\"action\"\\s*:\\s*\"(CONFIRM|CANCEL)\"")
                    .matcher(body);

            if (!idMatcher.find() || !actionMatcher.find()) {
                sendJson(exchange, 400,
                        "{\"message\":\"Reservation ID and action are required\"}");
                return;
            }

            long reservationId = Long.parseLong(idMatcher.group(1));
            String action = actionMatcher.group(1);

            ReservationService service = new ReservationService();

            boolean success;

            if ("CONFIRM".equals(action)) {
                success = service.confirmReservation(reservationId);
            } else {
                success = service.cancelReservation(reservationId);
            }

            if (success) {
                sendJson(exchange, 200,
                        "{\"message\":\"Reservation updated successfully\"}");
            } else {
                sendJson(exchange, 400,
                        "{\"message\":\"Reservation could not be updated\"}");
            }

        } catch (Exception e) {
            System.out.println("Manage reservations error: " + e.getMessage());

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to manage reservation\"}");
        }
    }
    private static void dashboard(HttpExchange exchange)
            throws IOException {

        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            User user = getAuthenticatedUser(exchange);

            if (user == null) {
                sendJson(exchange, 401,
                        "{\"message\":\"Please log in again\"}");
                return;
            }

            if (!"ADMIN".equals(user.getRole()) &&
                    !"INVENTORY_OPERATOR".equals(user.getRole())) {
                sendJson(exchange, 403,
                        "{\"message\":\"Management access required\"}");
                return;
            }

            long totalUnits = 0;
            long availableUnits = 0;
            long reservedUnits = 0;
            long expiredUnits = 0;

            String inventorySQL =
                    "SELECT COUNT(*) AS total_units, " +
                    "SUM(unit_status = 'AVAILABLE') AS available_units, " +
                    "SUM(unit_status = 'RESERVED') AS reserved_units, " +
                    "SUM(unit_status = 'EXPIRED') AS expired_units " +
                    "FROM blood_units";

            try (Connection con = jdbcconnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(inventorySQL);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    totalUnits = rs.getLong("total_units");
                    availableUnits = rs.getLong("available_units");
                    reservedUnits = rs.getLong("reserved_units");
                    expiredUnits = rs.getLong("expired_units");
                }
            }

            long totalRequests = 0;
            long fulfilledRequests = 0;
            long unfulfilledRequests = 0;
            long pendingRequests = 0;

            String requestSQL =
                    "SELECT COUNT(*) AS total_requests, " +
                    "SUM(request_status = 'FULFILLED') AS fulfilled, " +
                    "SUM(request_status = 'UNFULFILLED') AS unfulfilled, " +
                    "SUM(request_status IN ('SUBMITTED','QUEUED','PROCESSING')) AS pending " +
                    "FROM requests";

            try (Connection con = jdbcconnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(requestSQL);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    totalRequests = rs.getLong("total_requests");
                    fulfilledRequests = rs.getLong("fulfilled");
                    unfulfilledRequests = rs.getLong("unfulfilled");
                    pendingRequests = rs.getLong("pending");
                }
            }

            long totalReservations = 0;

            String reservationSQL =
                    "SELECT COUNT(*) AS total_reservations FROM reservations";

            try (Connection con = jdbcconnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(reservationSQL);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    totalReservations =
                            rs.getLong("total_reservations");
                }
            }

            String json =
                    "{\"totalUnits\":" + totalUnits +
                    ",\"availableUnits\":" + availableUnits +
                    ",\"reservedUnits\":" + reservedUnits +
                    ",\"expiredUnits\":" + expiredUnits +
                    ",\"totalRequests\":" + totalRequests +
                    ",\"fulfilledRequests\":" + fulfilledRequests +
                    ",\"unfulfilledRequests\":" + unfulfilledRequests +
                    ",\"pendingRequests\":" + pendingRequests +
                    ",\"totalReservations\":" + totalReservations +
                    "}";

            sendJson(exchange, 200, json);

        } catch (Exception e) {
            System.out.println(
                    "Dashboard API error: " + e.getMessage()
            );

            sendJson(exchange, 500,
                    "{\"message\":\"Unable to load dashboard\"}");
        }
    }
}
