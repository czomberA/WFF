package pl.edu.pja.dziabor;

import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.Models.Report;
import pl.edu.pja.dziabor.Models.Zone;
import pl.edu.pja.dziabor.Networking.Networking;
import pl.edu.pja.dziabor.Profiles.ProfileManager;
import pl.edu.pja.dziabor.database.SyncManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;

public class WifiListService {
    private static final WifiListService instance = new WifiListService();
    private final Device device = Device.getInstance();
    private final SyncManager syncManager = new SyncManager();
    private Networking networking;
    private ProfileManager profileManager;
    private void initialize(){
        this.networking = device.getNetworking();
        this.profileManager = device.getProfileManager();
    }

    private WifiListService() {
        initialize();
    }

    public static WifiListService getInstance() {
        return instance;
    }


    public HashMap<String, Network> getWifiConnections() {
        return networking.updateConnectionsFromDB(syncManager);
    }

    public void connectToNetwork(Network network) {
        networking.connectToNetwork(network, syncManager, profileManager, true);
    }

    public boolean checkConnection(Network network) throws IOException {
        return networking.checkConnection(network);
    }

    public HashMap<String, Network> getWifiConnectionsAgain() {
        return networking.getWifiConnectionsAgain(syncManager);
    }

    public void downloadData(Zone zone) {
        syncManager.DownloadData(zone);
    }

    public void report(Network n, String reason) {
        syncManager.getLocalDatabase().insertReport(new Report(n.getBssid(), LocalDate.now(), reason));
    }

    public void connectToNewNetwork(Network network, String text) {
        network.setPassword(text);
        networking.connectToNetwork(network, syncManager, profileManager, false);
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public Boolean performSync() {
        return syncManager.performSync();
    }
}
