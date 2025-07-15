package pl.edu.pja.dziabor;


import pl.edu.pja.dziabor.GUI.WifiList;

import java.awt.*;

public class Main {

    //TODO: Distinguish open networks and no password --> "__OPEN__" as password

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                WifiList frame = new WifiList();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}