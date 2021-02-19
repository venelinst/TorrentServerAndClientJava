package bg.sofia.uni.fmi.mjt.torrent.client.runnables;

import bg.sofia.uni.fmi.mjt.torrent.client.TorrentClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class FileTransferRunnable implements Runnable {
    private final Socket socket;

    public FileTransferRunnable(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedInputStream fileInputStream = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), 1024)) {

            String filename = in.readLine();
            String fileLocation = TorrentClient.SEEDING_DIR_PATH + filename;

            fileInputStream = new BufferedInputStream(new FileInputStream(fileLocation), 1024);

            System.out.println(String.format("Starting upload of file %s to client %s", fileLocation, socket.getRemoteSocketAddress()));

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            System.out.println(String.format("Successfully uploaded file %s", fileLocation));

            fileInputStream.close();

        } catch (FileNotFoundException e) {
            System.out.println("Attempted download of inexistent file");
            e.getMessage();
        } catch (IOException e) {
            e.getMessage();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    System.out.println("Error closing file input stream after uploading");
                    e.printStackTrace();
                }
            }
        }

    }
}
