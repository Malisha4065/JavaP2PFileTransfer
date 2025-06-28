import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;

public class Peer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                if ("1".equals(choice)) {
                    System.out.print("Enter port to listen on: ");
                    int port = Integer.parseInt(scanner.nextLine().trim());
                    receiveFile(port);
                } else if ("2".equals(choice)) {
                    System.out.print("Enter host to connect to: ");
                    String host = scanner.nextLine().trim();
                    System.out.print("Enter port to connect to: ");
                    int port = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Enter file path to send: ");
                    Path file = Paths.get(scanner.nextLine().trim());
                    sendFile(host, port, file);
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
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("Select an option:");
        System.out.println(" 1) Receive a file");
        System.out.println(" 2) Send a file");
        System.out.println(" 0) Exit");
        System.out.print("Enter choice: ");
    }

    private static void receiveFile(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
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