

import javax.swing.JFrame;
import java.sql.*;

public class App {
    public static void main(String[] args) throws Exception {

        int rowCount = 21;
        int columnCount = 19;
        int tileSize = 32;
        int boardWidth = columnCount * tileSize;
        int boardHeight = rowCount * tileSize;

        JFrame frame = new JFrame("Pac-Man");
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        PacMan pacmanGame = new PacMan();
        frame.add(pacmanGame);
        frame.pack();
        pacmanGame.requestFocus();
        frame.setVisible(true);

        // SQL Server Authentication Tests
        System.out.println("=== SQL Server Authentication Tests ===\n");
        
        // Test 1: SQL Authentication - no encryption
        testConnection("Test 1 - SQL Auth No Encryption", 
            "jdbc:sqlserver://localhost:1433;databaseName=master;user=pacman_user;password=PacMan123!;encrypt=false");
        
        // Test 2: SQL Authentication - with encryption
        testConnection("Test 2 - SQL Auth With Encryption", 
            "jdbc:sqlserver://localhost:1433;databaseName=master;user=pacman_user;password=PacMan123!;encrypt=true;trustServerCertificate=true");
        
        // Test 3: Connect to PacmanDB
        testConnection("Test 3 - PacmanDB Connection", 
            "jdbc:sqlserver://localhost:1433;databaseName=PacmanDB;user=pacman_user;password=PacMan123!;encrypt=false");
        
        // Test 4: Create table test
        createTableTest();
    }
    
    private static void testConnection(String testName, String connectionString) {
        System.out.println("=== " + testName + " ===");
        System.out.println("Connection String: " + connectionString);
        
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("[PASS] Driver loaded successfully");
            
            Connection conn = DriverManager.getConnection(connectionString);
            System.out.println("[PASS] Connection established successfully!");
            
            // Test a simple query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT @@SERVERNAME, @@VERSION");
            
            if (rs.next()) {
                String serverName = rs.getString(1);
                String version = rs.getString(2);
                System.out.println("[PASS] Server: " + serverName);
                System.out.println("[PASS] Version: " + version.substring(0, Math.min(80, version.length())) + "...");
            }
            
            rs.close();
            stmt.close();
            conn.close();
            System.out.println("[PASS] " + testName + " - PASSED\n");
            
        } catch (Exception e) {
            System.out.println("[FAIL] " + testName + " - FAILED");
            System.out.println("[FAIL] Error: " + e.getMessage());
            if (e instanceof SQLException) {
                SQLException sqlEx = (SQLException) e;
                System.out.println("[FAIL] SQL State: " + sqlEx.getSQLState());
                System.out.println("[FAIL] Error Code: " + sqlEx.getErrorCode());
            }
            System.out.println();
        }
    }
    
    private static void createTableTest() {
        System.out.println("=== Creating High Scores Table ===");
        
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:sqlserver://localhost:1433;databaseName=PacmanDB;user=pacman_user;password=PacMan123!;encrypt=false"
            );
            
            Statement stmt = conn.createStatement();
            
            // Create high scores table
            String createTableSQL = """
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='high_scores' AND xtype='U')
                CREATE TABLE high_scores (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    player_name NVARCHAR(50) NOT NULL,
                    score INT NOT NULL,
                    level_reached INT NOT NULL,
                    date_time DATETIME2 DEFAULT GETDATE()
                )
            """;
            
            stmt.executeUpdate(createTableSQL);
            System.out.println("[PASS] High scores table created/verified successfully");
            
            // Insert a test score
            stmt.executeUpdate("INSERT INTO high_scores (player_name, score, level_reached) VALUES ('TestPlayer', 1000, 1)");
            System.out.println("[PASS] Test score inserted successfully");
            
            // Read back the test score
            ResultSet rs = stmt.executeQuery("SELECT TOP 1 player_name, score FROM high_scores ORDER BY id DESC");
            if (rs.next()) {
                System.out.println("[PASS] Retrieved test score: " + rs.getString("player_name") + " - " + rs.getInt("score"));
            }
            
            rs.close();
            stmt.close();
            conn.close();
            System.out.println("[PASS] Database setup complete - ready for Pac-Man!");
            
        } catch (Exception e) {
            System.out.println("[FAIL] Table creation failed: " + e.getMessage());
        }
    }
}