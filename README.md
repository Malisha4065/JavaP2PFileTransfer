# Java P2P File Transfer

This is a simple peer-to-peer file transfer application in Java. When you run the peer, it prompts you to send or receive files interactively.

## Prerequisites

- Java 8 or higher

## Compile

```bash
javac -d bin src/Peer.java
```

## Usage

After compilation, simply run the peer without arguments; it will prompt for actions:

```bash
java -cp bin Peer
```

When launched, you’ll see a menu:

```
Select an option:
 1) Receive a file
 2) Send a file
 0) Exit
Enter choice:
```

**Receive** mode (choice `1`): you’ll be prompted for the listening port; the incoming file will be saved in the current directory under its original name.  

**Send** mode (choice `2`): you’ll be prompted for the target host, port, and file path; the chosen file will be sent to the receiver.  
Paths beginning with `~` are automatically expanded to your home directory.

Choose `0` to exit the application.