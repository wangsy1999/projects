import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.json.JSONObject;
import org.json.JSONException;

public class RegistrationForm extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton registerButton;
    private int port;  

    public RegistrationForm(Frame parent, boolean modal, int port) {
    	this.port = port;
        setTitle("Create Account");
        setSize(300, 200);
        setLayout(new GridLayout(4, 2, 5, 5));
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); 

        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        add(new JLabel("Confirm Password:"));
        confirmPasswordField = new JPasswordField();
        add(confirmPasswordField);

        registerButton = new JButton("Register");
        add(registerButton);
        registerButton.addActionListener(this::performRegistration);

        add(new JLabel(""));
    }


    private void performRegistration(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Socket socket = new Socket("localhost", this.port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Create JSON object with username and password
            JSONObject json = new JSONObject();
            json.put("action", "register");
            json.put("username", username);
            json.put("password", password);

            // Send JSON string to the server
            out.writeUTF(json.toString());

            // Read the response from the server
            String responseStr = in.readUTF();
            JSONObject response = new JSONObject(responseStr);

            // Process the response
            if ("success".equals(response.getString("status"))) {
                JOptionPane.showMessageDialog(this, "Registration successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                new LoginGUI(port).setVisible(true); // Open the login window after creating an admin account
            } else {
                // Display error message from server
                JOptionPane.showMessageDialog(this, response.getString("message"), "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to server", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (JSONException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing JSON", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


}
