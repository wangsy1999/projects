import javax.swing.*;
import java.io.IOException;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import org.json.JSONObject;
import org.json.JSONException;

public class Start {
    public static void main(String[] args) {
    	int port = 12000;  //set the port. Before run, check if the server using the same port. 

        SwingUtilities.invokeLater(() -> {
            if (!adminAccountExists(port)) {
                // If there is no admin account, open the admin registration form
                JOptionPane.showMessageDialog(null, "No admin account found. Please create an admin account.",
                        "Admin Setup Required", JOptionPane.INFORMATION_MESSAGE);
                AdminRegistrationForm adminRegistrationForm = new AdminRegistrationForm(port);
                adminRegistrationForm.setVisible(true);
            } else {
                // If admin exists, proceed to login
                new LoginGUI(port).setVisible(true);
            }
        });
    }


    private static boolean adminAccountExists(int port) {
        try (Socket socket = new Socket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            JSONObject request = new JSONObject();
            request.put("action", "check_admin");
            out.writeUTF(request.toString());

            String response = in.readUTF();
            JSONObject jsonResponse = new JSONObject(response);
            return jsonResponse.getString("status").equals("exists");
        } catch (IOException | JSONException e) {
            System.out.println("Network error or JSON parsing error: " + e.getMessage());
            return false;
        }
    }

}
