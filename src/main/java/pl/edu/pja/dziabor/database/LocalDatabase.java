package pl.edu.pja.dziabor.database;

import pl.edu.pja.dziabor.Models.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class LocalDatabase {
    public static final String DRIVER = "org.sqlite.JDBC";
    public static final String DB_URL = "jdbc:sqlite:LocalDatabase.db";
    private final HashMap<String, Network> savedNetworks = new HashMap<>();
    private static final LocalDatabase instance = new LocalDatabase();
    private static final DateTimeFormatter DB_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private Connection connection;
    private Statement statement;

    private LocalDatabase() {
        try {
            Class.forName(LocalDatabase.DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(DB_URL);
            statement = connection.createStatement();
        } catch (SQLException e) {
            System.err.println("Couldn't connect to database");
            e.printStackTrace();
        }
        CreateTables();
    }

    public static LocalDatabase getInstance() {
        return instance;
    }

    private void CreateTables(){
        String createProfiles = "CREATE TABLE IF NOT EXISTS profiles " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ssid varchar(255), " +
                "password varchar(255), " +
                "creation text" +
                "last_modified_at TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now')))";
        String createDownloadedNetworks = "CREATE TABLE IF NOT EXISTS networks " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ssid varchar(255), " +
                "password varchar(255), " +
                "bssid varchar(255), " +
                "classification varchar(255), " +
                "lastUse text, " +
                "failed bool not null default false," +
                "last_modified_at TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now')))";
        String createReports = "CREATE TABLE IF NOT EXISTS reports " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bssid varchar(255), " +
                "date text, " +
                "reason text," +
                "last_modified_at TEXT DEFAULT (strftime('%Y-%m-%d %H:%M:%f', 'now')))";
        String createSyncStatus = "CREATE TABLE IF NOT EXISTS sync_status" +
                "(table_name TEXT PRIMARY KEY," +
                "last_sync_time TEXT)";
        try {
            statement.execute(createProfiles);
            statement.execute(createDownloadedNetworks);
            statement.execute(createReports);
            statement.execute(createSyncStatus);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void insertNetwork(Network network){
        try {
            PreparedStatement prepStmt = connection.prepareStatement(
                    "insert into networks values (NULL, ?, ?, ?, ?, ?, ?, ?);");
            prepStmt.setString(1, network.getSsid());
            prepStmt.setString(2, network.getPassword());
            prepStmt.setString(3, network.getBssid());
            prepStmt.setString(4, "PL");
            prepStmt.setString(5, LocalDate.now().toString());
            prepStmt.setString(6, "false");
            prepStmt.setString(7, "1970-01-01 00:00:00.000");
            prepStmt.execute();
        } catch (SQLException e) {
            System.err.println("Connection cannot be saved");
            e.printStackTrace();
        }
    }

    public void insertReport(Report report) {
        try {
            PreparedStatement prepStmt = connection.prepareStatement(
                        "INSERT INTO reports VALUES (NULL, ?, ?, ?, ?);");
                prepStmt.setString(1, report.getBssid());
                prepStmt.setString(2, report.getDate().toString());
                prepStmt.setString(3, report.getReason());
                prepStmt.setString(4, LocalDate.now().toString());
                prepStmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Profile> getProfiles() {
        HashMap<String, Profile> profiles = new HashMap<>();
        Profile profile;
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM profiles");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                profile = new Profile(rs.getString("ssid"), rs.getString("password"));
                profiles.put(profile.getSSID(), profile);
                System.out.println("IN LOCAL DB: " + profile);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return profiles;
    }

    public void addProfile(Profile profile) {
        try {
            PreparedStatement prepStmt = connection.prepareStatement("INSERT INTO profiles VALUES (NULL, ?, ?, ?)");
            prepStmt.setString(1, profile.getSSID());
            prepStmt.setString(2, profile.getPassword());
            prepStmt.setString(3, LocalDate.now().toString());
            prepStmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void getAllNetworksFromDB(HashMap<String, Network> networks) {
        try{
            Connection conn = DriverManager.getConnection(DB_URL);

            String query = "SELECT ssid, bssid, password, lastUse FROM networks";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String ssid = rs.getString("ssid");
                    String bssid = rs.getString("bssid");
                    String password = rs.getString("password");
                    String lastUseStr = rs.getString("lastUse");
                    Network dbEntry = new Network();
                    dbEntry.setSsid(ssid);
                    dbEntry.setBssid(bssid);
                    dbEntry.setPassword(password);
                    dbEntry.setLastSuccessfulConnection((LocalDate.parse(lastUseStr)));
                    networks.put(bssid, dbEntry);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void getAllNetworks(){
        getAllNetworksFromDB(savedNetworks);
    }



    public void updateLastConnection(Network network, boolean failed) {
        System.out.println("In last connection: " + network);
        if (failed){
            try{
                PreparedStatement prepStmt = connection.prepareStatement("UPDATE networks set failed = 'TRUE' WHERE bssid = ?");
                prepStmt.setString(1, network.getBssid());
                prepStmt.execute();
                return;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (!failed){
            try {
                String date = LocalDate.now().toString();
                PreparedStatement prepStmt = connection.prepareStatement("UPDATE networks SET lastUse = ? WHERE bssid = ?");
                prepStmt.setString(1, date);      // or setTimestamp, if it's a timestamp
                prepStmt.setString(2, network.getBssid());     // assuming bssid is a String
                prepStmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



    public HashMap<String, Network> getSavedNetworks() {
        return savedNetworks;
    }


    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public List<NetworkUpdateDTO> getChangedNetworks(String lastSyncTimestamp) throws SQLException {
        List<NetworkUpdateDTO> changedNetworks = new ArrayList<>();
        String sql = "SELECT ssid, bssid, password, classification, lastUse, failed FROM networks WHERE last_modified_at < ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, lastSyncTimestamp);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ssid = rs.getString("ssid");
                    String bssid = rs.getString("bssid");
                    String password = rs.getString("password");
                    String classification = rs.getString("classification");
                    LocalDate lastUse = LocalDate.parse(rs.getString("lastUse"));
                    boolean failed = rs.getInt("failed") == 1;
                    changedNetworks.add(new NetworkUpdateDTO(ssid, bssid, password, classification, lastUse, failed));
                }
            }
        }
        return changedNetworks;
    }

    public String getLastSuccessfulSyncTimestamp(String tableName) throws SQLException {
        String timestamp = "1970-01-01 00:00:00.000"; // Default to a very old timestamp
        String sql = "SELECT last_sync_time FROM sync_status WHERE table_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    timestamp = rs.getString("last_sync_time");
                }
            }
        }
        return timestamp;
    }

    public void updateLastSuccessfulSyncTimestamp(String tableName, String timestamp) throws SQLException {
        String sql = "INSERT OR REPLACE INTO sync_status (table_name, last_sync_time) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, timestamp);
            ps.executeUpdate();
        }
    }

    public void clearReports() throws SQLException {
        String sql = "DELETE FROM reports";
        try (PreparedStatement ps = connection.prepareStatement(sql)){
            ps.executeUpdate();
        }
    }

    public List<?> getChangedReports() throws SQLException {
        List<Report> changedReports = new ArrayList<>();
        String sql = "SELECT * FROM reports";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String bssid = rs.getString("bssid");
                    String date = rs.getString("date");
                    String reason = rs.getString("reason");
//                    String classification = rs.getString("classification");
                    changedReports.add(new Report(bssid, LocalDate.parse(date), reason));
                }
            }
        }
        return changedReports;
    }
}
