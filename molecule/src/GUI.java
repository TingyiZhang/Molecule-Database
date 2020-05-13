import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class GUI extends Component implements ActionListener {
    int num_entry;
    JFrame guiFrame;
    JFrame secondFrame;
    JLabel title;
    JLabel add_label;
    JButton add_button;
    JTextField add_field;
    JLabel search_label;
    JButton search_button;
    JTextField search_field;
    JButton con;
    JLabel label;
    JTextArea data;

    private static SQLiteJDBC db = new SQLiteJDBC();

    ReadFile rf;

    public static void main(String[] args) throws SQLException {
        new GUI();
        db.connect();
        //db.createTable();
    }

    public GUI()
    {
        guiFrame = new JFrame();
        guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiFrame.setTitle("Molecule Database");
        guiFrame.setSize(400,150);
        guiFrame.setLocationRelativeTo(null);

        secondFrame = new JFrame("Molecule Database");
        secondFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        secondFrame.setSize(400,400);
        secondFrame.setLocationRelativeTo(null);
        secondFrame.setLayout(new BorderLayout());
        // secondFrame.setVisible(false);

        /*
        label = new JLabel("Database:");
        data = new JTextArea(400,400);
        data.setBounds(0, 20, 800, 800);
        secondFrame.add(label, BorderLayout.NORTH);
        secondFrame.add(data, BorderLayout.CENTER)
        secondFrame.setVisible(false);
         */

        DefaultTableModel model = new DefaultTableModel();


        final JPanel titlePanel = new JPanel();
        title = new JLabel("Welcome to Molecule Database!");
        titlePanel.add(title);

        final JPanel addPanel = new JPanel();
        addPanel.setVisible(true);
        add_label = new JLabel("Add molecule from: ");
        addPanel.add(add_label);

        add_field = new JTextField(10);
        addPanel.add(add_field);

        add_button = new JButton("Browse..");
        addPanel.add(add_button);

        search_label = new JLabel("Find molecule: ");
        addPanel.add(search_label);

        search_field = new JTextField(10);
        addPanel.add(search_field);

        search_button = new JButton("Browse..");
        addPanel.add(search_button);

        con = new JButton( "Get Database Statistics");

        add_button.addActionListener(this);
        search_button.addActionListener(this);
        con.addActionListener(this);

        guiFrame.add(titlePanel, BorderLayout.NORTH);
        guiFrame.add(addPanel, BorderLayout.CENTER);
        guiFrame.add(con, BorderLayout.SOUTH);
        guiFrame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == add_button) {
            JFileChooser fc = new JFileChooser();
            int i = fc.showOpenDialog(this);
            if (i == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                String filepath = f.getPath();
                add_field.setText(f.getName());

                try {
                    if (db.addMolecule(filepath)) {
                        JOptionPane.showMessageDialog(null, "Compound Added", "Confirmation", JOptionPane.INFORMATION_MESSAGE);
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "Compound already in there", "Confirmation", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (SQLException | IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed: invalid molecule", "Notification", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        }
        else if (e.getSource() == search_button) {
            JFileChooser fc = new JFileChooser();
            int i = fc.showOpenDialog(this);
            if (i == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                String filepath = f.getPath();
                search_field.setText(f.getName());

                try {
                    if (db.findMolecule(filepath)) {
                        JOptionPane.showMessageDialog(null, "Compound Found", "Confirmation", JOptionPane.INFORMATION_MESSAGE);
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "Compound NOT Found", "Confirmation", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (SQLException | IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed: invalid molecule", "Notification", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        }
        else if (e.getSource() == con) {
            //JOptionPane.showMessageDialog(null,"Success","Confirmation", JOptionPane.INFORMATION_MESSAGE);
            // secondFrame.setVisible(true);
            try {
                num_entry = db.getNumOfEntries();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, "There are " + num_entry + " compounds in database", "Database Statistics", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}