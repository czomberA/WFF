package pl.edu.pja.dziabor.database;

// src/test/java/pl/edu/pja/dziabor/database/LocalDbManagerTest.java

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.Models.NetworkUpdateDTO;
import pl.edu.pja.dziabor.Models.Profile;
import pl.edu.pja.dziabor.Models.Report; // Assuming you have a Report model
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalDatabaseTest {

    private LocalDatabase localDbManager;
    private Connection testConnection; // The connection to the in-memory H2 database

    @BeforeEach
    void setUp() throws SQLException {
        testConnection = DriverManager.getConnection("jdbc:sqlite:test_local_db.sqlite;DB_CLOSE_DELAY=-1");
        localDbManager = new LocalDatabase(testConnection);
        localDbManager.initDb(); // Initialize tables for each test
        localDbManager.updateLastSuccessfulSyncTimestamp("networks", "1970-01-01 00:00:00.000");

    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.prepareStatement("DROP TABLE IF EXISTS networks;").executeUpdate();
            testConnection.prepareStatement("DROP TABLE IF EXISTS main.reports;").executeUpdate();
            testConnection.close();
        }
    }

    @Test
    void testSaveAndGetNetwork() throws SQLException {
        Network network = new Network("TestNet", "AA:BB:CC:DD:EE:FF", "pass123", LocalDate.now());
        localDbManager.insertNetwork(network);

        List<NetworkUpdateDTO> networks = localDbManager.getChangedNetworks("1970-01-01 00:00:00.000"); // Get all
        assertNotNull(networks);
        assertEquals(1, networks.size());
        assertEquals("TestNet", networks.getFirst().getSSID());
        assertEquals("AA:BB:CC:DD:EE:FF", networks.getFirst().getBssid());
    }

    @Test
    void testSaveAndGetReport() throws SQLException {
        Report report = new Report( "AA:BB:CC:DD:EE:FF", LocalDate.now(), "Private");
        localDbManager.insertReport(report);

        List<Report> reports = localDbManager.getChangedReports();
        assertNotNull(reports);
        assertEquals(1, reports.size());
        assertEquals("AA:BB:CC:DD:EE:FF", reports.getFirst().getBssid());
        assertEquals("Private", reports.getFirst().getReason());
    }

    @Test
    void testDeleteReport() throws SQLException {
        Report report1 = new Report( "AA:BB:CC:DD:EE:FF", LocalDate.now(), "Private");
        Report report2 = new Report( "BB:CC:DD:EE:FF:00", LocalDate.now(), "Unsuccessful");
        localDbManager.insertReport(report1);
        localDbManager.insertReport(report2);

        assertEquals(2, localDbManager.getChangedReports().size());

        localDbManager.clearReports();
        assertEquals(0, localDbManager.getChangedReports().size());
    }

    @Test
    void testSyncTimestampManagement() throws SQLException {
        String initialTimestamp = localDbManager.getLastSuccessfulSyncTimestamp("networks");
        assertEquals("1970-01-01 00:00:00.000", initialTimestamp);

        String newTimestamp = "2024-07-19 10:30:00.000";
        localDbManager.updateLastSuccessfulSyncTimestamp("networks", newTimestamp);

        assertEquals(newTimestamp, localDbManager.getLastSuccessfulSyncTimestamp("networks"));

        String reportTimestamp = localDbManager.getLastSuccessfulSyncTimestamp("reports");
        assertEquals("1970-01-01 00:00:00.000", reportTimestamp);
    }

    @Test
    void testAddProfile() {
        Profile profile = new Profile("TestNetwork", "password123");
        localDbManager.addProfile(profile);
        HashMap<String, Profile> allProfiles = localDbManager.getProfiles();
        assertEquals(1, allProfiles.size());
        assertEquals("TestNetwork", allProfiles.get("TestNetwork").getSSID());
        assertEquals("password123", allProfiles.get("TestNetwork").getPassword());

        Profile profileEmpty = new Profile();
        assertThrows(RuntimeException.class, () -> localDbManager.addProfile(profileEmpty));
        allProfiles = localDbManager.getProfiles();
        assertEquals(1, allProfiles.size());
    }

    @Test
    void testUpdateLastConnectionIsFailed() throws SQLException {
        Network network = new Network("TestNet", "AA:BB:CC:DD:EE:FF", "pass123", LocalDate.now());
        localDbManager.insertNetwork(network);
        localDbManager.updateLastConnection(network, true);
        List<NetworkUpdateDTO> networks = localDbManager.getChangedNetworks("1970-01-01 00:00:00.000");
        assertEquals(1, networks.size(), "Should update existing network, not create a new one.");
        assertTrue(networks.getFirst().isFailed());
    }

    @Test
    void testUpdateLastConnectionNewConnection() throws SQLException {
        LocalDate oldDate = LocalDate.parse("2000-01-01");
        Network network = new Network("TestNet", "AA:BB:CC:DD:EE:FF", "pass123", oldDate);
        localDbManager.insertNetwork(network);
        Network updatedNetwork = new Network("TestNet", "AA:BB:CC:DD:EE:FF", "pass123", LocalDate.now());
        localDbManager.updateLastConnection(updatedNetwork, false);
        List<NetworkUpdateDTO> networks = localDbManager.getChangedNetworks("1970-01-01 00:00:00.000");

        assertEquals(1, networks.size(), "Should update existing network, not create a new one.");
        assertFalse(networks.getFirst().isFailed(), "Only the date of last successful connection should be changed.");
        assertEquals(LocalDate.now(), networks.getFirst().getLastSuccessfulConnection());
        assertNotEquals(oldDate, networks.getFirst().getLastSuccessfulConnection());

    }

    @Test
    void testUpdatePassword() throws SQLException {
        Network network = new Network("TestNet", "AA:BB:CC:DD:EE:FF", "oldPassword", LocalDate.now());
        localDbManager.insertNetwork(network);
        String password = "newPassword";
        localDbManager.updatePassword(network, password);
        List<NetworkUpdateDTO> networks = localDbManager.getChangedNetworks("1970-01-01 00:00:00.000");
        assertEquals(1, networks.size(), "Should update existing network, not create a new one.");
        assertEquals(password, networks.getFirst().getPassword());
        assertNotEquals("oldPassword", networks.getFirst().getPassword());
    }

    @Test
    void testGetAllNetworksFromDB() {

        HashMap<String, Network> savedNetworks = new HashMap<>();
        localDbManager.getAllNetworksFromDB(savedNetworks);
        assertEquals(0, savedNetworks.size());

        localDbManager.insertNetwork(new Network());
        localDbManager.getAllNetworksFromDB(savedNetworks);
        assertEquals(0, savedNetworks.size(), "It should not be possible to add network without fields");

        localDbManager.insertNetwork(new Network("TestNet", "AA:BB:CC:DD:EE:F3", "Password", LocalDate.now()));
        localDbManager.getAllNetworksFromDB(savedNetworks);
        assertEquals(1, savedNetworks.size());
    }
}