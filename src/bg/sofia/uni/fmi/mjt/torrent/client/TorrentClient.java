package bg.sofia.uni.fmi.mjt.torrent.client;

import bg.sofia.uni.fmi.mjt.torrent.client.runnables.BackgroundFetchRunnable;
import bg.sofia.uni.fmi.mjt.torrent.client.runnables.MiniServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TorrentClient {
    public static final String EXIT_COMMAND = "exit";
    public static final String TOKENIZER_REGEX = "\\s+";
    public static final String DOWNLOAD_COMMAND = "download";
    public static final String INVALID_PARAMETERS = "Invalid parameters";
    public static final String DOWNLOAD_DIR_NAME = "torrentDownloads";
    public static final String SEEDING_DIR_NAME = "torrentSeeding";
    public static final String DOWNLOAD_DIR_PATH = DOWNLOAD_DIR_NAME + File.separator;
    public static final String SEEDING_DIR_PATH = SEEDING_DIR_NAME + File.separator;

    private final Map<String, ClientUserData> userDataMap = new HashMap<>();
    private final DownloadCommandHandler downloadCommandHandler = new DownloadCommandHandler(userDataMap);

    public static void main(String[] args) {
        TorrentClient client = new TorrentClient();
        client.start();
    }

    public void start() {
        createDirectories();
        try (SocketChannel socketChannel = SocketChannel.open();
             BufferedReader socketReader = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
             PrintWriter socketWriter = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true);
             Scanner scanner = new Scanner(System.in)) {

            socketChannel.connect(new InetSocketAddress(NetworkConstants.SERVER_HOST, NetworkConstants.SERVER_PORT));

            String port = getPort(socketChannel);
            startThreads(port, socketReader);


            while (true) {
                String command = scanner.nextLine();
                if (command == null) continue;
                if (EXIT_COMMAND.equals(command)) {
                    return;
                }
                if (command.startsWith(DOWNLOAD_COMMAND)) {
                    downloadCommandHandler.handleDownloadCommand(command);
                    continue;
                }
                socketWriter.println(command);
            }
        } catch (IOException e) {
            System.out.println("There is a problem with the network communication");
            e.getMessage();
        }
    }


    private String getPort(SocketChannel socketChannel) throws IOException {
        InetSocketAddress clientSocketAddr = (InetSocketAddress) socketChannel.getLocalAddress();
        System.out.println(clientSocketAddr.toString());
        String port = clientSocketAddr.toString().substring(
                clientSocketAddr.toString().indexOf(":") + 1
        );
        return port;
    }


    private void createDirectories() {
        makedir(DOWNLOAD_DIR_NAME);
        makedir(SEEDING_DIR_NAME);

    }

    private void makedir(String dir) {
        File dirfile = new File(dir);
        if (dirfile.exists()) {
            return;
        }
        dirfile.mkdir();
    }
    private void startThreads(String port, BufferedReader socketReader){
        MiniServer miniServer = new MiniServer(Integer.parseInt(port) + 1);
        Thread miniServerThread = new Thread(miniServer);
        miniServerThread.setDaemon(true);
        miniServerThread.start();

        BackgroundFetchRunnable backgroundFetcher = new BackgroundFetchRunnable(userDataMap);
        Thread backgroundFetchThread = new Thread(backgroundFetcher);
        backgroundFetchThread.setDaemon(true);
        backgroundFetchThread.start();

        Thread outputThread = new Thread(() -> {
            String reply;
            try {
                while ((reply = socketReader.readLine()) != null) {
                    System.out.println(reply);
                }
            } catch (ClosedByInterruptException ignore) {
            } catch (IOException e) {
                System.out.println("Error trying to print to the console");
                System.out.println(e.getMessage());
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();
    }
}
