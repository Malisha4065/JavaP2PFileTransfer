# Java P2P File Transfer

This is a simple peer-to-peer file transfer application in Java. You can start one peer in receive mode to listen for incoming file transfers, and another peer in send mode to send files.

## Prerequisites

- Java 8 or higher

## Compile

```bash
javac -d bin src/Peer.java
```

## Usage

### Receive

```bash
java -cp bin Peer receive <port>
```

This will start a server listening on the specified port and save incoming files in the current directory.

### Send

```bash
java -cp bin Peer send <host> <port> <file-path>
```

This will connect to the receiver at `<host>:<port>` and send the specified file.

## Example

In one terminal, start the receiver:

```bash
java -cp bin Peer receive 5000
```

In another terminal, send a file:

```bash
java -cp bin Peer send 127.0.0.1 5000 /path/to/file.txt
```