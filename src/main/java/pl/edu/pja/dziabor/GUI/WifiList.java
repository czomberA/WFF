package pl.edu.pja.dziabor.GUI;

import net.miginfocom.swing.MigLayout;
import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.WifiListService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException; // Added for SwingWorker

public class WifiList extends JFrame {
    private static final int STATUS_COL = 2;
    private HashMap<String, Network> availableNetworks;
    private final WifiTableModel tableModel;
    private final JTable table;
    private final WifiListService service;

    // --- New components for progress indication ---
    private JProgressBar progressBar;
    private JButton refreshButton; // Make these members to enable/disable them
    private JButton updateButton;
    private JButton downloadButton;
    // --- End new components ---

    public WifiList() {
        service = WifiListService.getInstance();
        availableNetworks = service.getWifiConnections();
        if (!availableNetworks.isEmpty()) {
            tableModel = new WifiTableModel(availableNetworks.values().stream().toList());
        } else {
            tableModel = new WifiTableModel(new ArrayList<>());
        }

        setTitle("Wifi Finder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 600, 400);
        setMinimumSize(new Dimension(400, 450));

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        // Updated MigLayout to include space for the progress bar at the bottom
        contentPane.setLayout(new MigLayout("", "[][grow]", "[grow][][grow][][][][]")); // Added one more row for progress bar

        JLabel empLabel = new JLabel("Available networks:");
        contentPane.add(empLabel, "cell 0 0");

        // initialize JTable with model
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // Ensure the model actually contains the STATUS_COL before accessing
                if (getModel().getColumnCount() > STATUS_COL) {
                    Object value = getModel().getValueAt(row, STATUS_COL);
                    if (value instanceof LocalDate) {
                        LocalDate lastUse = (LocalDate) value;
                        LocalDate today = LocalDate.now();

                        if (Math.abs(ChronoUnit.DAYS.between(lastUse, today)) <= 7) {
                            c.setBackground(new Color(180, 255, 180)); // Lighter Green
                            c.setForeground(Color.BLACK);
                        } else if (lastUse.isEqual(LocalDate.MIN)) { // Compare with isEqual for LocalDate.MIN
                            c.setBackground(new Color(255, 180, 180)); // Lighter Red
                            c.setForeground(Color.BLACK);
                        } else {
                            c.setBackground(new Color(255, 255, 180)); // Lighter Yellow
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        // Default background for other types or null
                        c.setBackground(table.getBackground());
                        c.setForeground(table.getForeground());
                    }
                } else {
                    // Default background if STATUS_COL is not available
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }

                // Handle selected rows to maintain visual feedback
//                if (isSelected) {
//                    c.setBackground(table.getSelectionBackground());
//                    c.setForeground(table.getSelectionForeground());
//                }
                return c;
            }
        };

        // Hide the "Status" column (ensure it's not already removed or out of bounds)
        if (table.getColumnModel().getColumnCount() > STATUS_COL) {
            table.removeColumn(table.getColumnModel().getColumn(STATUS_COL));
        }
        // Ensure "Report" column exists before setting renderer/editor
        if (table.getColumnModel().getColumnIndex("Report") != -1) {
            table.getColumn("Report").setCellRenderer(new ButtonRenderer());
            table.getColumn("Report").setCellEditor(new ButtonEditor(new JCheckBox(), table, service));
        }


        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    JTable target = (JTable) me.getSource();
                    int row = target.getSelectedRow();
                    // int col = target.getSelectedColumn(); // col is not used after this
                    if (row >= 0) { // Only check row, as we're getting the Network object
                        WifiTableModel model = (WifiTableModel) table.getModel();
                        Network network = model.getNetwork(row);

                        if (network.getPassword() == null) {
                            // If password is unknown, show the EnterPassword popup
                            EnterPassword enterPassword = new EnterPassword(service, network);
                            // enterPassword.setVisible(true); // Handled by its constructor
                        } else {
                            // If password is known, initiate direct connection with SwingWorker
                            startConnection(network); // Call the new method
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        contentPane.add(scrollPane, "cell 0 1,grow,span 2"); // Span 2 columns

        refreshButton = new JButton("Refresh"); // Initialize as member
        updateButton = new JButton("Share updates"); // Initialize as member

        refreshButton.addActionListener(e -> {
            // Disable UI before refreshing (if refresh is also long-running)
            // For now, assuming refresh is fast, so no SwingWorker here.
            // If service.getWifiConnectionsAgain() is slow, you'd wrap this too.
            availableNetworks.clear();
            availableNetworks = service.getWifiConnectionsAgain();
            System.out.println("In GUI: " + availableNetworks);
            tableModel.updateData(availableNetworks.values().stream().toList());
            // The next two lines are redundant if updateData correctly replaces the list
            // tableModel.setNetworks(availableNetworks.values().stream().toList());
            // tableModel.fireTableDataChanged();
        });

        updateButton.addActionListener(e -> {
            setUiEnabled(false);
            progressBar.setVisible(true);
            ServerUpdateWorker worker = new ServerUpdateWorker();
            worker.execute();


        });
        downloadButton = new JButton("Download Data"); // Initialize as member
        downloadButton.addActionListener(e -> {
            DataDownload dataDownload = new DataDownload(service);
            dataDownload.setVisible(true);
        });

        contentPane.add(refreshButton, "cell 0 2");
        contentPane.add(updateButton, "cell 0 2"); // Check MigLayout for this line, it might overlap with refreshButton depending on exact layout
        // If these buttons should be next to each other, use "cell 0 2, wrap" for refresh and then "cell 1 2" for dataButton
        // Or use "cell 0 2, span 2" for both if they are on the same line.
        // Assuming "cell 0 2" for both means side-by-side in the same cell's row.
        contentPane.add(downloadButton, "cell 1 2"); // This also uses cell 1 2, so it might overlap or be next to dataButton depending on your exact MigLayout setup.

        // Initialize and add the progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false); // Hidden by default
        contentPane.add(progressBar, "cell 0 3, span 2, growx");
        //TODO: implement this
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Application closing. Attempting final cleanup...");
                try {
                    if (service.getSyncManager().getLocalDatabase() != null && service.getSyncManager().getLocalDatabase().getConnection() != null) {
                        service.getSyncManager().getLocalDatabase().close();
                        System.out.println("Local database connection closed.");
                    }
                    // Remote database connection should already be closed by SyncManager after sync
                    // but for safety, ensure its internal connection is closed if it somehow remained open
                    if (service.getSyncManager().getRemoteDatabase() != null) {
                        service.getSyncManager().getRemoteDatabase().close(); // Attempt to close just in case
                    }
                } catch (SQLException ex) {
                    System.err.println("Error closing database connections: " + ex.getMessage());
                }
                super.windowClosing(e);
            }
        });
    }
    private void setUiEnabled(boolean enabled) {
        table.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        updateButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
    }

    private void startConnection(Network network) {
        // Disable UI elements
        table.setEnabled(false);
        refreshButton.setEnabled(false);
        updateButton.setEnabled(false);
        downloadButton.setEnabled(false);
        progressBar.setVisible(true); // Show progress bar

        // Create and execute the SwingWorker
        MainNetworkConnectWorker worker = new MainNetworkConnectWorker(service, network);
        worker.execute();
    }

    // --- Inner class for SwingWorker ---
    private class MainNetworkConnectWorker extends SwingWorker<Boolean, Void> {

        private WifiListService service;
        private Network network;

        public MainNetworkConnectWorker(WifiListService service, Network network) {
            this.service = service;
            this.network = network;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            // This runs in a background thread
            System.out.println("Connecting to " + network.getSsid() + "...");
            service.connectToNetwork(network); // Your original connection method
            return service.checkConnection(network); // Returns true if connected, false otherwise
        }

        @Override
        protected void done() {
            // This runs on the Event Dispatch Thread (EDT)
            // Re-enable UI elements
            table.setEnabled(true);
            refreshButton.setEnabled(true);
            updateButton.setEnabled(true);
            downloadButton.setEnabled(true);
            progressBar.setVisible(false); // Hide progress bar

            try {
                Boolean connected = get(); // Get the result from doInBackground()
                if (connected) {
                    JOptionPane.showMessageDialog(WifiList.this,
                            "Successfully connected to " + network.getSsid() + "!",
                            "Connection Status", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(WifiList.this,
                            "Failed to connect to " + network.getSsid() + ".",
                            "Connection Status", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InterruptedException | ExecutionException ex) {
                // Handle exceptions from the background task
                JOptionPane.showMessageDialog(WifiList.this,
                        "An error occurred during connection: " + ex.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
    private class ServerUpdateWorker extends SwingWorker<Boolean, Void> {
//        private SyncManager syncManager;
//
//        public ServerUpdateWorker(SyncManager syncManager) {
//            this.syncManager = syncManager;
//        }

        @Override
        protected Boolean doInBackground() throws Exception {
            // Call the performSync method which does the actual database synchronization
            // IMPORTANT: Catch exceptions here if you want to explicitly handle them
            // within doInBackground before done() is called.
            // However, SwingWorker's get() in done() will re-throw exceptions from here.
            return service.performSync(); // This will return true/false based on sync success
        }

        @Override
        protected void done() {
            // This method is *always* called after doInBackground() completes,
            // whether it completed normally or threw an exception.
            setUiEnabled(true); // Re-enable all main UI components
            progressBar.setVisible(false); // Hide the progress bar

            try {
                // get() will throw an ExecutionException if an exception occurred in doInBackground()
                Boolean success = get(); // Retrieve the result (true/false) from doInBackground()
                if (success) {
                    JOptionPane.showMessageDialog(WifiList.this,
                            "Updates sent successfully!",
                            "Update Status", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // This covers cases where syncManager.performSync() returned false
                    JOptionPane.showMessageDialog(WifiList.this,
                            "Could not send all updates. Please check logs for details or try again.",
                            "Update Status", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InterruptedException ex) {
                // This means the worker thread was interrupted while waiting for the result
                Thread.currentThread().interrupt(); // Restore the interrupted status
                JOptionPane.showMessageDialog(WifiList.this,
                        "Update process was interrupted: " + ex.getMessage(),
                        "Update Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } catch (ExecutionException ex) {
                // This wraps any exception thrown by doInBackground()
                // The cause is the actual exception from syncManager.performSync()
                Throwable cause = ex.getCause();
                JOptionPane.showMessageDialog(WifiList.this,
                        "An error occurred during update: " + (cause != null ? cause.getMessage() : ex.getMessage()),
                        "Update Error", JOptionPane.ERROR_MESSAGE);
                if (cause != null) {
                    cause.printStackTrace();
                } else {
                    ex.printStackTrace();
                }
            }
        }
    }
}


// Your existing ButtonRenderer and ButtonEditor classes remain unchanged
class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText("Report");
        return this;
    }

}


class ButtonEditor extends DefaultCellEditor {
    private final JButton button;
    private boolean clicked;
    private int row;
    private final JTable table;
    private final WifiListService service;
    private static final String PRIVATE = "Private";
    private static final String UNSUCCESSFUL = "Unsuccessful";

    public ButtonEditor(JCheckBox checkBox, JTable table, WifiListService service) {
        super(checkBox);
        this.table = table;
        this.service = service;
        button = new JButton("Report");
        button.setOpaque(true);
        button.addActionListener(e -> {
            clicked = true;
            fireEditingStopped();
            showReportDialog();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        button.setText("Report");
        clicked = false;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return "Report";
    }

    @Override
    public boolean stopCellEditing() {
        clicked = false;
        return super.stopCellEditing();
    }

    private void showReportDialog() {
        String[] options = {"Private Network", "Connection Unsuccessful"};
        int selection = JOptionPane.showOptionDialog(
                null,
                "Select the reason for reporting:",
                "Network Report",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selection != JOptionPane.CLOSED_OPTION) {
            WifiTableModel model = (WifiTableModel) table.getModel();
            Network n = model.getNetwork(row);

            switch (selection) {
                case 0:
                    service.report(n, PRIVATE);
                    break;
                case 1:
                    service.report(n, UNSUCCESSFUL);
                    break;
                default:
                    break;
            }
            JOptionPane.showMessageDialog(null, "Network reported.",
                    "Report", JOptionPane.INFORMATION_MESSAGE);
        }


    }

}