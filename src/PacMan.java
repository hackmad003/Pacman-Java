import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import java.util.List;
import javax.sound.sampled.*;
//import java.io.IOException;
import java.net.URL;

import java.sql.*;
import java.time.LocalDateTime;




class HighScoreDatabase {
    // Working connection string with SQL Server Authentication
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;user=pacman_user;password=PacMan123!;encrypt=false;trustServerCertificate=true";

    public static void initializeDatabase() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
            // Connect to master database first to create PacmanDB if needed
            Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            
            // Create database if it doesn't exist
            stmt.executeUpdate("IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'PacmanDB') CREATE DATABASE PacmanDB");
            stmt.close();
            conn.close();
            
            // Now connect to PacmanDB to create table
            String pacmanDbURL = "jdbc:sqlserver://localhost:1433;databaseName=PacmanDB;user=pacman_user;password=PacMan123!;encrypt=false;trustServerCertificate=true";
            conn = DriverManager.getConnection(pacmanDbURL);
            
            // Create table
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
            
            stmt = conn.createStatement();
            stmt.executeUpdate(createTableSQL);
            stmt.close();
            conn.close();
            System.out.println("Connected to SQL Server and created PacmanDB successfully!");
            
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public static void saveScore(String playerName, int score, int level) {
        try {
            String saveURL = "jdbc:sqlserver://localhost:1433;databaseName=PacmanDB;user=pacman_user;password=PacMan123!;encrypt=false;trustServerCertificate=true";
            
            Connection conn = DriverManager.getConnection(saveURL);
            String insertSQL = "INSERT INTO high_scores (player_name, score, level_reached) VALUES (?, ?, ?)";
            
            PreparedStatement stmt = conn.prepareStatement(insertSQL);
            stmt.setString(1, playerName);
            stmt.setInt(2, score);
            stmt.setInt(3, level);
            
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            System.out.println("Score saved: " + playerName + " - " + score + " points!");
            
        } catch (SQLException e) {
            System.out.println("Error saving score: " + e.getMessage());
        }
    }

    public static String[] getTopScores() {
        try {
            String retrieveURL = "jdbc:sqlserver://localhost:1433;databaseName=PacmanDB;user=pacman_user;password=PacMan123!;encrypt=false;trustServerCertificate=true";
            
            Connection conn = DriverManager.getConnection(retrieveURL);
            String selectSQL = "SELECT TOP 10 player_name, score, level_reached, date_time FROM high_scores ORDER BY score DESC";
            
            PreparedStatement stmt = conn.prepareStatement(selectSQL);
            ResultSet rs = stmt.executeQuery();
            
            java.util.List<String> scores = new java.util.ArrayList<>();
            int rank = 1;
            
            while (rs.next()) {
                String playerName = rs.getString("player_name");
                int score = rs.getInt("score");
                int level = rs.getInt("level_reached");
                Timestamp dateTime = rs.getTimestamp("date_time");
                
                String scoreEntry = String.format("%d. %s - %d pts (Level %d)", 
                    rank++, playerName, score, level);
                scores.add(scoreEntry);
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
            if (scores.isEmpty()) {
                return new String[]{"No high scores yet - be the first!"};
            }
            
            return scores.toArray(new String[0]);
            
        } catch (SQLException e) {
            System.out.println("Error retrieving scores: " + e.getMessage());
            return new String[]{"Error loading high scores"};
        }
    }
}









public class PacMan extends JPanel implements ActionListener, KeyListener{

    class Block { 
        int x, y, width, height;
        Image image;
        int startX, startY;
        char direction = 'U';
        int velocityX = 0;
        int velocityY = 0;
        
        // AI properties for ghosts
        String ghostType = "";
        int targetX, targetY;
        boolean isScared = false;
        int scaredTimer = 0;

        // Constructor for Block
        Block(Image image, int x, int y, int width, int height){
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();

            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection; // Revert direction
                    updateVelocity();
                }
            }

            if(this == pacman) {
                updatePacmanImage();
            }
        }
    
        void updateVelocity(){
            int speed = tileSize/4;
            if(this.direction == 'U'){
                this.velocityX = 0;
                this.velocityY = -speed;
            } else if(this.direction == 'D'){
                this.velocityX = 0;
                this.velocityY = speed;
            } else if(this.direction == 'L'){
                this.velocityX = -speed;
                this.velocityY = 0;
            } else if(this.direction == 'R'){
                this.velocityX = speed;
                this.velocityY = 0;
            }
        }

        void reset(){
            this.x = this.startX;
            this.y = this.startY;
            this.isScared = false;
            this.scaredTimer = 0;
        }

        // AI pathfinding for ghosts
        char getAIDirection(){
            if(isScared){
                // Run away from Pacman
                return getFleeDirection();
            }
            
            switch(ghostType){
                case "red": // Aggressive - chase Pacman directly
                    return getChaseDirection(pacman.x, pacman.y);
                case "pink": // Ambush - target 4 tiles ahead of Pacman
                    int targetX = pacman.x + (pacman.direction == 'L' ? -4*tileSize : 
                                             pacman.direction == 'R' ? 4*tileSize : 0);
                    int targetY = pacman.y + (pacman.direction == 'U' ? -4*tileSize : 
                                             pacman.direction == 'D' ? 4*tileSize : 0);
                    return getChaseDirection(targetX, targetY);
                case "blue": // Patrol - move in patterns
                    return getPatrolDirection();
                case "orange": // Shy - chase when far, flee when close
                    double distance = Math.sqrt(Math.pow(this.x - pacman.x, 2) + Math.pow(this.y - pacman.y, 2));
                    if(distance > 8 * tileSize){
                        return getChaseDirection(pacman.x, pacman.y);
                    } else {
                        return getFleeDirection();
                    }
                default:
                    return directions[random.nextInt(4)];
            }
        }
        
        char getChaseDirection(int targetX, int targetY){
            List<Character> validDirections = getValidDirections();
            if(validDirections.isEmpty()) return this.direction;
            
            char bestDirection = validDirections.get(0);
            double minDistance = Double.MAX_VALUE;
            
            for(char dir : validDirections){
                int nextX = this.x, nextY = this.y;
                switch(dir){
                    case 'U': nextY -= tileSize; break;
                    case 'D': nextY += tileSize; break;
                    case 'L': nextX -= tileSize; break;
                    case 'R': nextX += tileSize; break;
                }
                
                double distance = Math.sqrt(Math.pow(nextX - targetX, 2) + Math.pow(nextY - targetY, 2));
                if(distance < minDistance){
                    minDistance = distance;
                    bestDirection = dir;
                }
            }
            return bestDirection;
        }
        
        char getFleeDirection(){
            List<Character> validDirections = getValidDirections();
            if(validDirections.isEmpty()) return this.direction;
            
            char bestDirection = validDirections.get(0);
            double maxDistance = 0;
            
            for(char dir : validDirections){
                int nextX = this.x, nextY = this.y;
                switch(dir){
                    case 'U': nextY -= tileSize; break;
                    case 'D': nextY += tileSize; break;
                    case 'L': nextX -= tileSize; break;
                    case 'R': nextX += tileSize; break;
                }
                
                double distance = Math.sqrt(Math.pow(nextX - pacman.x, 2) + Math.pow(nextY - pacman.y, 2));
                if(distance > maxDistance){
                    maxDistance = distance;
                    bestDirection = dir;
                }
            }
            return bestDirection;
        }
        
        char getPatrolDirection(){
            List<Character> validDirections = getValidDirections();
            if(validDirections.isEmpty()) return this.direction;
            
            // Prefer continuing straight, then turn
            if(validDirections.contains(this.direction)){
                return this.direction;
            }
            return validDirections.get(random.nextInt(validDirections.size()));
        }
        
        List<Character> getValidDirections(){
            List<Character> valid = new ArrayList<>();
            char opposite = getOppositeDirection(this.direction);
            
            for(char dir : directions){
                if(dir == opposite) continue; // Don't reverse direction
                
                int nextX = this.x, nextY = this.y;
                switch(dir){
                    case 'U': nextY -= tileSize; break;
                    case 'D': nextY += tileSize; break;
                    case 'L': nextX -= tileSize; break;
                    case 'R': nextX += tileSize; break;
                }
                
                Block testBlock = new Block(null, nextX, nextY, this.width, this.height);
                boolean validMove = true;
                for(Block wall : walls){
                    if(collision(testBlock, wall)){
                        validMove = false;
                        break;
                    }
                }
                if(validMove) valid.add(dir);
            }
            return valid;
        }
        
        char getOppositeDirection(char dir){
            switch(dir){
                case 'U': return 'D';
                case 'D': return 'U';
                case 'L': return 'R';
                case 'R': return 'L';
                default: return 'U';
            }
        }
    }    




    // Game parameters 
    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    // High scores display
    private boolean showingHighScores = false;
    private String[] topScores;

    // Images
    private Image wallImage;
    private Image pelletImage;
    private Image scaredGhostImage; 
    private Image cherryImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image redGhostImage;
    private Image pinkGhostImage;
    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    // Tile map representation
    // Each character represents a tile type
    // 'X' = wall, ' ' = food, 'C' = cherry, 'b' = blue ghost, 'o' = orange ghost,
    // 'p' = pink ghost, 'r' = red ghost, 'P' = Pacman, 'O' = power pellet
    // 'O' is used for empty space
    private String[] tileMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "X  C     X      C X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXrXX X XXXX",
            "O       bpo       O",
            "XXXX X XXXXX X XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXXXX X XXXX",
            "X        P        X",
            "X XX XXX X XXX XX X",
            "X  X           X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X  C            C X",
            "XXXXXXXXXXXXXXXXXXX"
    };



    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    HashSet<Block> cherries;
    Block pacman;
    Timer gameLoop;


    private int currentGhostMoveDelay = 2; 
    char[] directions = {'U', 'D', 'L', 'R'};
    Random random = new Random();
    int score = 0;
    int lives = 3;
    int level = 1;
    boolean gameOver = false;
    boolean gamePaused = false;
    boolean powerMode = false;
    int powerModeTimer = 0;
    int ghostMoveCounter = 0;

    // Sound clips
    //private Clip backgroundMusic;
    private Clip beginningSound;
    private Clip chompSound;
    private Clip deathSound;
    private Clip eatFruitSound;
    private Clip intermissionSound;

    // Constructor
    PacMan(){
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        HighScoreDatabase.initializeDatabase(); // Initialize the database connection

        loadImages();
        loadSounds();
        loadMap();

        for(Block ghost : ghosts){
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        //how often the game updates, 
        gameLoop = new Timer(100, this); // 100ms update interval
        gameLoop.start();

    }

    


    private void loadImages(){
        try {
            // Load images from resources
            wallImage = new ImageIcon(getClass().getResource("assets/Images/wall.png")).getImage();
            pelletImage = new ImageIcon(getClass().getResource("assets/Images/food.png")).getImage();
            cherryImage = new ImageIcon(getClass().getResource("assets/Images/cherry.png")).getImage();
            scaredGhostImage = new ImageIcon(getClass().getResource("assets/Images/scaredGhost.png")).getImage();
            // Ghost images
            blueGhostImage = new ImageIcon(getClass().getResource("assets/Images/blueGhost.png")).getImage();
            orangeGhostImage = new ImageIcon(getClass().getResource("assets/Images/orangeGhost.png")).getImage();
            pinkGhostImage = new ImageIcon(getClass().getResource("assets/Images/pinkGhost.png")).getImage();
            redGhostImage = new ImageIcon(getClass().getResource("assets/Images/redGhost.png")).getImage();
            // Pacman images for different directions
            pacmanUpImage = new ImageIcon(getClass().getResource("assets/Images/pacmanUp.png")).getImage();
            pacmanDownImage = new ImageIcon(getClass().getResource("assets/Images/pacmanDown.png")).getImage();
            pacmanLeftImage = new ImageIcon(getClass().getResource("assets/Images/pacmanLeft.png")).getImage();
            pacmanRightImage = new ImageIcon(getClass().getResource("assets/Images/pacmanRight.png")).getImage();
            
        } catch (Exception e) { // Handle image loading errors
            System.out.println("Could not load images: " + e.getMessage());
        }
    }

    private void loadSounds(){
        try {
            beginningSound = loadSound("assets/SoundEffects/pacman_beginning.wav");
            chompSound = loadSound("assets/SoundEffects/pacman_chomp.wav");
            deathSound = loadSound("assets/SoundEffects/pacman_death.wav");
            eatFruitSound = loadSound("assets/SoundEffects/pacman_eatfruit.wav");
            intermissionSound = loadSound("assets/SoundEffects/pacman_intermission.wav");
            
            // Play beginning sound when game starts
            if(beginningSound != null){
                playSound(beginningSound);
            }
        } catch (Exception e) {
            System.out.println("Could not load sounds: " + e.getMessage());
        }
    }

    private Clip loadSound(String filename){
        try {
            URL soundURL = getClass().getResource(filename);
            if(soundURL != null){
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                return clip;
            }
        } catch (Exception e) {
            System.out.println("Error loading sound " + filename + ": " + e.getMessage());
        }
        return null;
    }

    private void playSound(Clip clip){
        if(clip != null){
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void loadMap(){
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        cherries = new HashSet<Block>();
        ghosts = new HashSet<Block>();

        for(int r = 0; r < rowCount; r++){
            for(int c = 0; c < columnCount; c++){
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c*tileSize;
                int y = r*tileSize;

                if(tileMapChar == 'X'){
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b'){
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = "blue";
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'o'){
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = "orange";
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'p'){
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = "pink";
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'r'){
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = "red";
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'P'){
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                    //pacman.updateDirection('R'); // Start facing right
                }
                else if(tileMapChar == ' '){
                    // Center small pellets in the tile
                    int pelletSize = tileSize / 6; // Make pellets much smaller
                    int offsetX = (tileSize - pelletSize) / 2;
                    int offsetY = (tileSize - pelletSize) / 2;
                    Block food = new Block(pelletImage, x + offsetX, y + offsetY, pelletSize, pelletSize);
                    foods.add(food);
                }
                else if(tileMapChar == 'C'){
                    Block cherry = new Block(cherryImage, x, y, tileSize, tileSize);
                    cherries.add(cherry);
                }
            }
        }
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        draw(g);
    }

    // Add this method to prompt for player name and save score
    private void saveHighScore() {
        String playerName = javax.swing.JOptionPane.showInputDialog(
            this,
            "Enter your name for the high score:",
            "High Score!",
            javax.swing.JOptionPane.PLAIN_MESSAGE
        );
        
        if (playerName != null && !playerName.trim().isEmpty()) {
            HighScoreDatabase.saveScore(playerName.trim(), score, level);
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Score saved to database!",
                "Success",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        }
    }


    private int calculatePowerModeTime() {
        // Base time is 300 (3 seconds at 100ms timer)
        // Decrease by 20 each level, minimum of 100 (1 second)
        int baseTime = 300;
        int reduction = (level - 1) * 20;
        int powerTime = Math.max(100, baseTime - reduction);
        
        System.out.println("Level " + level + " - Power mode duration: " + (powerTime / 10.0) + " seconds");
        return powerTime;
    }


    private void showHighScores() {
        showingHighScores = true;
        topScores = HighScoreDatabase.getTopScores();
        repaint();
    }



    public void draw(Graphics g){
        // Handle high scores display
        if (showingHighScores) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, boardWidth, boardHeight);
            
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("HIGH SCORES", boardWidth/2 - 80, 50);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            
            for (int i = 0; i < topScores.length && i < 10; i++) {
                g.drawString(topScores[i], 50, 100 + (i * 30));
            }
            
            g.setColor(Color.CYAN);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("Press ESC to return", boardWidth/2 - 80, boardHeight - 50);
            return;
        }
        // Draw pacman
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        // Draw ghosts
        for(Block ghost : ghosts){
            if(ghost.isScared && powerMode){
                g.drawImage(scaredGhostImage, ghost.x, ghost.y, ghost.width, ghost.height, null);
            } else {
                g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
            }
        }

        // Draw walls
        for(Block wall : walls){
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        // Draw food
        for(Block food : foods){
            g.drawImage(food.image, food.x, food.y, food.width, food.height, null);
        }

        // Draw cherries
        for(Block cherry : cherries){
            //g.fillOval(cherry.x, cherry.y, cherry.width, cherry.height);
            g.drawImage(cherry.image, cherry.x, cherry.y, cherry.width, cherry.height, null);
        }

        // Draw UI - 
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        
        if(gameOver){
            g.drawString("Game Over! Score: " + score + " Press any key to restart", tileSize/2, tileSize/2);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Press S to save score | Press H to view high scores", tileSize/2, tileSize/2 + 30);
        } else if(gamePaused){
            // Create semi-transparent overlay
            g.setColor(new Color(0, 0, 0, 150)); // Black with 150/255 transparency
            g.fillRect(0, 0, boardWidth, boardHeight);
            
            // Main pause text - large and centered
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            
            String pauseText = "PAUSED";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(pauseText);
            int textHeight = fm.getHeight();
            
            // Center the text horizontally and vertically
            int x = (boardWidth - textWidth) / 2;
            int y = (boardHeight - textHeight) / 2 + fm.getAscent();
            
            g.drawString(pauseText, x, y);
            
            // Subtitle text - smaller and centered below
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            
            String resumeText = "Press SPACE to resume";
            FontMetrics fm2 = g.getFontMetrics();
            int resumeWidth = fm2.stringWidth(resumeText);
            int resumeX = (boardWidth - resumeWidth) / 2;
            int resumeY = y + 40; // 40 pixels below the main text
            
            g.drawString(resumeText, resumeX, resumeY);
            
            // High scores instruction - centered below that
            String highScoreText = "Press H to view high scores";
            int highScoreWidth = fm2.stringWidth(highScoreText);
            int highScoreX = (boardWidth - highScoreWidth) / 2;
            int highScoreY = resumeY + 30; // 30 pixels below resume text
            
            g.drawString(highScoreText, highScoreX, highScoreY);
        } else {
            // Main game UI with power mode timer
            String mainUI = "Lives: " + lives + " Score: " + score + " Level: " + level;
            
            // Add power mode timer if active
            if(powerMode && powerModeTimer > 0){
                double remainingTime = powerModeTimer / 10.0; // Convert to seconds
                g.setColor(Color.CYAN); // Different color for power mode info
                mainUI += String.format(" | POWER: %.1fs", remainingTime);
            }
            
            g.drawString(mainUI, tileSize/2, tileSize/2);
            
            // Instructions below
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("Press H for high scores", tileSize/2, tileSize/2 + 25);
            
            // Optional: Power mode indicator bar
            if(powerMode && powerModeTimer > 0){
                drawPowerModeBar(g);
            }
        }
        
        
    }

    private void drawPowerModeBar(Graphics g) {
        // Calculate the original power mode time for this level
        int maxPowerTime = calculatePowerModeTime();
        
        // Bar dimensions
        int barWidth = 150;
        int barHeight = 8;
        int barX = tileSize/2;
        int barY = tileSize/2 + 40;
        
        // Background bar (empty)
        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Calculate filled portion
        double fillPercentage = (double) powerModeTimer / maxPowerTime;
        int fillWidth = (int) (barWidth * fillPercentage);
        
        // Color changes based on remaining time
        if (fillPercentage > 0.6) {
            g.setColor(Color.CYAN);      // High time - cyan
        } else if (fillPercentage > 0.3) {
            g.setColor(Color.YELLOW);    // Medium time - yellow
        } else {
            g.setColor(Color.RED);       // Low time - red (warning!)
        }
        
        // Draw filled portion
        g.fillRect(barX, barY, fillWidth, barHeight);
        
        // Border around the bar
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight);
        
        // Label
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("POWER MODE", barX, barY - 2);
    }

    private int calculateGhostMoveDelay() {
        // Start with ghosts moving every 2 frames (current speed)
        // Reduce delay as levels increase, making them faster
        int baseDelay = 2;
        int reduction = (level - 1) / 3; // Every 3 levels, reduce delay by 1
        int ghostDelay = Math.max(1, baseDelay - reduction); // Minimum of 1 (move every frame)
        
        System.out.println("Level " + level + " - Ghost move delay: " + ghostDelay + " frames");
        return ghostDelay;
    }
    
    private void updatePacmanImage(){
        if(pacman.direction == 'U'){
            pacman.image = pacmanUpImage;
        } else if (pacman.direction == 'D'){
            pacman.image = pacmanDownImage;
        } else if (pacman.direction == 'L'){
            pacman.image = pacmanLeftImage;
        } else if (pacman.direction == 'R'){
            pacman.image = pacmanRightImage;
        }
    }

    public void move(){
        if(gamePaused || gameOver) return;

        // Move pacman
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        handleWrapAround(pacman);

        // Check wall collisions for pacman 
        for(Block wall : walls){
            if(collision(pacman, wall)){
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // Handle power mode timer
        if(powerMode){
            powerModeTimer--;
            if(powerModeTimer <= 0){
                powerMode = false;
                for(Block ghost : ghosts){
                    ghost.isScared = false;
                }
            }
        }

        // Move ghosts with AI (slower movement)
        ghostMoveCounter++;
        if(ghostMoveCounter >= currentGhostMoveDelay){ // Move ghosts every 2 frames
            ghostMoveCounter = 0;
            
            for(Block ghost : ghosts){
                // AI decision making
                if(random.nextInt(10) < 3){ // 30% chance to change direction
                    char aiDirection = ghost.getAIDirection();
                    if(aiDirection != ghost.direction){
                        ghost.updateDirection(aiDirection);
                    }
                }

                // Check ghost-pacman collision
                if(collision(ghost, pacman)){
                    if(ghost.isScared && powerMode){
                        // Ghost eaten
                        score += 200;
                        playSound(eatFruitSound); // Use fruit sound for eating ghosts
                        ghost.reset();
                    } else {
                        // Pacman caught
                        lives--;
                        playSound(deathSound);
                        if(lives <= 0){
                            gameOver = true;
                            return;
                        }
                        resetPositions();
                    }
                }

                // Special behavior for ghost in center
                if (ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                    ghost.updateDirection('D');
                }

                // Move ghost
                ghost.x += ghost.velocityX;
                ghost.y += ghost.velocityY;
                handleWrapAround(ghost);

                // Check wall collisions for ghost
                for (Block wall : walls) {
                    if (collision(ghost, wall)) {
                        ghost.x -= ghost.velocityX;
                        ghost.y -= ghost.velocityY;
                        char newDirection = ghost.getAIDirection();
                        ghost.updateDirection(newDirection);
                    }
                }
            }
        }

        // Check food consumption
        Block foodEaten = null;
        for(Block food : foods){
            if(collision(pacman, food)){
                foodEaten = food;
                score += 10;
                playSound(chompSound);
            }
        }
        foods.remove(foodEaten);

        // Check power pellet consumption
        Block cherryEaten = null;
        for(Block cherry : cherries){
            if(collision(pacman, cherry)){
                cherryEaten = cherry;
                score += 50;
                powerMode = true;
                powerModeTimer = calculatePowerModeTime();
                //powerModeTimer = 300; // 3 seconds at 100ms timer
                playSound(eatFruitSound); // Use fruit sound for power pellets
                
                // Make all ghosts scared
                for(Block ghost : ghosts){
                    ghost.isScared = true;
                }
            }
        }
        cherries.remove(cherryEaten);

        // Check win condition
        if(foods.isEmpty() && cherries.isEmpty()){
            level++;
            playSound(intermissionSound); // Play intermission sound between levels
            currentGhostMoveDelay = calculateGhostMoveDelay();
            loadMap();
            resetPositions();
            // Increase difficulty slightly
            gameLoop.setDelay(Math.max(50, gameLoop.getDelay() - 5));
        }
    }

    private void handleWrapAround(Block entity){
        if(entity.x < 0){
            entity.x = boardWidth - entity.width;
        } else if (entity.x >= boardWidth){
            entity.x = 0;
        }

    }

    public boolean collision(Block a, Block b){
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    public void resetPositions(){
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        for(Block ghost : ghosts){
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];

            ghost.updateDirection(newDirection);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if(gameOver){
            gameLoop.stop();
        }

    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        
        // Handle high scores screen
        if (showingHighScores) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                showingHighScores = false;
                repaint();
            }
            return;
        }

        if(gameOver){
            // ADD these new key handlers for game over screen
            if(e.getKeyCode() == KeyEvent.VK_S) {
                saveHighScore();
                return;
            }
            if(e.getKeyCode() == KeyEvent.VK_H) {
                showHighScores();
                return;
            }
            
            // Your existing game over logic
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            gameLoop.start();
            return; 
        }

        System.out.println("KeyEvent: " + e.getKeyCode());

        if(e.getKeyCode() == KeyEvent.VK_H){
            showHighScores();
            return;
        }

        if(e.getKeyCode() == KeyEvent.VK_SPACE){
            gamePaused = !gamePaused;
            if(gamePaused){
                gameLoop.stop();
                repaint();
            } else {
                gameLoop.start();
            }
            return;
        }

        // Only handle movement if game is not paused
        if(!gamePaused){
            if(e.getKeyCode() == KeyEvent.VK_UP){
                pacman.updateDirection('U');
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN){
                pacman.updateDirection('D');
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT){
                pacman.updateDirection('L');
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT){
                pacman.updateDirection('R');
            }
        }

    }


}
//SHOUT OUT TO KENNYYOPCODING FOR THE PACMAN GAME tutorial 
//https://www.youtube.com/watch?v=lB_J-VNMVpE&t=2011s&pp=ygUQa2Vubnl5aXAgcGFjbWFubQ%3D%3D