package pl.edu.pja.dziabor.Networking;

import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.Models.NetworkDTOcommand;
import pl.edu.pja.dziabor.Models.Profile;
import pl.edu.pja.dziabor.Profiles.ProfileManager;
import pl.edu.pja.dziabor.database.SyncManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkingWin implements Networking{

    HashMap<String, NetworkDTOcommand> networksFromRead = new HashMap<>();
    HashMap <String, Network> networks = new HashMap<>();
    public void findNetworks(){
        try{
            Process process = Runtime.getRuntime().exec("netsh wlan show networks mode=bssid");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println(line);
            }

            String outputString = output.toString();
            createConnections(outputString);
            reader.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void createConnections(String outputString){
        Pattern pattern = Pattern.compile("SSID \\d+ : (.+?)\\R.*?BSSID \\d+\\s+: ([0-9a-fA-F:]+)\\R.*?Signal\\s+: (\\d+)%", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(outputString);

        while (matcher.find()) {
            String ssid = matcher.group(1).trim();
            String bssid = matcher.group(2).trim();
            int signal = Integer.parseInt(matcher.group(3).trim());
            if (ssid.isEmpty()) {
                ssid = null;
            }
            networksFromRead.put(bssid, new NetworkDTOcommand(ssid, bssid, signal));
        }
    }

    public boolean checkConnection(Network network) throws IOException {
        Process checkProcess = Runtime.getRuntime().exec("netsh wlan show interfaces");
        BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("SSID") && line.contains(network.getSsid())) {
                return true;
            }
        }
        return false;
    }


    //UPDATES WIFIS FROM READ TO DATABASE
    public HashMap<String, Network> updateConnectionsFromDB(SyncManager syncManager) {
        findNetworks();
        try{
            networks = syncManager.updateDataFromDB(networksFromRead);
            return networks;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public HashMap<String, Network> getWifiConnectionsAgain(SyncManager syncManager) {
        networksFromRead.clear();
        findNetworks();
        HashMap<String, NetworkDTOcommand> newNetworks = new HashMap<>();
        for (String b : networksFromRead.keySet()) {
            if (!networks.containsKey(b)) {
                newNetworks.put(b, networksFromRead.get(b));
            }
        }
        if (!newNetworks.isEmpty()) {

            HashMap<String, Network> newNetworksUpdated;
            try {
                newNetworksUpdated = syncManager.updateDataFromDB(newNetworks);
                for (Network n : newNetworksUpdated.values()) {
                    networks.put(n.getBssid(), n);
                }
                return networks;
            } catch (Exception e) {
                System.out.println(e);
                return null;
            }
        }
        for (String b : networksFromRead.keySet()) {
            if (!networks.containsKey(b)) {
                networks.remove(b);
            }
        }
        return networks;
    }


    public boolean connectToNetwork(Network network, SyncManager syncManager, ProfileManager profileManager, boolean known) {

        Profile p;
        boolean newProfile = false;
        if (!profileManager.checkIfProfileExists(network, syncManager.getLocalDatabase().getProfiles()) || !known){
            p = profileManager.CreateWifiProfile(network);
            newProfile = true;
            try{
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            p = new Profile();
        }

        System.out.println("Trying to connect to " + network.getBssid());
        String command = String.format("netsh wlan connect ssid=%s name=%s", network.getSsid(), network.getSsid());

        boolean connected = false;
        try {
            System.out.println(Runtime.getRuntime().exec(command));
            Thread.sleep(5000);
            System.out.println("Wait over");
            if (newProfile){
                Thread.sleep(5000);
            }
            connected = checkConnection(network);
            if (!connected){
                System.out.println("DROP PROFILE");
                profileManager.dropProfile(network);
                System.out.println(network.getBssid()+" failed");
                syncManager.getLocalDatabase().updateLastConnection(network, true);
            } else {
                if (newProfile){
                    p.setSSID(network.getSsid());
                    p.setPassword(network.getPassword());
                    syncManager.getLocalDatabase().addProfile(p);
                }
                if (!known){
                    syncManager.getLocalDatabase().insertNetwork(network);
                }
                System.out.println(network.getBssid()+" update last conn");
                syncManager.getLocalDatabase().updateLastConnection(network, false);
            }
            System.out.println("Connected: " + connected);
        }catch (Exception e){
            e.printStackTrace();
        }

        return connected;
    }

    public HashMap<String, Network> getNetworks() {
        return networks;
    }

    public HashMap<String, NetworkDTOcommand> getNetworksFromRead() {
        return networksFromRead;
    }

    public void setNetworksFromRead(HashMap<String, NetworkDTOcommand> networksFromRead) {
        this.networksFromRead = networksFromRead;
    }

    public void setNetworks(HashMap<String, Network> networks) {
        this.networks = networks;
    }
}


