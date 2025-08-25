#!/bin/bash

# Client startup script for Multi-threaded Blackjack Game

echo "=== Blackjack Client Startup ==="
echo "Current directory: $(pwd)"
echo "Java version:"
java -version
echo

# Check if compiled classes exist
if [ ! -d "compiled/client" ] || [ ! -d "compiled/shared" ]; then
    echo "❌ Error: Compiled classes not found!"
    echo "Please compile first with: make"
    echo "Or run: make client"
    exit 1
fi

# Check if required classes exist
if [ ! -f "compiled/shared/BlackjackGame.class" ] || [ ! -f "compiled/client/BlackjackClient.class" ]; then
    echo "❌ Error: Required class files not found!"
    exit 1
fi

# Start the Blackjack client
echo "🎮 Starting Blackjack Client..."
echo "Classpath: compiled/client:compiled/shared"
echo

# Run client with proper classpath
java -cp "compiled/client:compiled/shared" BlackjackClient

echo "👋 Client session ended"
