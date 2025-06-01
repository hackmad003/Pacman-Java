import javax.swing.JFrame;  //window

public class App {
    public static void main(String[] args) throws Exception {

        int rowCount = 21; //0-20 
        int columnCount = 19; //0-18
        int tileSize = 32;  //size of each tile in pixels
        // 21 rows and 19 columns, each tile is 32x32 pixels
        // 21 * 32 = 672 pixels height
        // 19 * 32 = 608 pixels width
        // 672 x 608 pixels BOARD Dimensions

        int boardWidth = columnCount * tileSize; // 608 pixels BOARD WIDTH
        int boardHeight = rowCount * tileSize; // 672 pixels

        JFrame frame = new JFrame("Enhanced Pac-Man - AI Ghosts Edition"); //WINDOW TITLE/NEW WINDOW INSTANCE
        
        //frame basically is the window
        //and we have to add the game into the window using the board parameters and the game class that extends JPanel
        
        //frame.setVisible(true);
        frame.setSize(boardWidth, boardHeight); //set the size of the window
        frame.setLocationRelativeTo(null); //center the window on the screen
        frame.setResizable(false); //prevent resizing the window
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //close the application when the window is closed

        PacMan pacmanGame = new PacMan(); //create an instance of the PacMan game class which extends JPanel
        frame.add(pacmanGame); //add the game to the window
        frame.pack(); //pack the window to fit the game size
        pacmanGame.requestFocus(); //request focus for the game to receive key events
        frame.setVisible(true); //make the window visible
    }
}
