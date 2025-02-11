# Makefile for building the CS5700 Project 1 client
JAVAC=javac
JAVA=java
CLIENT_SRC=Client.java
CLIENT_CLASS=Client.class
CLIENT_SCRIPT=client
all: $(CLIENT_SCRIPT)
$(CLIENT_CLASS): $(CLIENT_SRC)
 $(JAVAC) $(CLIENT_SRC)
$(CLIENT_SCRIPT): $(CLIENT_CLASS)
 @echo "#!/bin/sh" > $(CLIENT_SCRIPT)
 @echo "$(JAVA) Client \"$$@\"" >> $(CLIENT_SCRIPT)
 @chmod +x $(CLIENT_SCRIPT)
clean:
 rm -f $(CLIENT_CLASS) $(CLIENT_SCRIPT)