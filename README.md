# Multi-threaded Multi-table Blackjack Game

A distributed Blackjack game implementation using Java RMI with multi-threading support, multiple tables, and point-based betting system.

## 🎯 Features

### 🎲 Core Game Features
- **Classic Blackjack Rules**: Hit, stand, dealer must hit until 17+
- **Proper Ace Handling**: Aces count as 11 or 1 automatically
- **Blackjack Payouts**: 3:2 for natural blackjack, 1:1 for regular wins
- **Point-based Betting System**: Virtual currency with buy-in functionality

### 🏓 Multi-table System
- **Multiple Blackjack Tables**: High Rollers, Standard, and Beginners tables
- **Table Selection**: Players choose which table to join
- **Variable Betting Limits**: Different minimum/maximum bets per table
- **Concurrent Gameplay**: Multiple tables running simultaneously

### 💰 Betting & Economy
- **Player Points System**: Virtual currency for betting
- **Buy-in Functionality**: Add more points when needed
- **Dynamic Betting**: Place bets before each hand
- **Bankruptcy Handling**: Options to buy-in more or leave when out of points

### 🧵 Multi-threading & Concurrency
- **Multi-player Support**: Multiple clients can play simultaneously
- **Thread-safe Operations**: Safe concurrent access to game state
- **Session Management**: Unique sessions for each player
- **Scalable Architecture**: Handles multiple concurrent tables

### 🌐 Distributed Architecture
- **Java RMI**: Remote Method Invocation for client-server communication
- **Client-Server Model**: Centralized game logic with distributed clients
- **Network Transparency**: Clients interact with server as if local

## 🛠️ Prerequisites & Environment

### System Requirements
- **Operating System**: Windows Subsystem for Linux (WSL), Linux, or macOS
- **Java**: OpenJDK 11 or higher
- **Build Tools**: GNU Make (optional, for Makefile usage)
- **Memory**: 512MB RAM minimum
- **Network**: Local network access for RMI communication

### Software Dependencies
- **Java Development Kit (JDK)**: For compilation and execution
- **GNU Make**: For using Makefile build system (optional)
- **Bash**: For running shell scripts

## 📦 Installation & Setup

### 1. Install Java in WSL
```bash
# Update package list
sudo apt update

# Install OpenJDK 11
sudo apt install openjdk-11-jdk -y

# Verify installation
java -version
javac -version
```

### 2. Clone the Repository
```bash
# Clone the repository
git clone <repository-url>
cd blackjack_game

# Or create directory structure manually
mkdir -p blackjack_game/{shared,server,client}
```

### 3. Directory Structure
```
blackjack_game/
├── README.md
├── Makefile
├── run_server.sh
├── run_client.sh
├── shared/
│   ├── Card.java
│   ├── Player.java
│   └── BlackjackGame.java
├── server/
│   ├── BlackjackTable.java
│   ├── TableManager.java
│   └── BlackjackServer.java
└── client/
    └── BlackjackClient.java
```

## 🔧 Compilation

### Option 1: Using Makefile (Recommended)
```bash
# Compile everything
make

# Compile specific components
make shared     # Compile shared classes
make server     # Compile server classes
make client     # Compile client classes

# Clean compiled files
make clean
```

### Option 2: Manual Compilation
```bash
# Create directories
mkdir -p compiled/{shared,server,client}

# Compile shared classes
javac -d compiled/shared shared/*.java

# Compile server classes
javac -d compiled/server -cp "compiled/shared" server/*.java

# Compile client classes
javac -d compiled/client -cp "compiled/shared" client/*.java
```

### Option 3: Using Bash Scripts
Create the compilation script:
```bash
# Create compile script
cat > compile_all.sh << 'EOF'
#!/bin/bash
mkdir -p compiled/{shared,server,client}
javac -d compiled/shared shared/*.java
javac -d compiled/server -cp "compiled/shared" server/*.java
javac -d compiled/client -cp "compiled/shared" client/*.java
echo "✅ Compilation complete!"
EOF

chmod +x compile_all.sh
./compile_all.sh
```

## 🚀 Running the Game

### 1. Start the Server
```bash
# Using Makefile
make run-server

# Or using the provided script
chmod +x run_server.sh
./run_server.sh

# Or manually
java -cp "compiled/server:compiled/shared" BlackjackServer
```

### 2. Start Client
```bash
# Using Makefile
make run-client

# Or using the provided script
chmod +x run_client.sh
./run_client.sh

# Or manually
java -cp "compiled/client:compiled/shared" BlackjackClient
```

## 🎮 Game Flow

### Server Side
1. Server starts and creates 3 tables with different betting limits
2. RMI registry is created on port 1099
3. Server waits for client connections

### Client Side
1. **Main Menu**: View available tables and join options
2. **Table Selection**: Choose from High Rollers, Standard, or Beginners
3. **Betting**: Place bets using available points
4. **Gameplay**: Classic Blackjack with hit/stand decisions
5. **Results**: Win/lose points based on game outcome
6. **Bankruptcy**: Buy-in more points or leave table when out of points

## 📋 Available Tables

| Table Name | Min Bet | Max Bet | Description |
|------------|---------|---------|-------------|
| High Rollers | 100 | 1000 | For high-stakes players |
| Standard | 10 | 100 | Balanced betting limits |
| Beginners | 1 | 10 | Low-stakes for new players |

## 🛠️ Development & Customization

### Adding New Tables
Modify `TableManager.java` to add new tables with custom betting limits.

### Customizing Game Rules
Adjust rules in `BlackjackTable.java` for different Blackjack variants.

### Extending Features
- Add player statistics tracking
- Implement tournament modes
- Add chat functionality between players
- Create GUI clients using JavaFX or Swing

## 🐛 Troubleshooting

### Common Issues

1. **"javac: not found"**
   ```bash
   sudo apt install openjdk-11-jdk
   ```

2. **Port already in use**
   ```bash
   # Kill existing processes
   pkill -f rmiregistry
   pkill -f BlackjackServer
   ```

3. **ClassNotFoundException**
   ```bash
   # Ensure proper classpath
   make clean && make
   ```

4. **Connection refused**
   ```bash
   # Check if server is running
   netstat -an | grep 1099
   ```

### Debugging Commands
```bash
# Check Java installation
java -version
javac -version

# Check running processes
ps aux | grep java

# Check network ports
netstat -an | grep 1099

# Clean rebuild
make clean && make
```

## 📊 Project Structure Details

### Core Classes
- **Card.java**: Represents playing cards
- **Player.java**: Manages player state and points
- **BlackjackGame.java**: RMI interface for client-server communication
- **BlackjackTable.java**: Individual table game logic
- **TableManager.java**: Manages multiple tables
- **BlackjackServer.java**: Main server implementation
- **BlackjackClient.java**: Client-side game interface

### Build System
- **Makefile**: Automated build and run system
- **Shell Scripts**: Alternative to Makefile for different workflows

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is for educational purposes. Feel free to use and modify for learning Java RMI, multi-threading, and distributed systems concepts.

## 🎯 Learning Objectives

This project demonstrates:
- Java RMI for distributed computing
- Multi-threading and concurrency
- Object-oriented design patterns
- Client-server architecture
- Session management
- Exception handling in distributed systems

---

**Happy Gaming!** 🃏
