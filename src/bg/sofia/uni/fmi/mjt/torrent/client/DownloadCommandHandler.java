package bg.sofia.uni.fmi.mjt.torrent.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DownloadCommandHandler {
    private final Map<String, ClientUserData> userDataMap;

    protected DownloadCommandHandler(Map<String, ClientUserData> userDataMap) {
        this.userDataMap = userDataMap;
    }


    public void handleDownloadCommand(String command) {
        List<String> tokenized = Arrays.asList(command.split(TorrentClient.TOKENIZER_REGEX));
        if (tokenized.size() != 4) {
            System.out.println(TorrentClient.INVALID_PARAMETERS);
            return;
        }
        String username = tokenized.get(1);
        String file = tokenized.get(2);
        String downloadLocation = tokenized.get(3);

        if (!userDataMap.containsKey(username)) {
            System.out.println(String.format("No peer information for user %s", username));
            return;
        }
        ClientUserData clientUserData = userDataMap.get(username);
        File downloadLocationFile = new File(TorrentClient.DOWNLOAD_DIR_PATH + downloadLocation);

        if (downloadLocationFile.exists()) {
            System.out.println("Cannot download file at this location, there is already a file with that name there");
            return;
        }

        handleDownload(file, downloadLocation, clientUserData, downloadLocationFile);

    }

    private void handleDownload(String file, String downloadLocation, ClientUserData clientUserData, File downloadLocationFile) {
        try (Socket socket = new Socket(clientUserData.address(), clientUserData.port());
             BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(downloadLocationFile), 1024);
             BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), 1024);
             PrintStream socketOutputStream = new PrintStream(socket.getOutputStream())) {

            socketOutputStream.println(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            System.out.printf("Starting download of %s%n", downloadLocation);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead = totalBytesRead + bytesRead;
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            if (totalBytesRead > 0) {
                System.out.println(String.format("Successfully downloaded file %s", downloadLocation));
            } else {
                System.out.println("Could not download any data, deleting file");
                downloadLocationFile.delete();
            }

        } catch (IOException e) {
            System.out.println("An error has occurred while downloading the file, the seeder's miniserver is probably not turned on");
            e.getMessage();
            downloadLocationFile.delete();
        }
    }
}
