import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import org.json.JSONObject;  

public class LoginGUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;
    private int port;
    public LoginGUI(int port) {
    	this.port = port;
        setTitle("Login");
        setSize(300, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new GridLayout(3, 2, 5, 5));

        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        loginButton = new JButton("Login");
        add(loginButton);
        loginButton.addActionListener(this::performLogin);

        registerButton = new JButton("Register");
        add(registerButton);
        registerButton.addActionListener(e -> openRegistrationForm(port));

        setLocationRelativeTo(null); // Center the window
    }

    private void performLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        try (Socket socket = new Socket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            
            // Create JSON object for login request
            JSONObject request = new JSONObject();
            request.put("action", "login");
            request.put("username", username);
            request.put("password", password);

            // Send JSON string to the server
            out.writeUTF(request.toString());
            
            // Read JSON response from server
            String response = in.readUTF();
            JSONObject jsonResponse = new JSONObject(response);

            // Check login status
            if ("success".equals(jsonResponse.getString("status"))) {
                String role = jsonResponse.getString("role"); // Retrieve the role from the response
                JOptionPane.showMessageDialog(this, "Login successful.");
                this.dispose();  // Close the login window
                // Open different UI based on user role
                if ("admin".equals(role)) {
                    StoreServerGUI AdminGUI = new StoreServerGUI(port);

                } else if ("user".equals(role)) {
                    ClientGUI ClientGUI = new ClientGUI(username,port);

                }
            } else {
                String message = jsonResponse.optString("message", "Login failed.");
                JOptionPane.showMessageDialog(this, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to server.", "Network Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //When click the register button, open the RegistrationForm method to let the user register
    private void openRegistrationForm(int port) {
    	this.dispose(); 
        RegistrationForm registrationForm = new RegistrationForm(this, true,port);
        registrationForm.setVisible(true);
    }

    // test the function
    public static void main(String[] args) {
    	int port = 12000;//make sure the port is the same as the server
        SwingUtilities.invokeLater(() -> new LoginGUI(port).setVisible(true));
    }
}
