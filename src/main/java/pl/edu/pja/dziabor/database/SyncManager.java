package pl.edu.pja.dziabor.database;

import pl.edu.pja.dziabor.Models.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.SQLException; // Import for SQL exceptions


public class SyncManager {
    private static final DateTimeFormatter DB_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    LocalDatabase localDatabase = LocalDatabase.getInstance();
    //LocalDatabase localDatabase;
    RemoteDatabase remoteDatabase = new RemoteDatabase();

    // TEST CONSTRUCTOR
    SyncManager(LocalDatabase localDatabase) {
        this.localDatabase = localDatabase;
        //this.remoteDatabase = remoteDatabase;
    }

    void setLocalDb(LocalDatabase localDatabase) {
        this.localDatabase = localDatabase;
    }

    public SyncManager() {

    }
    public boolean DownloadData(Zone zone){

        try {
            if (!remoteDatabase.connect()) {
                System.err.println("Failed to connect to remote database. Aborting sync.");
                return false;
            }
            localDatabase.getAllNetworks();
            ArrayList<Network> connections = remoteDatabase.DownloadNetworkData(zone);
            System.out.println("from remote" + connections);
            ArrayList<Network> newNetworks = new ArrayList<>();
            StringBuilder command = new StringBuilder("insert into networks (ssid, password, bssid, classification, lastuse) values ");
            boolean first = true;
            System.out.println("SAVED:" + localDatabase.getSavedNetworks());
            List<Network> toUpdate = new ArrayList<>();
            for (Network network : connections) {
                if(!localDatabase.getSavedNetworks().containsKey(network.getBssid())){
                    newNetworks.add(network);
                    if (!first) {
                        command.append(", ");
                    }
                    command.append(String.format("('%s', '%s', '%s', '%s', '%s')",
                            escapeSql(network.getSsid()),
                            escapeSql(network.getPassword()),
                            escapeSql(network.getBssid()),
                            escapeSql(zone.toString()),
                            escapeSql(network.getLastSuccessfulConnection().toString())));
                    first = false;
                } else {
                    toUpdate.add(network);
                }
            }
            if (!newNetworks.isEmpty()) {
                command.deleteCharAt(command.length() - 1);
                command.append(");");
                System.out.println(command);
                localDatabase.getStatement().execute(command.toString());
            }

            for (Network network : toUpdate) {
                Network stored = localDatabase.getSavedNetworks().get(network.getBssid());
                if (!stored.getPassword().equals(network.getPassword())) {
                    localDatabase.updatePassword(stored, network.getPassword());
                }

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (remoteDatabase != null) {
                    remoteDatabase.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing remote database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    public LocalDatabase getLocalDatabase() {
        return localDatabase;
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    public HashMap<String, Network> updateDataFromDB (Map<String, NetworkDTOcommand> connections){
        HashMap<String, Network> dbMap = new HashMap<>();
        localDatabase.getAllNetworksFromDB(dbMap);
        HashMap<String,Network> updatedNetworks = new HashMap<>();
        for (NetworkDTOcommand wifi : connections.values()) {
            String key = wifi.getBssid();
            Network n;
            if (dbMap.containsKey(key)) {
                n = new Network();
                Network fromDb = dbMap.get(key);
                n.setSsid(fromDb.getSsid());
                n.setBssid(fromDb.getBssid());
                n.setPassword(fromDb.getPassword());
                n.setSignal(wifi.getSignal());
                n.setLastSuccessfulConnection(fromDb.getLastSuccessfulConnection());
            } else {
                n = new Network(wifi);
            }
            updatedNetworks.put(n.getBssid(), n);
        }
        return updatedNetworks;
    }


    public Boolean performSync() {

        boolean overallSuccess = true;
        try {
            if (!remoteDatabase.connect()) {
                System.err.println("Failed to connect to remote database. Aborting sync.");
                return false;
            }
            System.out.println("Starting synchronization process...");
            if (!syncTable("networks")) {
                overallSuccess = false;
            }

            if (!syncTable("reports")) {
                overallSuccess = false;
            }

            if (overallSuccess) {
                System.out.println("All tables synced successfully.");

            } else {
                System.err.println("Synchronization completed with some errors. Check logs for details.");
            }
        } catch (Exception e) {
            overallSuccess = false;
        } finally {
            try {
                if (remoteDatabase != null) {
                    remoteDatabase.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing remote database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return overallSuccess;
    }

    private boolean syncTable(String tableName) throws SQLException {
        if (remoteDatabase.getConnection() == null || remoteDatabase.getConnection().isClosed()) {
            System.err.println("Remote database connection unexpectedly closed during syncTable for " + tableName);
            return false;
        }
        String currentSyncTimestamp = LocalDateTime.now().format(DB_TIMESTAMP_FORMATTER);
        boolean tableSyncSuccess = true;
        List<?> changedItems;

        try {
            if (!remoteDatabase.connect()) {
                throw new RuntimeException("Cannot connect to remote database");
            }
            String lastSyncForTable = localDatabase.getLastSuccessfulSyncTimestamp(tableName);
            System.out.println("  Syncing table: " + tableName + ". Last successful sync for this table: " + lastSyncForTable);

            if ("networks".equals(tableName)) {
                changedItems = localDatabase.getChangedNetworks(lastSyncForTable);
            } else if ("reports".equals(tableName)) {
                changedItems = localDatabase.getChangedReports();
            } else {
                System.err.println("  Unknown table '" + tableName + "' for synchronization.");
                return false;
            }

            if (changedItems.isEmpty()) {
                System.out.println("  No new changes for table: " + tableName);
                return true;
            }

            System.out.println("  Found " + changedItems.size() + " changes for table: " + tableName);

            for (Object item : changedItems) {
                try {
                    if ("networks".equals(tableName) && item instanceof NetworkUpdateDTO) {
                        remoteDatabase.upsertNetwork((NetworkUpdateDTO) item);
                    } else if ("reports".equals(tableName) && item instanceof Report) {
                        remoteDatabase.upsertReport((Report) item);

                    }
                } catch (SQLException e) {
                    System.err.println("  Error pushing item to remote for " + tableName + ": " + e.getMessage());
                    e.printStackTrace();
                    tableSyncSuccess = false; // Mark this table's sync as failed
                }
            }

            if (tableSyncSuccess) {
                localDatabase.updateLastSuccessfulSyncTimestamp(tableName, currentSyncTimestamp);
                System.out.println("  Successfully synced table: " + tableName);
                if (tableName.equals("reports")) {
                    localDatabase.clearReports();
                }
            } else {
                System.err.println("  Table " + tableName + " sync completed with errors. Timestamp not updated.");
            }

        } catch (SQLException e) {
            System.err.println("  Database error during sync for table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
            tableSyncSuccess = false;
        } catch (Exception e) {
            System.err.println("  An unexpected error occurred during sync for table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
            tableSyncSuccess = false;
        }
        return tableSyncSuccess;
    }

    public RemoteDatabase getRemoteDatabase() {
        return remoteDatabase;
    }
}