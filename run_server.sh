#!/bin/bash

# Server startup script for Multi-threaded Blackjack Game

echo "=== Blackjack Server Startup ==="
echo "Current directory: $(pwd)"
echo "Java version:"
java -version
echo

# Check if compiled classes exist
if [ ! -d "compiled/server" ] || [ ! -d "compiled/shared" ]; then
    echo "❌ Error: Compiled classes not found!"
    echo "Please compile first with: make"
    exit 1
fi

# Check if required classes exist
if [ ! -f "compiled/shared/BlackjackGame.class" ] || [ ! -f "compiled/server/BlackjackServer.class" ]; then
    echo "❌ Error: Required class files not found!"
    echo "Shared classes:"
    ls -la compiled/shared/
    echo "Server classes:"
    ls -la compiled/server/
    exit 1
fi

# Kill any existing rmiregistry processes
echo "🔄 Cleaning up existing RMI processes..."
pkill -f rmiregistry 2>/dev/null
sleep 1

# Check if port 1099 is still in use
if lsof -i :1099 > /dev/null 2>&1; then
    echo "⚠️  Port 1099 still in use, attempting to free it..."
    lsof -ti :1099 | xargs kill -9 2>/dev/null
    sleep 2
fi

# Start RMI registry WITH CLASSPATH (this is the key fix)
echo "🚀 Starting RMI Registry on port 1099 with classpath..."
java -cp "compiled/shared:compiled/server" sun.rmi.registry.RegistryImpl 1099 > rmiregistry.log 2>&1 &
RMI_PID=$!

# Wait for RMI registry to start
sleep 3

# Check if RMI registry started successfully
if ! kill -0 $RMI_PID 2>/dev/null; then
    echo "❌ Failed to start RMI registry"
    cat rmiregistry.log 2>/dev/null || echo "No log file found"
    exit 1
fi

echo "✅ RMI Registry started (PID: $RMI_PID)"

# Start the Blackjack server
echo "🎮 Starting Blackjack Server..."
echo "Classpath: compiled/server:compiled/shared"
echo

# Run server with proper classpath
java -cp "compiled/server:compiled/shared" BlackjackServer &

# Capture server PID
SERVER_PID=$!
echo "✅ Blackjack Server started (PID: $SERVER_PID)"

# Cleanup function
cleanup() {
    echo
    echo "🛑 Shutting down server..."
    if kill -0 $SERVER_PID 2>/dev/null; then
        kill $SERVER_PID
        echo "✅ Server stopped"
    fi
    if kill -0 $RMI_PID 2>/dev/null; then
        kill $RMI_PID
        echo "✅ RMI Registry stopped"
    fi
    # Final cleanup
    pkill -f rmiregistry 2>/dev/null
    exit 0
}

# Trap Ctrl+C
trap cleanup SIGINT SIGTERM

echo
echo "🎮 Blackjack Server is running!"
echo "📋 Available tables:"
echo "   - High Rollers (100-1000 points)"
echo "   - Standard (10-100 points)" 
echo "   - Beginners (1-10 points)"
echo
echo "💡 Press Ctrl+C to stop server"
echo

# Wait for server process
wait $SERVER_PID
