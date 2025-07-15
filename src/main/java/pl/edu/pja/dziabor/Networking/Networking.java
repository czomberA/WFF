package pl.edu.pja.dziabor.Networking;

import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.Profiles.ProfileManager;
import pl.edu.pja.dziabor.database.SyncManager;

import java.io.IOException;
import java.util.HashMap;

public interface Networking {
    void findNetworks();

    boolean checkConnection(Network network) throws IOException;

    HashMap<String, Network> updateConnectionsFromDB(SyncManager syncManager);

    HashMap<String, Network> getWifiConnectionsAgain(SyncManager syncManager);

    boolean connectToNetwork(Network network, SyncManager syncManager, ProfileManager profileManager, boolean known);
}
