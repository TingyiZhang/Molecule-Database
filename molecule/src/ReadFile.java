import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;

import java.io.*;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class ReadFile {
    SQLiteJDBC db = new SQLiteJDBC(); // Database initialization

    public ReadFile() {
        db.connect();
    }

    //Read all the txt files in the directory
    public void read(String filepath) throws SQLException, IOException {
        String[] list = new File(filepath).list();
        for (String e : list) {
            try {
                addMolecule(filepath + "/" + e);
            } catch (Exception ex){
                System.out.println("Adding Molecule: " + e + " failed.");
            }
        }
    }

    public void addMolecule(String filename) throws IOException, SQLException {
        try {
            singleRead(filename);
        } catch (Exception ex){
            System.out.println("Adding molecule " + filename + " failed: invalid molecule.");
        }
    }

    //Read a single txt file line by line and save the information of the compound into database
    public void singleRead(String filename) throws SQLException, IOException {
        ArrayList<String> lines = new ArrayList<>();
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader bf = new BufferedReader(fr);
            String str;
            while ((str = bf.readLine()) != null) {
                lines.add(str);
            }
            bf.close();
            fr.close();
        } catch (IOException e) {
            System.out.println("No such file.");
        }

        String name = lines.get(0); //Compound name
        System.out.println("Adding " + name + " to the database.");

        int num = Integer.valueOf(lines.get(1)); //Total atomic number
        int cid = db.insertCompound(name, num); //Save into compound table
        if (cid == -1) {
            System.out.println("Molecule already existed.");
            return;
        }
        for (int i = 0; i < num; i++) {
            int lid = i;
            String ename = lines.get(i + 2).replace("/", "").replace("\"", "");
            db.insertCE(lid, ename, cid); //Save LABEL OF VERTEX into compoundElement table
        }
        for (int j = num + 2; j < lines.size(); j++) {
            String[] link = lines.get(j).split(" ");
            int left = Integer.valueOf(link[0]);
            int right = Integer.valueOf(link[1]);
            db.insertStruct(left, right, cid); //Save adjacent list into structure table
        }
        System.out.println("Successful.");
    }


    // Read the periodical table into database
    public void readCSV(String filePath) {
        db.connect();
        List<List<String>> data = new ArrayList<>();
        String line = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String line0 = bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                String[] items = line.split(",");
                int eid = Integer.valueOf(items[0]);
                String ename = items[2].replace("/", "").replace("\"", "");
                String symbol = items[1].replace("/", "").replace("\"", "");
                db.insertPT(eid, ename, symbol);
            }
        } catch (IOException | SQLException e) {
            System.out.println("Reading "+ filePath + " Failed.");
        }

    }

    public ArrayList<String> toText(String TextFile) throws IOException, SQLException {
        db.connect();
        db.stmt = db.c.createStatement();
        BufferedReader read = new BufferedReader(new FileReader(TextFile));
        String line;
        ArrayList<String> text = new ArrayList<>();
        while((line = read.readLine()) != null) {
            String temp = line.replace("\"", "");
            ResultSet rs = db.stmt.executeQuery("SELECT * FROM periodicTable WHERE ENAME='" + line.replace("\"", "") + "';");
            if (rs.next()) {
                temp = rs.getString("ENAME");
            } else {
                rs = db.stmt.executeQuery("SELECT * FROM periodicTable WHERE SYMBOL='" + line.replace("\"", "") + "';");
                if (rs.next()) {
                    int eid = rs.getInt("EID");
                    rs = db.stmt.executeQuery("SELECT * FROM periodicTable WHERE EID='" + eid + "';");
                    temp = rs.getString("ENAME");
                }
            }
            text.add(temp);
        }
        return text;
    }

    public Graph<String, DefaultEdge> toGraph(ArrayList<String> Text) throws SQLException {
        db.connect();
        db.stmt = db.c.createStatement();
        int count = 0;
        int numberAtoms = 0;
        Vector<String> key = new Vector<>();
        Graph<String, DefaultEdge> theGraph = new Multigraph<>(DefaultEdge.class);

        for (String line : Text) {
            if (count == 0) {
                try {
                    numberAtoms = Integer.valueOf(line);
                    count++;
                }catch(Exception ex){
                    continue;
                }
            }
            if (count == 1) {
                numberAtoms = Integer.valueOf(line);
            } else if (count >= 2){
                key.addElement(line + " " + (count - 2));
                if (count <= numberAtoms + 1) {
                    try {
                        ResultSet rs = db.stmt.executeQuery("SELECT * FROM periodicTable WHERE ENAME='" + line.replace("\"", "") + "';");
                        line = rs.getString("ENAME");
                    } catch (Exception ex) {
                        return null;
                    }
                    theGraph.addVertex(line + " " + (count - 2));
                } else {
                    String[] edge = line.split(" ");
                    theGraph.addEdge(key.get(Integer.parseInt(edge[0])), key.get(Integer.parseInt(edge[1])));
                }
            }
            count++;
        }
        if (count <= 3) {
            return null;
        }

        return theGraph;
    }

    public HashMap<String, Integer> getFormula(ArrayList<String> Text) throws IOException {
        int count = 0;
        int numberAtoms = 0;
        HashMap<String, Integer> formula = new HashMap<>();
        for (String linefind : Text) {
            if (count == 0) {
                try {
                    numberAtoms = Integer.valueOf(linefind);
                    count++;
                }catch(Exception ex){
                    continue;
                }
            }
            if (count == 1) {
                numberAtoms = Integer.valueOf(linefind);
            } else if (count >= 2){
                if (count <= numberAtoms + 1) {
                    formula.merge(linefind, 1, Integer::sum);
                } else {
                    continue;
                }
            }
            count++;
        }
        return formula;
    }

    public int getEdge(ArrayList<String> Text) throws IOException {
        int count = 0;
        int numberBonds = 0;
        for (String line : Text) {
            if (count == 0) {
                try {
                    count++;
                }catch(Exception ex){
                    continue;
                }
            }
            if (count == 1) {
                continue;
            } else if (count >= 2){
                numberBonds++;
            }
            count++;
        }
        return numberBonds;
    }
    public int getAtom(ArrayList<String> Text) throws IOException {
        int count = 0;
        int numberAtoms = 0;

        for (String linefind : Text) {
            if (count == 0) {
                try {
                    numberAtoms = Integer.valueOf(linefind);
                    count++;
                }catch(Exception ex){
                    continue;
                }
            }
            if (count == 1) {
                numberAtoms = Integer.valueOf(linefind);
            } else if (count >= 2){
                if (count <= numberAtoms + 1) {
                    continue;
                } else {
                    continue;
                }
            }
            count++;
        }
        return numberAtoms;
    }
}

