package pl.edu.pja.dziabor.GUI;

import net.miginfocom.swing.MigLayout;
import pl.edu.pja.dziabor.Models.Zone;
import pl.edu.pja.dziabor.WifiListService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DataDownload extends JDialog {
    private final JPanel contentPanel = new JPanel();
    JComboBox<Zone> zonesComboBox;
    public DataDownload(WifiListService service) {
        setTitle("New Assignment");
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 240, 180);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPanel.setVisible(true);
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new MigLayout("", "[][grow]", "[][][]"));
        {
            JLabel WarehouseLabel = new JLabel("Warehouse");
            contentPanel.add(WarehouseLabel, "cell 0 0,alignx trailing");
        }
        {
            zonesComboBox = new JComboBox<Zone>();

            //TODO: display code
            zonesComboBox.addItem(new Zone("PL"));

            contentPanel.add(zonesComboBox, "cell 1 0,growx");
        }

        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("Download");
                okButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        service.downloadData(new Zone(zonesComboBox.getSelectedItem().toString()));
                    }
                });
                okButton.setActionCommand("Download");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton doneButton = new JButton("Done");
                doneButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                });
                doneButton.setActionCommand("Done");
                buttonPane.add(doneButton);
            }
    }
}}
