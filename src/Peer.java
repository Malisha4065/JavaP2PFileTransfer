import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Peer {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }
        String mode = args[0];
        try {
            if ("receive".equalsIgnoreCase(mode)) {
                if (args.length != 2) {
                    printUsage();
                    return;
                }
                int port = Integer.parseInt(args[1]);
                receiveFile(port);
            } else if ("send".equalsIgnoreCase(mode)) {
                if (args.length != 4) {
                    printUsage();
                    return;
                }
                String host = args[1];
                int port = Integer.parseInt(args[2]);
                Path file = Paths.get(args[3]);
                sendFile(host, port, file);
            } else {
                printUsage();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  To receive: java Peer receive <port>");
        System.out.println("  To send:    java Peer send <host> <port> <file-path>");
    }

    private static void receiveFile(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Waiting for incoming connection on port " + port);
        try (Socket socket = serverSocket.accept();
             DataInputStream dis = new DataInputStream(
                     new BufferedInputStream(socket.getInputStream()))) {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            System.out.println("Receiving file: " + fileName + " (" + fileSize + " bytes)");
            File outFile = new File(fileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                long remaining = fileSize;
                int read;
                while (remaining > 0 && (read = dis.read(buffer, 0,
                        (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, read);
                    remaining -= read;
                }
            }
            System.out.println("File received successfully.");
        }
    }

    private static void sendFile(String host, int port, Path file) throws IOException {
        File f = file.toFile();
        if (!f.exists() || !f.isFile()) {
            System.err.println("File does not exist or is not a file: " + file);
            return;
        }
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(
                     new BufferedOutputStream(socket.getOutputStream()));
             FileInputStream fis = new FileInputStream(f)) {
            System.out.println("Connected to " + host + ":" + port);
            dos.writeUTF(f.getName());
            dos.writeLong(f.length());
            dos.flush();
            System.out.println("Sending file: " + f.getName() + " (" + f.length() + " bytes)");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            dos.flush();
            System.out.println("File sent successfully.");
        }
    }
}