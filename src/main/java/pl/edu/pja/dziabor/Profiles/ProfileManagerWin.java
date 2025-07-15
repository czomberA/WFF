package pl.edu.pja.dziabor.Profiles;

import pl.edu.pja.dziabor.Models.Network;
import pl.edu.pja.dziabor.Models.Profile;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class ProfileManagerWin implements ProfileManager {
    public Profile CreateWifiProfile(Network network){
        try {
            String profileContent = "<?xml version=\"1.0\"?>\n" +
                    "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                    "    <name>" + network.getSsid() + "</name>\n" +
                    "    <SSIDConfig>\n" +
                    "        <SSID>\n" +
                    "            <name>" + network.getSsid() + "</name>\n" +
                    "        </SSID>\n" +
                    "    </SSIDConfig>\n" +
                    "    <connectionType>ESS</connectionType>\n" +
                    "    <connectionMode>manual</connectionMode>\n" +
                    "    <MSM>\n" +
                    "        <security>\n" +
                    "            <authEncryption>\n" +
                    "                <authentication>WPA2PSK</authentication>\n" +
                    "                <encryption>AES</encryption>\n" +
                    "                <useOneX>false</useOneX>\n" +
                    "            </authEncryption>\n" +
                    "            <sharedKey>\n" +
                    "                <keyType>passPhrase</keyType>\n" +
                    "                <protected>false</protected>\n" +
                    "                <keyMaterial>" + network.getPassword() + "</keyMaterial>\n" +
                    "            </sharedKey>\n" +
                    "        </security>\n" +
                    "    </MSM>\n" +
                    "    <MacRandomization xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v3\">\n" +
                    "        <enableRandomization>false</enableRandomization>\n" +
                    "    </MacRandomization>\n" +
                    "</WLANProfile>";

            // Save the profile to a temporary file
            String filePath = System.getProperty("java.io.tmpdir") + network.getSsid() + ".xml";
            System.out.println("FILEPATH: " + filePath);
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(profileContent);
            fileWriter.close();

            Runtime.getRuntime().exec("netsh wlan add profile filename="+ filePath);
            return new Profile(network.getSsid(), network.getPassword());
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }


    public void dropProfile(Network network) {
        try{
             Runtime.getRuntime().exec("netsh wlan delete profile name="+ network.getSsid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkIfProfileExists(Network network, HashMap<String, Profile> profiles) {
        System.out.println("Check if profile exists: " + profiles);
        if (profiles.containsKey(network.getSsid())) {
            Profile profile = profiles.get(network.getSsid());
            if (!profile.getPassword().isEmpty()){
                if (profile.getPassword().equals(network.getPassword())){
                    return true;
                }
            }
        }
        boolean exists = false;
        boolean matchingPassword = false;
        try {
            Process checkProcess = Runtime.getRuntime().exec("netsh wlan show profiles");
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(network.getSsid())) {
                    exists = true;
                }
            }
            if (!exists){
                return false;
            }
            String command = "netsh wlan show profile name=" +network.getSsid()+" key=clear";
            Process passwordCheck = Runtime.getRuntime().exec(command);
            System.out.println(passwordCheck);
            BufferedReader passwordReader = new BufferedReader(new InputStreamReader(passwordCheck.getInputStream()));
            while ((line = passwordReader.readLine()) != null) {
                System.out.println(network.getPassword());
                if (line.contains(network.getPassword())) {
                    matchingPassword = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("SSID: "+exists+" password: "+matchingPassword);
        return exists && matchingPassword;
    }


}
