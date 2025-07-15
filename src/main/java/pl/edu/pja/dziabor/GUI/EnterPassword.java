package pl.edu.pja.dziabor.GUI;

import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.WifiListService;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class EnterPassword extends JFrame {

    private final JPasswordField passwordField;
    private final JButton validateButton;
    private final JProgressBar progressBar;

    public EnterPassword(WifiListService service, Network network) {
        setTitle("Enter password");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new FlowLayout());

        passwordField = new JPasswordField(15);
        validateButton = new JButton("Connect");

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        validateButton.addActionListener(e -> {
            passwordField.setEnabled(false);
            validateButton.setEnabled(false);
            progressBar.setVisible(true);

            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);
            NetworkConnectWorker worker = new NetworkConnectWorker(service, network, password, passwordChars);
            worker.execute();

            Arrays.fill(passwordChars, '0');
        });

        contentPanel.add(passwordField);
        contentPanel.add(validateButton);

        add(contentPanel, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class NetworkConnectWorker extends SwingWorker<Boolean, Void> {

        private final WifiListService service;
        private final Network network;
        private String password;
        private final char[] passwordCharsToClear;

        public NetworkConnectWorker(WifiListService service, Network network, String password, char[] passwordCharsToClear) {
            this.service = service;
            this.network = network;
            this.password = password;
            this.passwordCharsToClear = passwordCharsToClear;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            service.connectToNewNetwork(network, password);
            return service.checkConnection(network);
        }

        @Override
        protected void done() {
            passwordField.setEnabled(true);
            validateButton.setEnabled(true);
            progressBar.setVisible(false);
            password = null;
            if (passwordCharsToClear != null) {
                Arrays.fill(passwordCharsToClear, '0');
            }

            try {
                Boolean connected = get();
                if (connected) {
                    JOptionPane.showMessageDialog(null, "Connection successful!",
                            "Connection attempt", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Could not connect to the network!",
                            "Connection attempt", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InterruptedException | ExecutionException ex) {
                JOptionPane.showMessageDialog(null,
                        "An error occurred during connection: " + ex.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                dispose();
            }
        }
    }
}