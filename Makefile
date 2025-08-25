# Makefile for Multi-threaded Blackjack Game

.PHONY: all server client clean

# Java compiler
JAVAC = javac
JAVA = java

# Directories
SHARED_SRC = shared
SERVER_SRC = server
CLIENT_SRC = client
SHARED_CLASSES = compiled/shared
SERVER_CLASSES = compiled/server
CLIENT_CLASSES = compiled/client

all: shared server client

# Compile shared classes
shared:
	mkdir -p $(SHARED_CLASSES)
	$(JAVAC) -d $(SHARED_CLASSES) \
		$(SHARED_SRC)/Card.java \
		$(SHARED_SRC)/Player.java \
		$(SHARED_SRC)/BlackjackGame.java
	@echo "✅ Shared classes compiled"

# Compile server classes
server: shared
	mkdir -p $(SERVER_CLASSES)
	$(JAVAC) -d $(SERVER_CLASSES) \
		-cp "$(SHARED_CLASSES)" \
		$(SERVER_SRC)/BlackjackTable.java \
		$(SERVER_SRC)/TableManager.java \
		$(SERVER_SRC)/BlackjackServer.java
	@echo "✅ Server classes compiled"

# Compile client classes
client: shared
	mkdir -p $(CLIENT_CLASSES)
	$(JAVAC) -d $(CLIENT_CLASSES) \
		-cp "$(SHARED_CLASSES)" \
		$(CLIENT_SRC)/BlackjackClient.java
	@echo "✅ Client classes compiled"

# Start server
run-server: server
	@echo "=== Starting Blackjack Server ==="
	rmiregistry 1099 &
	sleep 2
	$(JAVA) -cp "$(SERVER_CLASSES):$(SHARED_CLASSES)" BlackjackServer

# Start client
run-client: client
	@echo "=== Starting Blackjack Client ==="
	$(JAVA) -cp "$(CLIENT_CLASSES):$(SHARED_CLASSES)" BlackjackClient

# Clean compiled files
clean:
	rm -rf compiled
	@echo "✅ Clean completed!"

# Help
help:
	@echo "Available targets:"
	@echo "  all        - Compile everything"
	@echo "  shared     - Compile shared classes"
	@echo "  server     - Compile server classes"
	@echo "  client     - Compile client classes"
	@echo "  run-server - Start the server"
	@echo "  run-client - Start a client"
	@echo "  clean      - Remove compiled files"
