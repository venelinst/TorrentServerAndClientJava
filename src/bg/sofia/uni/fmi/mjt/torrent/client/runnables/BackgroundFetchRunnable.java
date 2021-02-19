package bg.sofia.uni.fmi.mjt.torrent.client.runnables;

import bg.sofia.uni.fmi.mjt.torrent.client.ClientUserData;
import bg.sofia.uni.fmi.mjt.torrent.client.NetworkConstants;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BackgroundFetchRunnable implements Runnable {
    private static final String FETCH_COMMAND = "fetch";
    private static final String CACHE_FILE_LINE_TEMPLATE = "%s - %s:%s";
    private static final String FETCH_DATA_SEPARATOR = ":";
    private final String CLIENT_CACHE_FILE = "cache.txt";
    private static final int SLEEP_TIME = 5000;//30,000 ms = 30 s

    private final Map<String, ClientUserData> userMappings;

    public BackgroundFetchRunnable(Map<String, ClientUserData> userMappings) {
        this.userMappings = userMappings;
    }

    @Override
    public void run() {

        try (Socket socket = new Socket(NetworkConstants.SERVER_HOST, NetworkConstants.SERVER_PORT)) {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                writer.println(FETCH_COMMAND);
                FileOutputStream output = new FileOutputStream(CLIENT_CACHE_FILE, false);

                userMappings.clear();
                String line;
                while ((line = reader.readLine()) != null && !line.equals(NetworkConstants.EMPTY_STRING)) {
                    List<String> tokenized = Arrays.asList(line.split(FETCH_DATA_SEPARATOR));
                    if (tokenized.size() != 3) {
                        System.out.println("Could not fetch user data, server passed an invalid line to the fetch thread");
                        System.out.println("'" + line + "'");
                        break;
                    }
                    String username = tokenized.get(0);
                    String address = tokenized.get(1);
                    String port = tokenized.get(2);
                    userMappings.put(username, new ClientUserData(address, Integer.parseInt(port)));
                    String cacheFileLine = String.format(CACHE_FILE_LINE_TEMPLATE, username, address, port) + System.lineSeparator();
                    output.write(cacheFileLine.getBytes());
                }
                Thread.sleep(SLEEP_TIME);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
