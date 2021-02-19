package bg.sofia.uni.fmi.mjt.torrent.server;

import bg.sofia.uni.fmi.mjt.torrent.server.runnables.ClientThreadRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TorrentServer implements Runnable {
    public static final int SERVER_PORT = 8800;
    public static final String EMPTY_STRING = "";
    private final CommandHandler commandHandler = new CommandHandler();
    private static final int THREAD_LIMIT = 20;
    private ExecutorService executor;
    private boolean running = false;
    private ServerSocket serverSocket;


    public static void main(String[] args) {
        TorrentServer server = new TorrentServer();
        server.start();
    }

    public void start() {
        if (!running) {
            running = true;
            Thread server = new Thread(this);
            server.start();
        }
    }

    public void stop() throws IOException, InterruptedException {
        commandHandler.clear();

        running = false;
        if (serverSocket != null) serverSocket.close();
        serverSocket = null;
        Thread.sleep(200);

        if (executor != null) {
            executor.shutdownNow();
            while (!executor.isTerminated()) {
                executor.awaitTermination(10, TimeUnit.SECONDS);
                System.out.println("Could not shut down executor, trying again");
            }
        }
        executor = null;
        Thread.sleep(200);
    }


    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            executor = Executors.newFixedThreadPool(THREAD_LIMIT);

            Socket clientSocket;

            while (running) {
                clientSocket = serverSocket.accept();
                if (!running) {
                    break;
                }
                System.out.println("Server accepted connection request from client "
                        + clientSocket.getRemoteSocketAddress());

                ClientThreadRunnable clientThread = new ClientThreadRunnable(commandHandler, clientSocket);
                executor.execute(clientThread);
            }
        } catch (IOException e) {
            System.out.println("An error has occurred on the server: " + e.getMessage());
        } finally {
            try {
                if (executor != null) executor.shutdownNow();
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                System.out.println("Could not close server socket: " + e.getMessage());
                //e.printStackTrace();
            }
        }
    }

}
