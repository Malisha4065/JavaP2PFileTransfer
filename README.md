# Java P2P File Transfer

This projeWhen launched, you'll see a menu:

**Basic Version:**
```
Select an option:
 1) Receive a file
 2) Send a file
 0) Exit
Enter choice:
```

**Secure Version:**
```
=== SECURE P2P FILE TRANSFER ===
Select an option:
 1) Receive a file (secure)
 2) Send a file (secure)
 0) Exit
Enter choice:
```

**Receive** mode (choice `1`): you'll be prompted for the listening port; the incoming file will be saved in the current directory. In secure mode, files are prefixed with "secure_" and are encrypted during transfer.

**Send** mode (choice `2`): you'll be prompted for the target host, port, and file path; the chosen file will be sent to the receiver.  
Paths beginning with `~` are automatically expanded to your home directory.

Choose `0` to exit the application.

## Security Features (SecurePeer)

The secure version implements a comprehensive cryptographic protocol:

1. **RSA Key Exchange**: Each peer generates a 2048-bit RSA key pair for authentication and key exchange
2. **Nonce Challenge**: Sender creates and signs a nonce for identity verification
3. **AES Session Key**: Receiver generates a 128-bit AES session key and IV for file encryption
4. **Digital Signatures**: All critical data (nonces, keys, file hashes, timestamps) are signed with RSA
5. **File Integrity**: SHA-256 hashing ensures file integrity during transfer
6. **Timestamp Protection**: Prevents replay attacks with signed timestamps
7. **End-to-End Encryption**: Files are encrypted with AES-128-CBC before transmission

### Security Protocol Flow:
1. Peer authentication via RSA public key exchange
2. Nonce challenge for identity verification
3. Secure AES session key establishment
4. File encryption and digital signing
5. Integrity verification upon receipt

All cryptographic operations use industry-standard algorithms (RSA-2048, AES-128, SHA-256) ensuring robust security for file transfers. a simple peer-to-peer file transfer application and a secure version with end-to-end encryption.

## Prerequisites

- Java 8 or higher

## Compile

### Basic Version
```bash
javac -d bin src/Peer.java
```

### Secure Version
```bash
javac -d bin src/SecurePeer.java src/SecureFileTransferProtocol.java
```

## Usage

### Basic P2P Transfer
After compilation, simply run the peer without arguments; it will prompt for actions:

```bash
java -cp bin Peer
```

### Secure P2P Transfer
For encrypted file transfers with digital signatures:

```bash
java -cp bin SecurePeer
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