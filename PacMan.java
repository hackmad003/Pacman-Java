import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import java.util.List;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;


public class PacMan extends JPanel implements ActionListener, KeyListener{



    class Block { //START OF BLOCK CLASS
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

            // Update Pacman's image AFTER collision checking to ensure direction and image match
            if(this == pacman) {
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
    }    //END OF BLOCK CLASS



    // Game parameters 
    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

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
            // You would need to add these sound files to your project
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

    public void draw(Graphics g){
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

        // Draw UI
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        
        if(gameOver){
            g.drawString("Game Over! Score: " + score + " Press any key to restart", tileSize/2, tileSize/2);
        } else if(gamePaused){
            g.drawString("PAUSED - Press SPACE to resume", tileSize/2, tileSize/2);
        } else {
            g.drawString("Lives: " + lives + " Score: " + score + " Level: " + level, tileSize/2, tileSize/2);
        }
        
        
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
        if(ghostMoveCounter >= 2){ // Move ghosts every 2 frames
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
                    ghost.updateDirection('U');
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
                powerModeTimer = 300; // 3 seconds at 100ms timer
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

        if(gameOver){
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            gameLoop.start();
            return; 
        }

        System.out.println("KeyEvent: " + e.getKeyCode());

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
