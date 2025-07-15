package pl.edu.pja.dziabor.database;

import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.Models.NetworkUpdateDTO;
import pl.edu.pja.dziabor.Models.Report;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

public class RemoteDatabase {
    //TODO this shouldn't be in code
    private static final String URL = "jdbc:postgresql://localhost:5432/networkappdatabase";
    private static final String USER = "postgres";
    private static final String PASSWORD = "password";
    private Connection connection;
    private Statement stat;

    public RemoteDatabase() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
//        try {
//            connection = DriverManager.getConnection(URL, USER, PASSWORD);
//            stat = connection.createStatement();
//            if (connection != null) {
//                System.out.println("Connected to remote database");
//            }
//        } catch (SQLException e) {
//            System.err.println("Cannot connect to database");
//            e.printStackTrace();
//        }
    }

    public boolean connect() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            System.out.println("Already connected to remote database.");
            return true; // Already connected
        }
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            if (this.connection != null) {
                System.out.println("Connected to remote database.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Cannot connect to remote database: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return false; // Should ideally not be reached if exception is thrown
    }

    public void close() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
            System.out.println("Remote database connection closed.");
        }
        this.connection = null;
    }

    public ArrayList<Network> DownloadNetworkData() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Remote database connection is not open for DownloadNetworkData.");
        }
        ArrayList<Network> connections = new ArrayList<>();
        Network network;
        try{
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM networks");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
               System.out.println("REMOTE:");
                System.out.println(rs.getString("ssid"));
                network = new Network(rs.getString("ssid"), rs.getString("bssid"), rs.getString("password"), LocalDate.parse(rs.getString("lastUse")));
                connections.add(network);
            }
        } catch (SQLException ex) {
            System.out.println("IN REMOTE:" + ex.getMessage());
        }
        return connections;
    }


    public void upsertNetwork(NetworkUpdateDTO network) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Remote database connection is not open for DownloadNetworkData.");
        }
        String checkSql = "SELECT COUNT(*) FROM networks WHERE bssid = ?";
        try (PreparedStatement psCheck = connection.prepareStatement(checkSql)) {
            psCheck.setString(1, network.getBssid());
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Updating network + network");
                    updateNetwork(network);
                } else {
                    System.out.println("Inserting network + network");
                    insertNetwork(network);
                }
            }
        }
    }

    //TODO: here and in pudate, also add lastSynch
    private void insertNetwork(NetworkUpdateDTO network) throws SQLException {
        String insertSql = "INSERT INTO networks (ssid, bssid, password, lastUse, failed) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement psInsert = connection.prepareStatement(insertSql)) {
            psInsert.setString(1, network.getSsid());
            psInsert.setString(2, network.getBssid());
            psInsert.setString(3, network.getPassword());
            psInsert.setDate(4, java.sql.Date.valueOf(network.getLastSuccessfulConnection()));
            psInsert.setBoolean(5, network.isFailed());
            psInsert.executeUpdate();
            System.out.println("Inserted network remotely: " + network.getSsid());
        }
    }

    private void updateNetwork(NetworkUpdateDTO network) throws SQLException {
        String updateSql = "UPDATE networks SET password = ?, lastUse = ?, failed = ? WHERE bssid = ?";
        try (PreparedStatement psUpdate = connection.prepareStatement(updateSql)) {
            psUpdate.setString(1, network.getPassword());
            psUpdate.setDate(2, java.sql.Date.valueOf(network.getLastSuccessfulConnection()));
            psUpdate.setBoolean(3, network.isFailed());
            psUpdate.setString(4, network.getBssid());
            psUpdate.executeUpdate();
            System.out.println("Updated network remotely: " + network.getSsid());
        }
    }

    public void upsertReport(Report report) {
        try{
            if (connection == null || connection.isClosed()) {
                throw new SQLException("Remote database connection is not open for upsertReport.");
            }
            insertReport(report);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void insertReport(Report report) throws SQLException {
        String insertSql = "INSERT INTO reports (bssid, date_of_report, reason) VALUES (?, ?, ?)";
        try (PreparedStatement psInsert = connection.prepareStatement(insertSql)) {
            psInsert.setString(1, report.getBssid());
            psInsert.setString(2, report.getDate().toString());
//            psInsert.setDate(3, java.sql.Date.valueOf(network.getLastSuccessfulConnection())); // Convert LocalDate to java.sql.Date
            psInsert.setString(3, report.getReason());
            psInsert.executeUpdate();
            System.out.println("Inserted report remotely: " + report.getBssid());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
