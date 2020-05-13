import java.sql.SQLException;
import java.io.*;
import java.util.*;

public class Main {
    private static SQLiteJDBC db = new SQLiteJDBC();

    public static void main(String[] args) throws SQLException, IOException {
        System.out.println("Molecule Database Group 6");
        File file = new File("");
        String filepath = file.getAbsolutePath() + "/molecules";

        Main.db.connect();
        ReadFile rf = new ReadFile();

        try {
            Main.db.createTable();
            rf.readCSV("elements.csv");
            System.out.println("An empty database is created.");
            System.out.println("--------------------");
            System.out.println("Database initialization:");
            System.out.println("--------------------");
            System.out.println("Initialize database from \"init\" folder.");
            rf.read(file.getAbsolutePath() + "/init");
            System.out.println("Database updated");
            db.connect();
        } catch (Exception ex) {
            System.out.println("Existed Database Detected");
            System.out.println("There are " + db.getNumOfEntries() + " molecules in the database.");
        }

        System.out.println("Start testing...");
        System.out.println("--------------------");
        System.out.println("Single file adding testing: ");
        long startTime = System.nanoTime();
        System.out.println("--------------------");
        rf.addMolecule("acetylene.txt");
        rf.addMolecule("acetylene.txt");
        rf.addMolecule("carbon_dioxide.txt");
        rf.addMolecule("glucose.txt");
        rf.addMolecule("water.txt");
        rf.addMolecule("test.txt");
        System.out.println("--------------------");
        System.out.println("Find Molecule testing:");
        System.out.println("--------------------");
        db.findMolecule("water1.txt");
        db.findMolecule("glucose.txt");
        db.findMolecule("carbon_dioxide.txt");
        db.findMolecule("acetylene.txt");
        db.findMolecule("test.txt");
        long endTime = System.nanoTime();
        System.out.println();
        System.out.println("The time of basic add and find (11 operations) is: " + ((endTime-startTime)/1000000) + " ms.");
//        System.out.println();
//        System.out.println("--------------------");
//        System.out.println("Find most similar testing:");
//        System.out.println("--------------------");
//        db.findMostSimilar("water1.txt");
//        db.findMostSimilar("test.txt");
//        System.out.println("--------------------");
//        System.out.println("Find sub graph testing:");
//        System.out.println("--------------------");
//        db.findSubgraph("water.txt");
//        db.subGraphSearch("water.txt");
//        db.findSubgraph("test.txt");
//        db.subGraphSearch("test.txt");
        System.out.println("--------------------");
        System.out.println("All tests finished.\n");
        System.out.println("Try \"./md --help\" for instructions.");

        Scanner s = new Scanner(System.in);
        String input = s.nextLine();

        while (input != null) {
            try {
                String[] command = input.trim().split("\\s+");
                if (!command[0].equals("./md")) {
                    System.out.println("Invalid command.");
                    System.out.println("Try \"./md --help\" for instructions.");
                }else if (command[1].equals("--addMolecule")) {
                    try {
                        rf.addMolecule(command[2]);
                    } catch (Exception e) {
                        System.out.println("Filename incorrect");
                    }
                } else if (command[1].equals("--findMolecule")) {
                    try {
                        db.findMolecule(command[2]);
                    } catch (Exception e) {
                        System.out.println("Filename incorrect");
                    }
                } else if (command[1].equals("download")) {
                    System.out.println("Downloading 1000 compounds, may take a while...");
                    GetMolecule pulltest = new GetMolecule();
                    for (int i = 0; i < 1000; i++)
                        pulltest.getMolecule(i); // Download 1,000 known compounds from PubChem into database
                    rf.read(filepath);// Add all compounds from PubChem into database
                    System.out.println("Database updated");
                } else if (command[1].equals("--findMostSimilar")) {
                    try {
                        db.findMostSimilar(command[2]);
                    } catch (Exception e) {
                        System.out.println("Filename incorrect");
                    }
                } else if (command[1].equals("--findSubGraph")) {
                    try {
                        db.findSubgraph(command[2]);
                    } catch (Exception e) {
                        System.out.println("Filename incorrect");
                    }
                } else if (command[1].equals("gui")) {
                    System.out.println("Opening gui...");
                    GUI gui = new GUI();
                    gui.main(null);
                } else if (command[1].equals("exit")) {
                    System.out.println("Exiting...");
                    return;
                } else if (command[1].equals("--help")) {
                    System.out.println("Commands:");
                    System.out.println("--------------------");
                    System.out.println("./md --addMolecule [filename]");
                    System.out.println("./md --findMolecule [filename]");
                    System.out.println("./md --findMostSimilar [filename]");
                    System.out.println("./md --findSubGraph [filename]");
                    System.out.println("./md gui: open GUI");
                    System.out.println("./md download: download 1000 compounds from online database");
                    System.out.println("./md exit");
                    System.out.println("--------------------");
                } else {
                    System.out.println("Invalid command.");
                    System.out.println("Try \"./md --help\" for instructions.");
                }
            } catch(Exception Ex){
                System.out.println("Invalid command.");
                System.out.println("Try \"./md --help\" for instructions.");
            }
            input = s.nextLine();
        }
        System.exit(0);
    }
}
