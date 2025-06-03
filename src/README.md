# 🎮 Enhanced Pac-Man Game

A Java-based Pac-Man game with SQL Server database integration, AI-powered ghosts, and progressive difficulty system.


## 🌟 Features

### 🎯 Core Gameplay
- Classic Pac-Man mechanics with modern enhancements
- Smooth 60 FPS gameplay with responsive controls
- Sound effects for immersive experience
- Level progression with increasing difficulty

### 🤖 Intelligent AI Ghosts
- **Red Ghost (Blinky)**: Aggressive direct chaser
- **Pink Ghost (Pinky)**: Ambush strategy - targets 4 tiles ahead
- **Blue Ghost (Inky)**: Patrol patterns
- **Orange Ghost (Clyde)**: Shy behavior - chase when far, flee when close

### 📊 Database Integration
- **SQL Server** high score system
- Persistent score storage across game sessions
- Player name registration
- Top 10 leaderboard display

### 🚀 Progressive Difficulty
- **Ghost Speed**: Increases every 3 levels
- **Power Mode Duration**: Decreases each level (3.0s → 1.0s minimum)
- **Game Speed**: Overall speed increases with each level
- **Strategic Depth**: Higher levels require advanced tactics

### 🎨 Enhanced UI
- Real-time power mode timer with visual countdown bar
- Color-coded power mode indicator (Cyan → Yellow → Red)
- Centered pause screen with semi-transparent overlay
- Live statistics display (Lives, Score, Level, Power Timer)

## 🛠️ Technical Specifications

### Architecture
- **Language**: Java 11+
- **GUI Framework**: Java Swing
- **Database**: Microsoft SQL Server
- **JDBC Driver**: Microsoft SQL Server JDBC Driver 12.10.0

### Data Structures
- `HashSet<Block>` for game entities (O(1) collision detection)
- `ArrayList<Character>` for AI pathfinding
- `String[]` arrays for game map representation

### Database Schema
```sql
CREATE TABLE high_scores (
    id INT IDENTITY(1,1) PRIMARY KEY,
    player_name NVARCHAR(50) NOT NULL,
    score INT NOT NULL,
    level_reached INT NOT NULL,
    date_time DATETIME2 DEFAULT GETDATE()
)
```

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK) 11 or higher**
- **Microsoft SQL Server** (Express edition works)
- **SQL Server Management Studio** (recommended)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/hackmad003/Pacman-Java.git
   cd Pacman-Java
   ```

2. **Download JDBC Driver**
   - Download [Microsoft JDBC Driver for SQL Server](https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server)
   - Place `mssql-jdbc-12.10.0.jre11.jar` in the `lib/` folder

3. **Set up SQL Server**
   ```sql
   -- Enable mixed mode authentication
   -- Create login
   CREATE LOGIN pacman_user WITH PASSWORD = 'PacMan123!';
   
   -- Database will be created automatically by the application
   ```

4. **Compile and Run**
   ```bash
   # Windows
   javac -cp ".;lib/mssql-jdbc-12.10.0.jre11.jar" src/*.java
   java -cp ".;lib/mssql-jdbc-12.10.0.jre11.jar;src" App
   
   # Linux/Mac
   javac -cp ".:lib/mssql-jdbc-12.10.0.jre11.jar" src/*.java
   java -cp ".:lib/mssql-jdbc-12.10.0.jre11.jar:src" App
   ```

## 🎮 How to Play

### Controls
- **Arrow Keys**: Move Pac-Man (Up, Down, Left, Right)
- **SPACE**: Pause/Resume game
- **H**: View high scores (anytime)
- **S**: Save score to database (after game over)
- **ESC**: Return from high scores screen

### Gameplay Elements
- **Yellow Dots**: Regular pellets (10 points each)
- **Red Cherries**: Power pellets (50 points each)
  - Activates power mode
  - Makes ghosts scared and edible (200 points each)
  - Duration decreases with each level
- **Ghosts**: Avoid when normal, chase when scared

### Scoring System
- Regular pellet: **10 points**
- Power pellet: **50 points**
- Scared ghost: **200 points**
- **Bonus**: Completion of each level

## 📁 Project Structure

```
Pacman-Java/
├── src/
│   ├── App.java              # Main application entry point
│   ├── PacMan.java           # Core game logic and GUI
│   └── HighScoreDatabase.java # Database connection class
├── assets/
│   ├── Images/               # Game sprites and graphics
│   │   ├── pacmanUp.png
│   │   ├── redGhost.png
│   │   └── ...
│   └── SoundEffects/         # Audio files
│       ├── pacman_chomp.wav
│       └── ...
├── lib/
│   └── mssql-jdbc-12.10.0.jre11.jar
└── README.md
```

## 🔧 Configuration

### Database Connection
Update connection strings in `HighScoreDatabase.java`:
```java
private static final String DB_URL = "jdbc:sqlserver://localhost:1433;user=pacman_user;password=PacMan123!;encrypt=false;trustServerCertificate=true";
```

### Game Settings
Modify game parameters in `PacMan.java`:
- `tileSize`: Game resolution (default: 32px)
- `gameLoop.setDelay()`: Game speed (default: 100ms)
- Power mode duration calculation in `calculatePowerModeTime()`

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Original Pac-Man concept by Toru Iwatani (Namco)
- Game development inspired by [KennyYopCoding](https://github.com/KennyYopCoding)
- Java Swing documentation and community tutorials
- Microsoft SQL Server documentation

## 📈 Roadmap

- [ ] Multiplayer support
- [ ] Additional ghost AI strategies
- [ ] Custom map editor
- [ ] Achievement system
- [ ] Sound volume controls
- [ ] Mobile version (Android)

## 🐛 Known Issues

- Power mode timer may occasionally display incorrect values during rapid level transitions
- Ghost pathfinding can be optimized for better performance with large maps

## 📞 Contact

**Developer**: hackmad003  
**Project Link**: [https://github.com/hackmad003/Pacman-Java](https://github.com/hackmad003/Pacman-Java)

---

⭐ **Star this repository if you found it helpful!**