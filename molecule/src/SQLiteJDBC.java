import org.jgrapht.Graph;
import org.jgrapht.graph.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class SQLiteJDBC {
    Connection c = null;
    Statement stmt = null;
    int num_entry;

    public void connect() { //Connect the database
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:molecule.db");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        // System.out.println("Opened database successfully");
    }

    public void createTable() throws SQLException { //Create four tables
        stmt = c.createStatement();
        String sql0 = "CREATE TABLE periodicTable" +
                "(EID           INT         PRIMARY KEY     NOT NULL," +
                " ENAME         VARCHAR                     NOT NULL," +
                " SYMBOL        VARCHAR                     NOT NULL)";
        stmt.executeUpdate(sql0);
        String sql1 = "CREATE TABLE compound" +
                "(CID           INTEGER   PRIMARY KEY  AUTOINCREMENT," +
                " CNAME         VARCHAR                     NOT NULL," +
                " ENUMBER       INT                         NOT NULL)";
        stmt.executeUpdate(sql1);
        String sql2 = "CREATE TABLE compoundElement" +
                "(LID           INT                         NOT NULL," +
                " EID           INT                         NOT NULL," +
                " CID           INT                         NOT NULL," +
                "CONSTRAINT cons1 FOREIGN KEY (EID) REFERENCES periodicTable(EID)," +
                "CONSTRAINT cons2 FOREIGN KEY (CID) REFERENCES compound(CID))";
        stmt.executeUpdate(sql2);
        String sql3 = "CREATE TABLE structure" +
                "(LID1           INT                         NOT NULL," +
                " LID2           INT                         NOT NULL," +
                " CID            INT                         NOT NULL," +
                "CONSTRAINT cons3 FOREIGN KEY (CID) REFERENCES compound(CID))";
        stmt.executeUpdate(sql3);
        stmt.close();
        c.close();
    }

    //Four insert functions
    public void insertPT(int eid, String ename, String symbol) throws SQLException {
        stmt = c.createStatement();
        String sql = "INSERT INTO periodicTable (EID, ENAME, SYMBOL) " +
                "VALUES (" + eid + ", '" + ename + "' , '" + symbol + "');";
        stmt.executeUpdate(sql);
    }

    public int insertCompound(String cname, int enumber) throws SQLException {
        stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM compound WHERE CNAME='" + cname.replace("'", "''") + "';");
        if (rs.next()) {
            return -1;
        }
        String sql = "INSERT INTO compound (CID, CNAME, ENUMBER) " +
                "VALUES (NULL,'" + cname.replace("'", "''") + "', " + enumber + ");";
//        System.out.println(sql);
        stmt.executeUpdate(sql);
        int cid = 0;
        rs = stmt.executeQuery("SELECT * FROM compound WHERE CNAME='" + cname.replace("'", "''") + "';");
        while (rs.next()) {
            cid = rs.getInt("CID");
            break;
        }
        return cid;
    }

    public void insertCE(int lid, String ename, int cid) throws SQLException {
        stmt = c.createStatement();
        int eid = 0;
        ResultSet rs = stmt.executeQuery("SELECT * FROM periodicTable WHERE ENAME='" + ename + "';");

        if (!rs.next()) {
            rs = stmt.executeQuery("SELECT * FROM periodicTable WHERE SYMBOL='" + ename + "';");
        }

        eid = rs.getInt("EID");
//        System.out.println(eid);

        String sql = "INSERT INTO compoundElement (LID, EID, CID) " +
                "VALUES (" + lid + ", " + eid + ", " + cid + ");";
//        System.out.println(sql);
        stmt.executeUpdate(sql);
    }

    public void insertStruct(int lid1, int lid2, int cid) throws SQLException {
        stmt = c.createStatement();
        String sql = "INSERT INTO structure (LID1, LID2, CID) " +
                "VALUES (" + lid1 + ", " + lid2 + ", " + cid + ");";
//        System.out.println(sql);
        stmt.executeUpdate(sql);
    }

    public boolean findMolecule(String FileName) throws SQLException, IOException {
        try {
            ReadFile rf = new ReadFile();
            stmt = c.createStatement();
            Graph<String, DefaultEdge> input = null;
            ArrayList<String> inputFile = null;
            try {
                inputFile = rf.toText(FileName);

                // check if the input is valid
                input = rf.toGraph(inputFile);
                if (input == null) {
                    throw new IOException();
                }

                // search name first
                ResultSet rs = stmt.executeQuery("SELECT CNAME FROM compound WHERE CNAME='" + inputFile.get(0) +  "';");
                if (rs.next()) {
                    System.out.println("Found " + rs.getString("CNAME"));
                    return true;
                }
            } catch (Exception ex) {
                throw new IOException();
            }
            ResultSet rs = stmt.executeQuery("SELECT * FROM compound WHERE ENUMBER='" + input.vertexSet().size() + "';");
            // check graph isomorphic
            while (rs.next()) {
                ArrayList<String> existedMolecule = toText(rs.getInt("CID"));
                Graph<String, DefaultEdge> molecule = rf.toGraph(existedMolecule);

                if (!rf.getFormula(existedMolecule).equals(rf.getFormula(inputFile))) {
                    continue;
                }

                GraphIso pair = new GraphIso(input, molecule);
                if (pair.checkSGI()) {
                    System.out.println("Found " + rs.getString("CNAME"));
                    return true;
                }
            }
            System.out.println("NOT FOUND");
            return false;
        } catch (Exception ex) {
            System.out.println("Finding molecule " + FileName + " failed: invalid molecule.");
            return false;
        }
    }

    // input is a sub graph
    public boolean subGraphSearch(String FileName) throws SQLException, IOException {
        try {
            ReadFile rf = new ReadFile();
            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM compound'" + "';");
            int count = 0;
            Graph<String, DefaultEdge> input = rf.toGraph(rf.toText(FileName));
            Set<String> subNode = input.vertexSet();
            ArrayList<String> subList = new ArrayList<>(subNode);
            while (rs.next()) {
                ArrayList<String> existedMolecule = toText(rs.getInt("CID"));
                // molecule is the base graph in this case
                Graph<String, DefaultEdge> molecule = rf.toGraph(existedMolecule);
                // if molecule does not contain vertex and edge in base graph, skip to next iteration
                if (!molecule.containsVertex(subList.get(0))) {
                    continue;
                }
                try {
                    Set<String> moleSub = molecule.vertexSet();
                    Set<DefaultEdge> moleEdge = molecule.edgeSet();
                    // find induced sub-graph, it they match, print component's name
                    AsSubgraph<String, DefaultEdge> moleSubGraph = new AsSubgraph<>(molecule, moleSub, moleEdge);
                    GraphIso pair = new GraphIso(input, moleSubGraph);
                    if (pair.checkSGI()) {
                        System.out.println("Same subgraph found: " + rs.getString("CNAME"));
                        count = count + 1;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (count == 0) {
                System.out.println("Sub graph NOT FOUND");
                return false;
            } else {
                System.out.println("Finished, there are " + count + " components found.");
                return true;
            }
        } catch (Exception ex) {
            System.out.println("Sub graph NOT FOUND");
            return false;
        }
    }

    public boolean findMostSimilar(String FileName) throws SQLException, IOException {
        // check if there is exact match first
        try {
            ReadFile rf = new ReadFile();
            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM compound'" + "';");

            HashMap<String, Integer> inputFormula = rf.getFormula(rf.toText(FileName));
            Graph<String, DefaultEdge> inputGraph = rf.toGraph(rf.toText(FileName));

            boolean atomFlag = false;
            int totalAtomDifferential = Integer.MAX_VALUE;
            String name = "";
            while (rs.next()) {
                ArrayList<String> existedMolecule = toText(rs.getInt("CID"));
                HashMap<String, Integer> moleculeFormular = rf.getFormula(existedMolecule);
                Graph<String, DefaultEdge> moleculeGraph = rf.toGraph(existedMolecule);

                if (moleculeFormular.equals(inputFormula)) {
                    GraphIso pair = new GraphIso(inputGraph, moleculeGraph);
                    if (pair.checkSGI()) {
                        System.out.println("The most similar molecule found: " + rs.getString("CNAME"));
                        return true;
                    } else {
                        System.out.println("Found a molecule with the same formula: " + name);
                    }
                } else {
                    if (moleculeFormular.keySet().equals(inputFormula.keySet())) {
                        int totalAtoms = 0;
                        for (Integer atoms : moleculeFormular.values()) {
                            totalAtoms += atoms;
                        }
                        int totalAtoms2 = 0;
                        for (Integer atoms : inputFormula.values()) {
                            totalAtoms2 += atoms;
                        }
                        if (totalAtomDifferential > Math.abs(totalAtoms - totalAtoms2)) {
                            totalAtomDifferential = Math.abs(totalAtoms - totalAtoms2);
                            atomFlag = true;
                            name = rs.getString("CNAME");
                        }
                    }
                }
            }

            if (atomFlag) {
                System.out.println("The most similar molecule found: " + name);
                return true;
            } else {
                System.out.println("NOT FOUND");
                return true;
            }
        } catch (Exception ex) {
            System.out.println("Finding most similar molecule of " + FileName + " failed: invalid molecule");
            return false;
        }
    }


    public ArrayList<String> toText(int cid) throws SQLException {
        ArrayList<String> text = new ArrayList<>();
        stmt = c.createStatement();
        Statement newState = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM compound WHERE CID='" + cid + "';");
        text.add(rs.getString("CNAME"));
        text.add(rs.getString("ENUMBER"));
        rs = stmt.executeQuery("SELECT * FROM compoundElement WHERE CID='" + cid + "';");
        while (rs.next()) {
            text.add(newState.executeQuery("SELECT * FROM periodicTable WHERE EID='" + rs.getInt("EID") + "';").getString("ENAME").replace("\"", ""));
        }
        rs = stmt.executeQuery("SELECT * FROM structure WHERE CID='" + cid + "';");
        while (rs.next()) {
            text.add(rs.getString("LID1") + " " + rs.getString("LID2"));
        }
        return text;
    }

    public int getNumOfEntries() throws SQLException {
        connect();
        num_entry = 0;
        stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM compound'" + "';");
        while(rs.next()) num_entry++;
        return num_entry;
    }

    public boolean addMolecule(String FileName) throws SQLException, IOException {
        try {
            ArrayList<String> lines = new ArrayList<>();
            try {
                FileReader fr = new FileReader(FileName);
                BufferedReader bf = new BufferedReader(fr);
                String str;
                while ((str = bf.readLine()) != null) {
                    lines.add(str);
                }
                bf.close();
                fr.close();
            } catch (IOException e) {
                System.out.println("No such file.");
                return false;
            }

            String name = lines.get(0); //Compound name
            System.out.println("Adding " + name + " to the database.");

            int num = Integer.valueOf(lines.get(1)); //Total atomic number
            int cid = insertCompound(name, num); //Save into compound table
            if (cid == -1) {
                System.out.println("Molecule already existed.");
                return false;
            }
            for (int i = 0; i < num; i++) {
                int lid = i;
                String ename = lines.get(i + 2).replace("/", "").replace("\"", "");
                insertCE(lid, ename, cid); //Save LABEL OF VERTEX into compoundElement table
            }
            for (int j = num + 2; j < lines.size(); j++) {
                String[] link = lines.get(j).split(" ");
                int left = Integer.valueOf(link[0]);
                int right = Integer.valueOf(link[1]);
                insertStruct(left, right, cid); //Save adjacent list into structure table
            }
            System.out.println("Successful");
            return true;
        } catch (Exception ex) {
            throw new IOException();
        }
    }

    public boolean findSubgraph(String FileName) throws SQLException, IOException {
        ReadFile rf = new ReadFile();
        stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM compound'" + "';");
        Graph<String, DefaultEdge> inputGraph = rf.toGraph(rf.toText(FileName));

        int numberBonds = rf.getEdge(rf.toText(FileName));
        int numberAtoms = rf.getAtom(rf.toText(FileName));
        boolean bonds_and_elements = false;
        HashMap<String, Integer> inputFormula = rf.getFormula(rf.toText(FileName));

        while(rs.next()) {
            ArrayList<String> existedMolecule = toText(rs.getInt("CID"));
            HashMap<String, Integer> moleculeFormular = rf.getFormula(existedMolecule);
            Graph<String, DefaultEdge> moleculeGraph = rf.toGraph(existedMolecule);

            int numEdges = rf.getEdge(existedMolecule);
            int numAtoms = rf.getAtom(existedMolecule);

            try {
                if(numEdges >= numberBonds && numAtoms >= numberAtoms) {
                    for (String element : inputFormula.keySet()) {
                        if(moleculeFormular.containsKey(element)){
                            bonds_and_elements = true;
                        }
                        else
                            bonds_and_elements = false;
                    }
                }
                if(bonds_and_elements){
                    GraphIso pair = new GraphIso(inputGraph, moleculeGraph);
                    if (pair.checkSGI()) {
                        System.out.println("The Subgraph found: " + rs.getString("CNAME"));
                        return true;
                    }
                }
                bonds_and_elements = false;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("SUB-GRAPH NOT FOUND");
        return false;
    }

}