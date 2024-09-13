import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONException;

public class AdminRegistrationForm extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton createAdminButton;
    private int port;

    public AdminRegistrationForm(int port) {
    	this.port = port;
        setTitle("Create Admin Account");
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

        createAdminButton = new JButton("Create Admin");
        add(createAdminButton);
        createAdminButton.addActionListener(this::createAdminAccount);

        add(new JLabel(""));
    }

    private void createAdminAccount(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }



        try (Socket socket = new Socket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            JSONObject request = new JSONObject();
            request.put("action", "create_admin");
            request.put("username", username);
            request.put("password", password);
            out.writeUTF(request.toString());

            String response = in.readUTF();
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.getString("status").equals("success")) {
                JOptionPane.showMessageDialog(this, "Admin account created successfully.");
                this.dispose();
                new LoginGUI(port).setVisible(true); 
            } else {
                JOptionPane.showMessageDialog(this, jsonResponse.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException | JSONException ex) {
            JOptionPane.showMessageDialog(this, "Network error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

 
}
