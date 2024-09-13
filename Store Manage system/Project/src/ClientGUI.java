import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.Socket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.EOFException;
import org.json.JSONException;
import java.text.DecimalFormat;
public class ClientGUI {
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private JPanel browsePanel, orderPanel, historyPanel;
    private DefaultTableModel cartTableModel, tableModel, historyTableModel, orderItemsTableModel;
    private JTable cartTable, historyTable;
    private JLabel cartTotalLabel;
    private String username;
    private int port;
    private DecimalFormat df = new DecimalFormat("#.00");
    
    public ClientGUI(String username,int port) {
    	this.port = port;
        this.username = username;
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Client Shopping Interface");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);

        tabbedPane = new JTabbedPane();
        browsePanel = new JPanel(new BorderLayout());
        orderPanel = new JPanel(new BorderLayout());
        historyPanel = new JPanel(new BorderLayout());

        setupBrowsePanel();
        setupCartPanel();
        setupHistoryPanel();

        tabbedPane.addTab("Browse Inventory", browsePanel);
        tabbedPane.addTab("Your Cart", orderPanel);
        tabbedPane.addTab("Order History", historyPanel);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) { 
                refreshCart();
            }
            if (tabbedPane.getSelectedIndex() == 2) { 
            	refreshHistory();
            }
            if (tabbedPane.getSelectedIndex() == 0) {
            	refreshInventory();
            }
        });

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupBrowsePanel() {
        JLabel browseLabel = new JLabel("Available Products");
        browsePanel.add(browseLabel, BorderLayout.NORTH);

        String[] columnNames = {"Product ID", "Product Name",  "Available Quantity","Price"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        browsePanel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Inventory");
        refreshButton.addActionListener(e -> refreshInventory());
       // browsePanel.add(refreshButton, BorderLayout.SOUTH);
        JButton addToCartButton = new JButton("Add to Cart");
        
        
        addToCartButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int itemId = (int) table.getValueAt(selectedRow, 0);
                int stockQuantity = (int) table.getValueAt(selectedRow, 2); // Assuming stock quantity is the fourth column
                String itemName = (String) table.getValueAt(selectedRow, 1);

                // Prompt the user for the desired quantity
                String quantityString = JOptionPane.showInputDialog(null, "Enter quantity for " + itemName + ": ", "Select Quantity", JOptionPane.PLAIN_MESSAGE);
                if (quantityString != null) {
                    try {
                        int quantity = Integer.parseInt(quantityString);
                        if (quantity <= 0) {
                            JOptionPane.showMessageDialog(null, "Invalid quantity selected.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (quantity > stockQuantity) {
                            JOptionPane.showMessageDialog(null, "Does not have enough stock.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        // Add to cart logic here (considering the stock)
                        addItemToCart(itemId, quantity);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "No item selected.");
            }
        });
        JPanel formPanel = new JPanel(new GridLayout(0, 2));
        refreshButton.addActionListener(e -> refreshCart());
        formPanel.add(refreshButton);
        formPanel.add(addToCartButton);
        browsePanel.add(formPanel, BorderLayout.SOUTH);

        refreshInventory();
    }

    private void setupCartPanel() {
        JLabel cartLabel = new JLabel("Shopping Cart");
        JPanel formPanel = new JPanel(new GridLayout(0, 3));

        orderPanel.add(cartLabel, BorderLayout.NORTH);

        String[] cartColumns = {"Product ID", "Product Name", "Price", "Quantity", "Total"};
        cartTableModel = new DefaultTableModel(cartColumns, 0);
        cartTable = new JTable(cartTableModel);
        JScrollPane cartScrollPane = new JScrollPane(cartTable);
        orderPanel.add(cartScrollPane, BorderLayout.CENTER);

        cartTotalLabel = new JLabel("Total: $0.00");
        refreshCart();        
        JButton checkoutButton = new JButton("Checkout");
        checkoutButton.addActionListener(e -> checkout());
        formPanel.add(cartTotalLabel);
        formPanel.add(new JLabel(""));
        formPanel.add(new JLabel(""));
        formPanel.add(checkoutButton);
        JButton deleteFromCartButton = new JButton("Delete Selected");
        deleteFromCartButton.addActionListener(e -> deleteSelectedItemFromCart());
        formPanel.add(deleteFromCartButton);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshCart());
        formPanel.add(refreshButton);
        orderPanel.add(formPanel, BorderLayout.SOUTH);

        
    }

    private void setupHistoryPanel() {
        historyPanel.setLayout(new BorderLayout());  // Ensure the panel uses BorderLayout

        JLabel historyLabel = new JLabel("Order History");
        historyPanel.add(historyLabel, BorderLayout.NORTH);

        String[] historyColumns = {"Order ID", "Date", "Total", "Status"};
        historyTableModel = new DefaultTableModel(historyColumns, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = new JTable(historyTableModel);

        // Now it's safe to add a selection listener to ordersTable.
        historyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displayOrderItems();
            }
        });

        JScrollPane historyScrollPane = new JScrollPane(historyTable);
        
        // Set up for Order Items Detail Table
        String[] orderItemColumnNames = {"Item ID", "Item Name", "Quantity"};
        orderItemsTableModel = new DefaultTableModel(null, orderItemColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable orderItemsTable = new JTable(orderItemsTableModel);
        JScrollPane orderItemsScrollPane = new JScrollPane(orderItemsTable);

        // Create a split pane to hold both the history and order items tables
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, historyScrollPane, orderItemsScrollPane);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.5);

        // Add the split pane to the main history panel
        historyPanel.add(splitPane, BorderLayout.CENTER);

        // Button to refresh the history table
        JButton refreshHistoryButton = new JButton("Refresh History");
        refreshHistoryButton.addActionListener(e -> refreshHistory());
        historyPanel.add(refreshHistoryButton, BorderLayout.SOUTH);
    }


    private void refreshInventory() {
        JSONObject request = new JSONObject();
        request.put("action", "get_inventory");
        String response = sendRequestToServer(request.toString());
        if (response == null) return;

        tableModel.setRowCount(0);
        try {
            JSONArray jsonArray = new JSONArray(response); 
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                int quantity = item.getInt("quantity");
                if (quantity > 0) {  // Add the item only if quantity is greater than zero
                    tableModel.addRow(new Object[]{ 
                        item.getInt("id"),  
                        item.getString("name"), 
                        quantity,
                        item.getDouble("price")
                         
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error parsing inventory data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void refreshHistory() {
        JSONObject request = new JSONObject();
        request.put("action", "get_history");
        request.put("username", this.username);
        String response = sendRequestToServer(request.toString());
        //System.out.println(response);
        if (response == null) return;

        historyTableModel.setRowCount(0);
        try {
            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject order = jsonArray.getJSONObject(i);
                historyTableModel.addRow(new Object[]{
                    order.getInt("order_id"),
                    order.getString("order_date"),
                    order.getBigDecimal("total").setScale(2, RoundingMode.HALF_UP),
                    order.getString("status")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error parsing history data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshCart() {
        JSONObject request = new JSONObject();
        request.put("action", "get_cart");
        request.put("username", this.username);
        String response = sendRequestToServer(request.toString());

        if (response == null) {
            JOptionPane.showMessageDialog(frame, "Error retrieving cart data.", "Network Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        cartTableModel.setRowCount(0); 
        double total = 0.0;
        try {
            JSONArray jsonArray = new JSONArray(response);


            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                String name = item.getString("name");
                double price = item.getDouble("price");
                int itemId = item.getInt("item_id");
                int quantity = item.getInt("quantity");
                DecimalFormat df = new DecimalFormat("#.00"); 

                double itemTotal = price * quantity;
                String formattedItemTotal = df.format(itemTotal); 

                cartTableModel.addRow(new Object[]{itemId, name, price, quantity, formattedItemTotal});
                total += Double.parseDouble(formattedItemTotal);
            }

            cartTotalLabel.setText(String.format("Total: $%.2f", total));

        } catch (JSONException e) {
            JOptionPane.showMessageDialog(frame, "Error parsing cart data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkout() {
        if (cartTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(frame, "Your cart is empty.", "Checkout Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            JSONArray items = new JSONArray();
            double total = 0.0; 

            for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                JSONObject item = new JSONObject();
                int itemId = (Integer) cartTableModel.getValueAt(i, 0);
                int quantity = (Integer) cartTableModel.getValueAt(i, 3);
                double price = (Double) cartTableModel.getValueAt(i, 2);  
                double itemTotal = price * quantity;  

                total += itemTotal;  

                item.put("id", itemId);
                item.put("quantity", quantity);
                items.put(item);
            }

            JSONObject checkoutRequest = new JSONObject();
            checkoutRequest.put("action", "checkout");
            checkoutRequest.put("username", username);
            checkoutRequest.put("items", items);
            checkoutRequest.put("total", total);  
            checkoutRequest.put("status", "Pending");
            String response = sendRequestToServer(checkoutRequest.toString());
            
            if (response != null) {
                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.getString("status");
                if ("success".equals(status)) {
                    JOptionPane.showMessageDialog(frame, "Checkout successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    cartTableModel.setRowCount(0);
                    cartTotalLabel.setText("Total: $0.00");
                } else {
                    String errorMessage = jsonResponse.optString("error", "Unknown error during checkout.");
                    JOptionPane.showMessageDialog(frame, "Checkout failed: " + errorMessage, "Checkout Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Server response was null, checkout failed.", "Checkout Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (JSONException e) {
            JOptionPane.showMessageDialog(frame, "Failed to process checkout due to JSON parsing error: " + e.getMessage(), "JSON Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void addItemToCart(int itemId, int quantity) {
        JSONObject request = new JSONObject();
        request.put("action", "add_to_cart");
        request.put("username", username);
        request.put("item_id", itemId);
        request.put("quantity", quantity);

        String response = sendRequestToServer(request.toString());
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");
            if ("success".equals(status)) {
                JOptionPane.showMessageDialog(null, "Item(s) added to cart!");
                refreshInventory();  
            } else {
                
                String errorMessage = jsonResponse.optString("error", "Failed to add item to cart.");
                JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (JSONException e) {
            // Handle the case where the response is not in proper JSON format
            JOptionPane.showMessageDialog(null, "Error parsing server response: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        
    }
    

    private void deleteSelectedItemFromCart() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an item to delete from the cart.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int itemId = (int) cartTableModel.getValueAt(selectedRow, 0);

        JSONObject request = new JSONObject();
        request.put("action", "delete_from_cart");
        request.put("username", this.username);
        request.put("item_id", itemId);

        String response = sendRequestToServer(request.toString());
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");
            if ("success".equals(status)) {
                JOptionPane.showMessageDialog(null, "Item(s) deleted!");
                refreshInventory();  
            } else {
                
                String errorMessage = jsonResponse.optString("error", "Failed to deleted item from cart.");
                JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (JSONException e) {

            JOptionPane.showMessageDialog(null, "Error parsing server response: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        refreshCart();
    }

    private void displayOrderItems() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow < 0) {
            orderItemsTableModel.setRowCount(0);
            return;
        }
        JSONObject request = new JSONObject();
        request.put("action", "display_order_items");
        request.put("order_id", historyTableModel.getValueAt(selectedRow, 0).toString());
        String jsonResponse = sendRequestToServer(request.toString());
        JSONObject response = new JSONObject(jsonResponse);
        if (response.getString("status").equals("success")) {
            JSONArray data = response.getJSONArray("data");
            SwingUtilities.invokeLater(() -> {
                orderItemsTableModel.setRowCount(0);
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    Object[] row = new Object[]{
                        item.getInt("item_id"),
                        item.getString("name"),
                        item.getInt("quantity"),
                        //item.getBigDecimal("price")
                    };
                    orderItemsTableModel.addRow(row);
                }
            });
        } else {
            JOptionPane.showMessageDialog(frame, "Failed to display order items: " + response.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    
    private String sendRequestToServer(String request) {
        try (Socket socket = new Socket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            out.writeUTF(request); 
            out.flush();
            return in.readUTF(); 
        } catch (EOFException e) {
            System.err.println("Server closed connection unexpectedly: " + e.getMessage());
            return "Server closed the connection unexpectedly.";
        } catch (IOException e) {
            System.err.println("Network error: " + e.getMessage());
            return "Network error: " + e.getMessage();
        }
    }


    public static void main(String[] args) {
    	ClientGUI a =  new ClientGUI("exampleUser",12000);
    }
}
