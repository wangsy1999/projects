import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import org.json.JSONObject;
import java.net.Socket;
import java.io.*;
import java.io.IOException;
import java.math.RoundingMode;

import org.json.JSONArray;

public class StoreServerGUI {
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private JPanel inventoryPanel, ordersPanel,adminAccountPanel;
    private JTable inventoryTable, ordersTable,orderItemsTable;
    private DefaultTableModel inventoryTableModel, ordersTableModel,orderItemsTableModel;;
    private JTextField itemNameField, quantityField, priceField;
    private JButton addButton, updateButton, deleteButton,changeStatusButton;
    private JComboBox<String> statusComboBox;
    private int port;
    
    public StoreServerGUI(int port) {
    	this.port = port;
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Store Management");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout(5, 5));
        frame.setSize(800, 600);

        tabbedPane = new JTabbedPane();

        setupInventoryPanel();
        setupOrdersPanel();
        setupAdminAccountPanel();
        tabbedPane.addTab("Inventory", inventoryPanel);
        tabbedPane.addTab("Current Orders", ordersPanel);
        tabbedPane.addTab("Create New Admin", adminAccountPanel);
        frame.add(tabbedPane, BorderLayout.CENTER);
        refreshInventoryTable();
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) { 
            	refreshOrdersTable();
            }

            if (tabbedPane.getSelectedIndex() == 0) {
            	refreshInventoryTable();
            }
        });
    }

    private void setupInventoryPanel() {
        inventoryPanel = new JPanel(new BorderLayout());

        // Inventory Table
        String[] inventoryColumnNames = {"ID", "Item Name", "Quantity", "Price"};
        
        inventoryTableModel = new DefaultTableModel(null, inventoryColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Make table cells non-editable
                return false;
            }
        };
        inventoryTable = new JTable(inventoryTableModel);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryPanel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);

        // Inventory Controls
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.add(new JLabel("Item Name:"));
        itemNameField = new JTextField();
        inputPanel.add(itemNameField);
        
        inputPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField();
        inputPanel.add(quantityField);
        
        inputPanel.add(new JLabel("Price:"));
        priceField = new JTextField();
        inputPanel.add(priceField);
        
        addButton = new JButton("Add Item");
        inputPanel.add(addButton);
        addButton.addActionListener(e -> addItem());
        
        updateButton = new JButton("Update Selected");
        inputPanel.add(updateButton);
        updateButton.addActionListener(e -> updateItem());
        
        deleteButton = new JButton("Delete Selected");
        inputPanel.add(deleteButton);
        deleteButton.addActionListener(e -> deleteItem());
        
        JButton refreshButton = new JButton("Refresh");
        inputPanel.add(refreshButton);
        
        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            // Check if this is a valid selection event and there's an actual row selected
            if (!e.getValueIsAdjusting() && inventoryTable.getSelectedRow() != -1) {
                // Get the model from the table and the selected row index

                int selectedRow = inventoryTable.getSelectedRow();
                
                String itemName =inventoryTableModel.getValueAt(selectedRow, 1).toString();
                String quantity = inventoryTableModel.getValueAt(selectedRow, 2).toString();
                String price = inventoryTableModel.getValueAt(selectedRow, 3).toString();
                
                // Set the text fields with the selected item's details
                itemNameField.setText(itemName);
                quantityField.setText(quantity);
                priceField.setText(price);
            }
        });
   
        refreshButton.addActionListener(e -> {
            refreshInventoryTable();
        });
        
        inventoryPanel.add(inputPanel, BorderLayout.SOUTH);

    }
    
    
    private void refreshInventoryTable() {
        JSONObject request = new JSONObject();
        request.put("action", "get_inventory");
        String response = sendDataToServer(request.toString());
        if (response == null) return;

        inventoryTableModel.setRowCount(0);
        try {
            JSONArray jsonArray = new JSONArray(response); 
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                inventoryTableModel.addRow(new Object[]{ 
                        item.getInt("id"),  
                        item.getString("name"), 
                        item.getInt("quantity"),
                        item.getDouble("price") 

                });
                itemNameField.setText("");   // Clear the item name field
                quantityField.setText("");   // Clear the quantity field
                priceField.setText("");		// Clear the price field

            }
        
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error parsing inventory data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addItem() {
        try {
            JSONObject request = new JSONObject();
            request.put("action", "add_item");
            request.put("name", itemNameField.getText().trim());

            // Parse quantity and price, with error handling for number format exceptions
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());

            request.put("quantity", quantity);
            request.put("price", price);
            
            String response = sendDataToServer(request.toString());
            handleServerResponse(response);
        } catch (NumberFormatException e) {
            // Handle incorrect number formats
            JOptionPane.showMessageDialog(null, "Please enter valid numbers for quantity and price.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            // Handle other exceptions
            JOptionPane.showMessageDialog(null, "Error adding item: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void updateItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an item to update.");
            return;
        }

        try {
            JSONObject request = new JSONObject();
            request.put("action", "update_item");
            request.put("item_id", inventoryTableModel.getValueAt(selectedRow, 0).toString());
            request.put("name", itemNameField.getText().trim());
            
            // Parse quantity and price, with error handling for number format exceptions
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());

            request.put("quantity", quantity);
            request.put("price", price);

            String response = sendDataToServer(request.toString());
            handleServerResponse(response);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter valid numbers for quantity and price.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error updating item: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an item to delete.");
            return;
        }

        try {
            JSONObject request = new JSONObject();
            request.put("action", "delete_item");
            request.put("item_id", inventoryTableModel.getValueAt(selectedRow, 0).toString());

            String response = sendDataToServer(request.toString());
            handleServerResponse(response);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error deleting item: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void setupOrdersPanel() {
        ordersPanel = new JPanel(new BorderLayout());

        // Define the column names for the orders table.
        String[] ordersColumnNames = {"Order ID", "Customer ID", "Total", "Status", "Order Date"};
        ordersTableModel = new DefaultTableModel(null, ordersColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };

        // Ensure ordersTable is initialized here.
        ordersTable = new JTable(ordersTableModel);

        // Now it's safe to add a selection listener to ordersTable.
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displayOrderItems();
            }
        });

        // Setup for Order Items Detail Table (Ensure this is done after ordersTable is initialized)
        String[] orderItemColumnNames = {"Item ID", "Item Name", "Quantity"};
        orderItemsTableModel = new DefaultTableModel(null, orderItemColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        orderItemsTable = new JTable(orderItemsTableModel);

        // Setup the split pane to show both the orders table and the detail table.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                               new JScrollPane(ordersTable),
                                               new JScrollPane(orderItemsTable));
        splitPane.setDividerLocation(200); // Adjust as needed based on your UI preferences.
        splitPane.setResizeWeight(0.5); // Adjust the split weight as needed.
        JPanel statusChangePanel = new JPanel();
        statusComboBox = new JComboBox<>(new String[]{"Processing", "Ready for pickup", "Need action, please contact us","Completed","Canceled"});
        changeStatusButton = new JButton("Change Status");
        
        changeStatusButton.addActionListener(e -> changeOrderStatus());
        ordersPanel = new JPanel(new BorderLayout());
        

        JButton refreshOrdersButton = new JButton("Refresh Orders");
        refreshOrdersButton.addActionListener(e -> refreshOrdersTable());
        statusChangePanel.add(refreshOrdersButton);

        // Rest of your method...
        ordersPanel.add(statusChangePanel, BorderLayout.NORTH);
        statusChangePanel.add(new JLabel("Change Status:"));
        statusChangePanel.add(statusComboBox);
        statusChangePanel.add(changeStatusButton);

        // Add to the ordersPanel layout
        ordersPanel.add(statusChangePanel, BorderLayout.NORTH);
        ordersPanel.add(splitPane, BorderLayout.CENTER);

        // Initial refresh of the orders table to load data.
        refreshOrdersTable(); 
    }
    
    private void changeOrderStatus() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an order to update its status.", "No Order Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JSONObject request = new JSONObject();
        request.put("action", "update_order_status");
        request.put("order_id", ordersTableModel.getValueAt(selectedRow, 0).toString());
        request.put("status", statusComboBox.getSelectedItem().toString());

        String response = sendDataToServer(request.toString());
        handleServerResponse(response);
        refreshOrdersTable();
    }
        
        
    private void displayOrderItems() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow < 0) {
            orderItemsTableModel.setRowCount(0);
            return;
        }
        JSONObject request = new JSONObject();
        request.put("action", "display_order_items");
        request.put("order_id", ordersTableModel.getValueAt(selectedRow, 0).toString());
        String jsonResponse = sendDataToServer(request.toString());
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
                        item.getInt("quantity")
                       
                    };
                    orderItemsTableModel.addRow(row);
                }
            });
        } else {
            JOptionPane.showMessageDialog(frame, "Failed to display order items: " + response.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

        
        private void refreshOrdersTable() {
            JSONObject request = new JSONObject();
            request.put("action", "refresh_orders");
            String jsonResponse = sendDataToServer(request.toString());
            JSONObject response = new JSONObject(jsonResponse);
            if (response.getString("status").equals("success")) {
                JSONArray data = response.getJSONArray("data");
                SwingUtilities.invokeLater(() -> {
                    ordersTableModel.setRowCount(0);
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        Object[] row = new Object[]{
                            item.getInt("order_id"),
                            item.getString("username"),
                            item.getBigDecimal("total").setScale(2, RoundingMode.HALF_UP),
                            item.getString("status"),
                            item.getString("order_date")
                        };
                        ordersTableModel.addRow(row);
                    }
                });
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to fetch orders: " + response.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void setupAdminAccountPanel() {
            adminAccountPanel = new JPanel();
            
            adminAccountPanel.setLayout(new GridLayout(6, 2, 5, 5)); 
            
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JPasswordField confirmPasswordField = new JPasswordField();
            JButton createOrUpdateButton = new JButton("Create New Admin");

            createOrUpdateButton.addActionListener(e -> {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                String confirmPassword = new String(confirmPasswordField.getPassword());

                if (username.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Username cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (password.isEmpty() || confirmPassword.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Password fields cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(null, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JSONObject request = new JSONObject();
                request.put("action", "create_admin");
                request.put("username", username);
                request.put("password", password);  
                String response = sendDataToServer(request.toString());
                handleServerResponse(response); 
            });
            
            adminAccountPanel.add(new JLabel("Username:"));
            adminAccountPanel.add(usernameField);
            adminAccountPanel.add(new JLabel("Password:"));
            adminAccountPanel.add(passwordField);
            adminAccountPanel.add(new JLabel("Confirm Password:"));
            adminAccountPanel.add(confirmPasswordField);
            adminAccountPanel.add(createOrUpdateButton);
        }

        
    private String sendDataToServer(String data) {
        try (Socket socket = new Socket("localhost", port);  
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            out.writeUTF(data);
            return in.readUTF();  // Read the response from the server
        } catch (IOException e) {
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    private void handleServerResponse(String jsonResponse) {
        JSONObject response = new JSONObject(jsonResponse);
        String status = response.getString("status");
        if ("success".equals(status)) {
            JOptionPane.showMessageDialog(frame, "Operation successful.");
            refreshInventoryTable();
        } else {
            JOptionPane.showMessageDialog(frame, "Failed: " + response.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    

    public static void main(String[] args) {
    	StoreServerGUI a=new StoreServerGUI(12000);
    }
}
