package bg.sofia.uni.fmi.mjt.torrent.client.runnables;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiniServer implements Runnable {
    private static final int THREAD_LIMIT = 5;
    private final ExecutorService executor;
    private final int PORT;
    private boolean running = true;

    public MiniServer(int port) {
        PORT = port;
        executor = Executors.newFixedThreadPool(THREAD_LIMIT);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(String.format("MiniServer started on port %d and listening for connect requests", PORT));
            Socket clientSocket;

            while (running) {
                try {
                    clientSocket = serverSocket.accept();

                    FileTransferRunnable transferThread = new FileTransferRunnable(clientSocket);
                    executor.execute(transferThread);
                } catch (IOException e) {
                    System.out.println("Something went wrong with the miniserver");
                    //e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("Could not create miniserver");
        }
    }
}
