package pl.edu.pja.dziabor.GUI;


import pl.edu.pja.dziabor.Models.Network;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class WifiTableModel extends AbstractTableModel {
        private String[] columnNames =
                {
                        "SSID",
                        "Signal",
                        "Status",
                        "Report"
                };

        private List<Network> networks;





    public WifiTableModel(List<Network> networks) {
            this.networks = networks;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }


    @Override
        public int getRowCount() {
            return networks.size();
        }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 2: return Date.class;
            default: return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        Network network= getNetwork(row);

        switch (column)
        {
            case 0: return network.getSsid();
            case 1: return network.getSignal();
            case 2: return network.getLastSuccessfulConnection();
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column)
    {
        Network network = getNetwork(row);

        switch (column)
        {
            case 0: network.setSsid((String)value); break;
            case 1: network.setSignal((Integer) value); break;
            case 2: network.setLastSuccessfulConnection((LocalDate) value); break;
        }

        fireTableCellUpdated(row, column);
    }

    public Network getNetwork(int row)
    {
        return networks.get( row );
    }

    public void addConnection(Network network)
    {
        insertNetwork(getRowCount(), network);
    }

    public void insertNetwork(int row, Network network)
    {
        networks.add(row, network);
        fireTableRowsInserted(row, row);
    }
    @Override
    public boolean isCellEditable(int row, int column)
    {
        switch (column)
        {
            case 3: return true; // only the report is editable
            default: return false;
        }
    }
    public void setNetworks(List<Network> networks) {
        this.networks = networks;
    }

    //TODO check if chat is right about this one --> it proposes differently
    public void updateData(List<Network> newData) {
 //       System.out.println("In model "+networks);
        this.networks = newData;
        fireTableDataChanged(); // Tell the JTable to refresh itself
    }


}

