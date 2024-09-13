import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.RoundingMode;

public class Server {
    private ServerSocket serverSocket;
    private volatile boolean isRunning = true;
    private JTextArea textArea;
    private JTextField commandField;
    
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        setupGUI();
        log("Server started on port " + port);
    }

    // The UI to show the server logs
    private void setupGUI() {
        JFrame frame = new JFrame("Server Logs");
        textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Create a text field for command input
        commandField = new JTextField();
        commandField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCommand(commandField.getText().trim());
                commandField.setText("");
            }
        });

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(commandField, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });
        frame.setVisible(true);
    }

    private void handleCommand(String command) {
        if ("close".equalsIgnoreCase(command)) {
            stopServer();
        } else {
            log("Unknown command: " + command);
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> textArea.append(message + "\n"));
    }

    public void start() {
        log("Server is running and ready to accept connections...");
        try {
            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(new ClientHandler(socket)).start();
                } catch (IOException e) {
                    if (!isRunning) {
                        log("Server is shutting down...");
                    } else {
                        log("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log("Error closing server socket: " + e.getMessage());
            }
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            serverSocket.close();

            new Timer(500, e -> {
                ((Timer)e.getSource()).stop(); 
                System.exit(0); 
            }).start();
        } catch (IOException e) {
            log("Error during server shutdown: " + e.getMessage());
            new Timer(500, ev -> {
                ((Timer)ev.getSource()).stop(); 
                System.exit(0); 
            }).start();
        }
    }
    
        public static void main(String[] args) {
        	// set the port
            int port = 12000;
            try {
                Server server = new Server(port);
                server.start();
            } catch (IOException e) {
                System.err.println("Server failed to start: " + e.getMessage());
                e.printStackTrace();
            }
        }


    private class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                    
                String received = in.readUTF();  // Receive JSON string from client
                JSONObject request = new JSONObject(received);  // Parse JSON
                String action = request.getString("action");  // Get the action type from JSON

                //if the client need modify the database, it will send a request
                switch (action) {
                    case "login":
                        handleLogin(request, out);
                        break;
                    case "register":
                        handleRegistration(request, out);  
                        break;
                    case "get_inventory":
                        handleGetInventory(out);
                        break;
                    case "get_cart":
                        handleGetCart(request.getString("username"), out);
                        break;
                    case "add_to_cart":
                        handleAddToCart(request, out);  
                        break;
                    case "checkout":
                        handleCheckout(request, out);
                        break;
                    case "get_history":
                        handleGetHistory(request.getString("username"), out);
                        break;
                    case "delete_from_cart":
                        handleDeleteFromCart(request, out);
                        break;
                    case "check_admin":
                        checkAdminAccount(out);
                        break;
                    case "create_admin":
                        createAdminAccount(request, out);
                        break;
                    case "add_item":
                        handleAddItem(request, out);
                        break;
                    case "update_item":
                        handleUpdateItem(request, out);
                        break;
                    case "delete_item":
                        handleDeleteItem(request, out);
                        break;
                    case "update_order_status":
                        handleUpdateOrderStatus(request, out);
                        break;
                    case "refresh_orders":
                        handleRefreshOrders(out);
                        break;
                    case "display_order_items":
                        int orderId = request.getInt("order_id");  
                        handleDisplayOrderItems(orderId, out);
                        break;                       
                    default:
                        out.writeUTF("Invalid request");
                        break;
                        
                }
            } catch (IOException | JSONException e) {
                System.out.println("Error handling client " + socket + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void checkAdminAccount(DataOutputStream out) throws IOException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM users WHERE role = 'admin'")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 0) {
                out.writeUTF("{\"status\":\"exists\"}");
            } else {
                out.writeUTF("{\"status\":\"not_exists\"}");
            }
        } catch (SQLException e) {
            out.writeUTF("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            System.out.println("SQL Error in checkAdminAccount: " + e.getMessage());
        }
    }

    private void createAdminAccount(JSONObject request, DataOutputStream out) throws IOException {
        String username = request.getString("username");
        String password = request.getString("password");
        String hashedPassword = hashPassword(password);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'admin')")) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                out.writeUTF("{\"status\":\"success\"}");
            } else {
                out.writeUTF("{\"status\":\"failure\", \"message\":\"Failed to create admin account.\"}");
            }
        } catch (SQLException e) {
            out.writeUTF("{\"status\":\"error\", \"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
 
    
    private void handleLogin(JSONObject request, DataOutputStream out) throws IOException {
        String username = request.getString("username");
        String password = request.getString("password");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT password_hash, role FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedPasswordHash = rs.getString("password_hash");
                String userRole = rs.getString("role"); 
                if (storedPasswordHash.equals(hashPassword(password))) {
                    JSONObject response = new JSONObject();
                    response.put("status", "success");
                    response.put("role", userRole);
                    out.writeUTF(response.toString());
                } else {
                    JSONObject response = new JSONObject();
                    response.put("status", "failure");
                    response.put("message", "Invalid username or password.");
                    out.writeUTF(response.toString());
                }
            } else {
                JSONObject response = new JSONObject();
                response.put("status", "failure");
                response.put("message", "User not found.");
                out.writeUTF(response.toString());
            }
        }catch (SQLException e) {
            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("message", "Database error.");
            out.writeUTF(response.toString());
            System.out.println("SQL Error in handleLogin: " + e.getMessage());
        }
    }


    private void handleRegistration(JSONObject request, DataOutputStream out) throws IOException {
        String username = request.getString("username");
        String password = request.getString("password");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                JSONObject response = new JSONObject();
                response.put("status", "failure");
                response.put("message", "Username already exists.");
                out.writeUTF(response.toString());
            } else {
                String hashedPassword = hashPassword(password);
                try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'user')")) {
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, hashedPassword);
                    int affectedRows = insertStmt.executeUpdate();
                    if (affectedRows > 0) {
                        JSONObject response = new JSONObject();
                        response.put("status", "success");
                        out.writeUTF(response.toString());
                    } else {
                        JSONObject response = new JSONObject();
                        response.put("status", "failure");
                        response.put("message", "Failed to create user.");
                        out.writeUTF(response.toString());
                    }
                }
            }
        } catch (SQLException e) {
            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("message", "Database error.");
            out.writeUTF(response.toString());
            System.out.println("SQL Error in handleRegistration: " + e.getMessage());
        }
    }


    private void handleGetInventory(DataOutputStream out) throws IOException {
        JSONArray jsonArray = new JSONArray();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT item_id, name, price, quantity FROM inventory ORDER BY item_id ASC")) {
            while (rs.next()) {
                try {
                    JSONObject item = new JSONObject();
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("price", rs.getDouble("price"));
                    item.put("name", rs.getString("name"));      
                    item.put("id", rs.getInt("item_id"));
                    jsonArray.put(item);
                } catch (JSONException e) {
                    System.out.println("JSON Error: " + e.getMessage());
                }
            }
            out.writeUTF(jsonArray.toString());
        } catch (SQLException e) {
            System.out.println("SQL Error in handleGetInventory: " + e.getMessage());
            out.writeUTF("Database error in inventory retrieval");
        }
    }
    
    private void handleAddToCart(JSONObject request, DataOutputStream out) throws IOException {
        String username = request.getString("username");
        int itemId = request.getInt("item_id");
        int quantityToAdd = request.getInt("quantity");

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check current stock from the inventory
            String stockSql = "SELECT quantity FROM inventory WHERE item_id = ?";
            PreparedStatement stockStmt = conn.prepareStatement(stockSql);
            stockStmt.setInt(1, itemId);
            ResultSet stockRs = stockStmt.executeQuery();
            
            if (!stockRs.next()) {
                out.writeUTF("{\"status\":\"failure\",\"error\":\"Item not found.\"}");
                return;
            }
            
            int stockQuantity = stockRs.getInt("quantity");
            
            // Check if the item is already in the cart
            String checkSql = "SELECT quantity FROM cart WHERE item_id = ? AND username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, itemId);
            checkStmt.setString(2, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int existingQuantity = rs.getInt("quantity");
                if (existingQuantity + quantityToAdd > stockQuantity) {
                    out.writeUTF("{\"status\":\"failure\",\"error\":\"Insufficient stock available.\"}");
                    return;
                }
                // Update the quantity in the cart
                String updateSql = "UPDATE cart SET quantity = quantity + ? WHERE item_id = ? AND username = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, quantityToAdd);
                updateStmt.setInt(2, itemId);
                updateStmt.setString(3, username);
                updateStmt.executeUpdate();
            } else {
                // Insert the item into the cart if not already present
                if (quantityToAdd > stockQuantity) {
                    out.writeUTF("{\"status\":\"failure\",\"error\":\"Insufficient stock available.\"}");
                    return;
                }
                String insertSql = "INSERT INTO cart (item_id, quantity, username) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, itemId);
                insertStmt.setInt(2, quantityToAdd);
                insertStmt.setString(3, username);
                insertStmt.executeUpdate();
            }
            out.writeUTF("{\"status\":\"success\"}");
        } catch (SQLException e) {
            out.writeUTF("{\"status\":\"failure\",\"error\":\"Database error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    private void handleGetCart(String username, DataOutputStream out) {
        JSONArray jsonArray = new JSONArray();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT c.item_id, i.name, i.price, c.quantity, (i.price * c.quantity) AS total " +
                 "FROM cart c JOIN inventory i ON c.item_id = i.item_id " +
                 "WHERE c.username = ? ORDER BY c.item_id ASC")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("item_id", rs.getInt("item_id"));
                    item.put("name", rs.getString("name"));
                    item.put("price", rs.getDouble("price"));
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("total", rs.getDouble("total"));  
                    jsonArray.put(item);
                }
                out.writeUTF(jsonArray.toString());
            } catch (JSONException e) {
                System.out.println("JSON Error in handleGetCart: " + e.getMessage());
                out.writeUTF("{\"status\":\"error\",\"message\":\"JSON error in cart data retrieval.\"}");
            }
        } catch (SQLException e) {
            System.out.println("SQL Error in handleGetCart: " + e.getMessage());
            try {
                out.writeUTF("{\"status\":\"error\",\"message\":\"Database error in cart retrieval.\"}");
            } catch (IOException ioException) {
                System.out.println("IO Error when writing SQL error response: " + ioException.getMessage());
            }
        } catch (IOException e) {
            System.out.println("IO Error when writing response: " + e.getMessage());
        }
    }



    private void handleCheckout(JSONObject checkoutDetails, DataOutputStream out) throws IOException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Enable transaction control

            String username = checkoutDetails.getString("username");
            JSONArray items = checkoutDetails.getJSONArray("items");
            double total = checkoutDetails.getDouble("total");
            String status = checkoutDetails.getString("status");

            String createOrderSQL = "INSERT INTO orders (username, order_date, total, status) VALUES (?, CURRENT_DATE,? ,?)";
            try (PreparedStatement createOrderStmt = conn.prepareStatement(createOrderSQL, Statement.RETURN_GENERATED_KEYS)) {
                createOrderStmt.setString(1, username);
                createOrderStmt.setDouble(2, total);
                createOrderStmt.setString(3, status);
                int orderAffectedRows = createOrderStmt.executeUpdate();

                if (orderAffectedRows == 0) {
                    conn.rollback();
                    out.writeUTF("{\"status\":\"failure\",\"error\":\"Failed to create order.\"}");
                    return;
                }

                ResultSet generatedKeys = createOrderStmt.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    conn.rollback();
                    out.writeUTF("{\"status\":\"failure\",\"error\":\"Failed to retrieve order ID.\"}");
                    return;
                }
                long orderId = generatedKeys.getLong(1);

                // Process each item in the order
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    int itemId = item.getInt("id");
                    int quantity = item.getInt("quantity");

                    String insertOrderItemSQL = "INSERT INTO order_items (order_id, item_id, quantity) VALUES (?, ?, ?)";
                    try (PreparedStatement insertOrderItemStmt = conn.prepareStatement(insertOrderItemSQL)) {
                        insertOrderItemStmt.setLong(1, orderId);
                        insertOrderItemStmt.setInt(2, itemId);
                        insertOrderItemStmt.setInt(3, quantity);
                        insertOrderItemStmt.executeUpdate();
                    }

                    String updateInventorySQL = "UPDATE inventory SET quantity = quantity - ? WHERE item_id = ? AND quantity >= ?";
                    try (PreparedStatement updateInventoryStmt = conn.prepareStatement(updateInventorySQL)) {
                        updateInventoryStmt.setInt(1, quantity);
                        updateInventoryStmt.setInt(2, itemId);
                        updateInventoryStmt.setInt(3, quantity);
                        int inventoryAffectedRows = updateInventoryStmt.executeUpdate();

                        if (inventoryAffectedRows == 0) {
                            conn.rollback();
                            out.writeUTF("{\"status\":\"failure\",\"error\":\"Not enough stock for item ID: " + itemId + "\"}");
                            return;
                        }
                    }
                }

                String clearCartSQL = "DELETE FROM cart WHERE username = ?";
                try (PreparedStatement clearCartStmt = conn.prepareStatement(clearCartSQL)) {
                    clearCartStmt.setString(1, username);
                    clearCartStmt.executeUpdate();
                }

                conn.commit();
                out.writeUTF("{\"status\":\"success\"}");
            }
        } catch (JSONException | SQLException e) {
            System.out.println("Error in handleCheckout: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("Rollback error: " + ex.getMessage());
            }
            out.writeUTF("{\"status\":\"failure\",\"error\":\"" + e.getMessage() + "\"}");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    System.out.println("Error closing connection: " + ex.getMessage());
                }
            }
        }
    }

    private void handleDeleteFromCart(JSONObject request, DataOutputStream out) throws IOException {
        String username = request.getString("username");
        int itemId = request.getInt("item_id");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM cart WHERE item_id = ? AND username = ?")) {
            stmt.setInt(1, itemId);
            stmt.setString(2, username);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                out.writeUTF("{\"status\":\"success\"}");
            } else {
                out.writeUTF("{\"status\":\"failure\",\"message\":\"Item not found or already removed.\"}");
            }
        } catch (SQLException e) {
            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("message", "Database error: " + e.getMessage());
            out.writeUTF(response.toString());
            System.out.println("SQL Error in handleDeleteFromCart: " + e.getMessage());
        }
    }
    
    private void handleGetHistory(String username, DataOutputStream out) throws IOException {
        JSONArray jsonArray = new JSONArray();
        try (Connection conn = DatabaseConnection.getConnection();
            
             PreparedStatement stmt = conn.prepareStatement("SELECT order_id, order_date, total, status FROM orders WHERE username = ? ORDER BY order_date DESC,CASE WHEN status ='Pending' THEN 2 WHEN status ='Ready for pickup'THEN 4 WHEN status ='Need action, please contact us' THEN 1 WHEN status = 'Processing' THEN 3 WHEN status = 'Completed' THEN 5 ELSE 6 END ASC")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    JSONObject order = new JSONObject();
                    order.put("order_id", rs.getInt("order_id"));
                    order.put("order_date", rs.getString("order_date").toString());
                    order.put("total", rs.getDouble("total"));
                    order.put("status", rs.getString("status"));
                    jsonArray.put(order);
                } catch (JSONException e) {
                    System.out.println("JSON Error: " + e.getMessage());
                }
            }
            out.writeUTF(jsonArray.toString());
        } catch (SQLException e) {
            System.out.println("SQL Error in handleGetHistory: " + e.getMessage());
            out.writeUTF("Database error in history retrieval");
        }
    }

    private void handleAddItem(JSONObject request, DataOutputStream out) throws IOException {
        String name = request.getString("name");
        int quantity = request.getInt("quantity");
        double price = request.getDouble("price");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM inventory WHERE name = ?");
             PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO inventory (name, quantity, price) VALUES (?, ?, ?)")) {
            
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                out.writeUTF("{\"status\":\"failure\",\"message\":\"Item already exists. Please use update button.\"}");
                return;
            }

            insertStmt.setString(1, name);
            insertStmt.setInt(2, quantity);
            insertStmt.setDouble(3, price);
            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows > 0) {
                out.writeUTF("{\"status\":\"success\"}");
            } else {
                out.writeUTF("{\"status\":\"failure\",\"message\":\"Failed to add item.\"}");
            }
        } catch (SQLException e) {
            out.writeUTF("{\"status\":\"error\",\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    
    private void handleUpdateItem(JSONObject request, DataOutputStream out) throws IOException {
        String name = request.getString("name");
        int quantity = request.getInt("quantity");
        double price = request.getDouble("price");
        int itemId = request.getInt("item_id");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE inventory SET name = ?, quantity = ?, price = ? WHERE item_id = ?")) {
            stmt.setString(1, name);
            stmt.setInt(2, quantity);
            stmt.setDouble(3, price);
            stmt.setInt(4, itemId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                out.writeUTF("{\"status\":\"success\"}");
            } else {
                out.writeUTF("{\"status\":\"failure\",\"message\":\"No item found or no changes made.\"}");
            }
        } catch (SQLException e) {
            out.writeUTF("{\"status\":\"error\",\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
    
    private void handleDeleteItem(JSONObject request, DataOutputStream out) throws IOException {
        int itemId = request.getInt("item_id");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Check if the item exists in any non-completed orders
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.order_id " +
                "WHERE oi.item_id = ? AND o.status <> 'Completed'");
            checkStmt.setInt(1, itemId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // If the item exists in any non-completed orders, do not allow deletion
                out.writeUTF("{\"status\":\"failure\",\"message\":\"Cannot delete item because it is part of an uncompleted order.\"}");
                return;
            }

            // If the item does not exist in any non-completed orders, proceed to dSelete from inventory
            PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM inventory WHERE item_id = ?");
            deleteStmt.setInt(1, itemId);
            int affectedRows = deleteStmt.executeUpdate();
            if (affectedRows > 0) {
                out.writeUTF("{\"status\":\"success\"}");
            } else {
                out.writeUTF("{\"status\":\"failure\",\"message\":\"Item not found or already removed.\"}");
            }
        } catch (SQLException e) {
            out.writeUTF("{\"status\":\"error\",\"message\":\"Database error: " + e.getMessage() + "\"}");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    out.writeUTF("{\"status\":\"error\",\"message\":\"Error closing connection: " + ex.getMessage() + "\"}");
                }
            }
        }
    }



    private void handleRefreshOrders(DataOutputStream out) throws IOException {
        String sql = "SELECT order_id, username, total, status, order_date FROM orders ORDER BY CASE WHEN status IS NULL OR status ='Need action, please contact us' THEN 4 WHEN status ='Pending' THEN 1 WHEN status ='Ready for pickup' THEN 3 WHEN status = 'Processing' THEN 2 WHEN status = 'Completed' THEN 5 ELSE 6 END ASC, order_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            JSONArray jsonArray = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("order_id", rs.getInt("order_id"));
                item.put("username", rs.getString("username"));
                item.put("total", rs.getBigDecimal("total").setScale(2, RoundingMode.HALF_UP));
                item.put("status", rs.getString("status"));
                item.put("order_date", rs.getString("order_date"));
                jsonArray.put(item);
            }
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", jsonArray);
            out.writeUTF(response.toString());
        } catch (SQLException e) {
            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("message", "Database error: " + e.getMessage());
            out.writeUTF(response.toString());
        }
    }
    
    private void handleDisplayOrderItems(int orderId, DataOutputStream out) throws IOException {
        String sql = "SELECT oi.item_id, i.name, oi.quantity, i.price FROM order_items oi JOIN inventory i ON oi.item_id = i.item_id WHERE oi.order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            JSONArray jsonArray = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("item_id", rs.getInt("item_id"));
                item.put("name", rs.getString("name"));
                item.put("quantity", rs.getInt("quantity"));
                item.put("price", rs.getBigDecimal("price").setScale(2, RoundingMode.HALF_UP));
                jsonArray.put(item);
            }
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", jsonArray);
            out.writeUTF(response.toString());
        } catch (SQLException e) {
            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("message", "Database error: " + e.getMessage());
            out.writeUTF(response.toString());
        }
    }

	private void handleUpdateOrderStatus(JSONObject request, DataOutputStream out) throws IOException {
	    int orderId = request.getInt("order_id");
	    String status = request.getString("status");
	
	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement stmt = conn.prepareStatement("UPDATE orders SET status = ? WHERE order_id = ?")) {
	        stmt.setString(1, status);
	        stmt.setInt(2, orderId);
	        int affectedRows = stmt.executeUpdate();
	        if (affectedRows > 0) {
	            out.writeUTF("{\"status\":\"success\"}");
	        } else {
	            out.writeUTF("{\"status\":\"failure\",\"message\":\"Order not found or no change made.\"}");
	        }
	    } catch (SQLException e) {
	        out.writeUTF("{\"status\":\"error\",\"message\":\"Database error: " + e.getMessage() + "\"}");
	    }
	}
    
	
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot hash the password", e);
        }
    }


}