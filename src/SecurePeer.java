import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.Scanner;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class SecurePeer {
    
    private KeyPair keyPair;
    
    public SecurePeer() throws Exception {
        // Generate RSA key pair for this peer
        this.keyPair = SecureFileTransferProtocol.generateRSAKeyPair();
        System.out.println("RSA key pair generated for this peer.");
    }
    
    public static void main(String[] args) {
        try {
            SecurePeer peer = new SecurePeer();
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                printMenu();
                String choice = scanner.nextLine().trim();
                try {
                    if ("1".equals(choice)) {
                        System.out.print("Enter port to listen on: ");
                        int port = Integer.parseInt(scanner.nextLine().trim());
                        peer.receiveFile(port);
                    } else if ("2".equals(choice)) {
                        System.out.print("Enter host to connect to: ");
                        String host = scanner.nextLine().trim();
                        System.out.print("Enter port to connect to: ");
                        int port = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Enter file path to send: ");
                        String pathInput = scanner.nextLine().trim();
                        if (pathInput.startsWith("~")) {
                            pathInput = System.getProperty("user.home") + pathInput.substring(1);
                        }
                        Path file = Paths.get(pathInput);
                        peer.sendFile(host, port, file);
                    } else if ("0".equals(choice)) {
                        System.out.println("Exiting.");
                        break;
                    } else {
                        System.out.println("Invalid choice. Please try again.");
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            scanner.close();
        } catch (Exception e) {
            System.err.println("Failed to initialize secure peer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== SECURE P2P FILE TRANSFER ===");
        System.out.println("Select an option:");
        System.out.println(" 1) Receive a file (secure)");
        System.out.println(" 2) Send a file (secure)");
        System.out.println(" 0) Exit");
        System.out.print("Enter choice: ");
    }

    private void receiveFile(int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Waiting for secure connection on port " + port);
            
            try (Socket socket = serverSocket.accept();
                 DataInputStream dis = new DataInputStream(
                         new BufferedInputStream(socket.getInputStream()));
                 DataOutputStream dos = new DataOutputStream(
                         new BufferedOutputStream(socket.getOutputStream()))) {
                
                System.out.println("Connection established. Starting secure handshake...");
                
                // Step 1: Receive sender's public key
                int senderPubKeyLength = dis.readInt();
                byte[] senderPubKeyBytes = new byte[senderPubKeyLength];
                dis.readFully(senderPubKeyBytes);
                PublicKey senderPublicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(senderPubKeyBytes));
                
                // Step 2: Send our public key
                byte[] ourPubKeyBytes = keyPair.getPublic().getEncoded();
                dos.writeInt(ourPubKeyBytes.length);
                dos.write(ourPubKeyBytes);
                dos.flush();
                
                // Step 3: Receive and verify nonce
                int encryptedNonceLength = dis.readInt();
                byte[] encryptedNonce = new byte[encryptedNonceLength];
                dis.readFully(encryptedNonce);
                
                int signedNonceLength = dis.readInt();
                byte[] signedNonce = new byte[signedNonceLength];
                dis.readFully(signedNonce);
                
                byte[] decryptedNonce = SecureFileTransferProtocol.rsaDecrypt(encryptedNonce, keyPair.getPrivate());
                boolean isNonceValid = SecureFileTransferProtocol.verifySignature(decryptedNonce, signedNonce, senderPublicKey);
                
                if (!isNonceValid) {
                    System.err.println("Nonce verification failed! Connection terminated.");
                    return;
                }
                System.out.println("Nonce verified successfully.");
                
                // Step 4: Generate and send AES session key and IV
                SecretKey aesSessionKey = SecureFileTransferProtocol.generateAESKey();
                byte[] iv = SecureFileTransferProtocol.generateIV();
                
                byte[] encryptedKey = SecureFileTransferProtocol.rsaEncrypt(aesSessionKey.getEncoded(), senderPublicKey);
                byte[] encryptedIv = SecureFileTransferProtocol.rsaEncrypt(iv, senderPublicKey);
                byte[] signedKey = SecureFileTransferProtocol.signData(aesSessionKey.getEncoded(), keyPair.getPrivate());
                byte[] signedIv = SecureFileTransferProtocol.signData(iv, keyPair.getPrivate());
                
                dos.writeInt(encryptedKey.length);
                dos.write(encryptedKey);
                dos.writeInt(encryptedIv.length);
                dos.write(encryptedIv);
                dos.writeInt(signedKey.length);
                dos.write(signedKey);
                dos.writeInt(signedIv.length);
                dos.write(signedIv);
                dos.flush();
                
                System.out.println("AES session key and IV sent.");
                
                // Step 5: Receive encrypted file
                String fileName = dis.readUTF();
                long encryptedFileSize = dis.readLong();
                
                int fileHashLength = dis.readInt();
                byte[] fileHash = new byte[fileHashLength];
                dis.readFully(fileHash);
                
                int signedHashLength = dis.readInt();
                byte[] signedFileHash = new byte[signedHashLength];
                dis.readFully(signedFileHash);
                
                long timestamp = dis.readLong();
                int signedTimestampLength = dis.readInt();
                byte[] signedTimestamp = new byte[signedTimestampLength];
                dis.readFully(signedTimestamp);
                
                System.out.println("Receiving encrypted file: " + fileName + " (" + encryptedFileSize + " bytes encrypted)");
                
                // Read encrypted file data
                byte[] encryptedFileData = new byte[(int)encryptedFileSize];
                dis.readFully(encryptedFileData);
                
                // Verify timestamp signature
                byte[] timestampBytes = String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8);
                boolean isTimestampValid = SecureFileTransferProtocol.verifySignature(timestampBytes, signedTimestamp, senderPublicKey);
                if (!isTimestampValid) {
                    System.err.println("Timestamp signature verification failed!");
                    return;
                }
                
                // Verify file hash signature
                boolean isFileHashValid = SecureFileTransferProtocol.verifySignature(fileHash, signedFileHash, senderPublicKey);
                if (!isFileHashValid) {
                    System.err.println("File hash signature verification failed!");
                    return;
                }
                
                System.out.println("Signatures verified. Decrypting file...");
                
                // Decrypt file
                SecretKeySpec aesKeySpec = new SecretKeySpec(aesSessionKey.getEncoded(), "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                byte[] decryptedFileData = SecureFileTransferProtocol.aesDecrypt(encryptedFileData, aesKeySpec, ivSpec);
                
                // Verify file integrity
                byte[] calculatedHash = SecureFileTransferProtocol.calculateSHA256Hash(decryptedFileData);
                boolean hashesMatch = java.util.Arrays.equals(fileHash, calculatedHash);
                
                if (!hashesMatch) {
                    System.err.println("File integrity check failed!");
                    return;
                }
                
                // Save decrypted file
                File outFile = new File("secure_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(decryptedFileData);
                }
                
                System.out.println("File received and decrypted successfully!");
                System.out.println("Saved as: " + outFile.getName());
                System.out.println("File integrity verified: " + hashesMatch);
            }
        }
    }

    private void sendFile(String host, int port, Path file) throws Exception {
        File f = file.toFile();
        if (!f.exists() || !f.isFile()) {
            System.err.println("File does not exist or is not a file: " + file);
            return;
        }
        
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(
                     new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(
                     new BufferedInputStream(socket.getInputStream()));
             FileInputStream fis = new FileInputStream(f)) {
            
            System.out.println("Connected to " + host + ":" + port);
            System.out.println("Starting secure handshake...");
            
            // Step 1: Send our public key
            byte[] ourPubKeyBytes = keyPair.getPublic().getEncoded();
            dos.writeInt(ourPubKeyBytes.length);
            dos.write(ourPubKeyBytes);
            dos.flush();
            
            // Step 2: Receive receiver's public key
            int receiverPubKeyLength = dis.readInt();
            byte[] receiverPubKeyBytes = new byte[receiverPubKeyLength];
            dis.readFully(receiverPubKeyBytes);
            PublicKey receiverPublicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(receiverPubKeyBytes));
            
            // Step 3: Create and send signed nonce
            String nonce = SecureFileTransferProtocol.generateNonce();
            byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedNonce = SecureFileTransferProtocol.rsaEncrypt(nonceBytes, receiverPublicKey);
            byte[] signedNonce = SecureFileTransferProtocol.signData(nonceBytes, keyPair.getPrivate());
            
            dos.writeInt(encryptedNonce.length);
            dos.write(encryptedNonce);
            dos.writeInt(signedNonce.length);
            dos.write(signedNonce);
            dos.flush();
            
            System.out.println("Nonce sent.");
            
            // Step 4: Receive AES session key and IV
            int encryptedKeyLength = dis.readInt();
            byte[] encryptedKey = new byte[encryptedKeyLength];
            dis.readFully(encryptedKey);
            
            int encryptedIvLength = dis.readInt();
            byte[] encryptedIv = new byte[encryptedIvLength];
            dis.readFully(encryptedIv);
            
            int signedKeyLength = dis.readInt();
            byte[] signedKey = new byte[signedKeyLength];
            dis.readFully(signedKey);
            
            int signedIvLength = dis.readInt();
            byte[] signedIv = new byte[signedIvLength];
            dis.readFully(signedIv);
            
            // Decrypt and verify AES key and IV
            byte[] aesKeyBytes = SecureFileTransferProtocol.rsaDecrypt(encryptedKey, keyPair.getPrivate());
            byte[] ivBytes = SecureFileTransferProtocol.rsaDecrypt(encryptedIv, keyPair.getPrivate());
            
            boolean isKeyValid = SecureFileTransferProtocol.verifySignature(aesKeyBytes, signedKey, receiverPublicKey);
            boolean isIvValid = SecureFileTransferProtocol.verifySignature(ivBytes, signedIv, receiverPublicKey);
            
            if (!isKeyValid || !isIvValid) {
                System.err.println("AES key/IV verification failed!");
                return;
            }
            
            System.out.println("AES session key received and verified.");
            
            // Step 5: Prepare and encrypt file
            byte[] fileData = Files.readAllBytes(file);
            byte[] fileHash = SecureFileTransferProtocol.calculateSHA256Hash(fileData);
            
            SecretKeySpec aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            byte[] encryptedFileData = SecureFileTransferProtocol.aesEncrypt(fileData, aesKeySpec, ivSpec);
            
            // Sign file hash and timestamp
            byte[] signedFileHash = SecureFileTransferProtocol.signData(fileHash, keyPair.getPrivate());
            long timestamp = System.currentTimeMillis();
            byte[] timestampBytes = String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8);
            byte[] signedTimestamp = SecureFileTransferProtocol.signData(timestampBytes, keyPair.getPrivate());
            
            // Send encrypted file with metadata
            dos.writeUTF(f.getName());
            dos.writeLong(encryptedFileData.length);
            
            dos.writeInt(fileHash.length);
            dos.write(fileHash);
            dos.writeInt(signedFileHash.length);
            dos.write(signedFileHash);
            
            dos.writeLong(timestamp);
            dos.writeInt(signedTimestamp.length);
            dos.write(signedTimestamp);
            
            dos.write(encryptedFileData);
            dos.flush();
            
            System.out.println("Encrypted file sent successfully!");
            System.out.println("Original file: " + f.getName() + " (" + fileData.length + " bytes)");
            System.out.println("Encrypted size: " + encryptedFileData.length + " bytes");
            System.out.println("File hash: " + SecureFileTransferProtocol.bytesToHex(fileHash));
        }
    }
}
