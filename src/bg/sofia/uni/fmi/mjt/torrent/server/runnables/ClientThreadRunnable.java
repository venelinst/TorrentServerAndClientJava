package bg.sofia.uni.fmi.mjt.torrent.server.runnables;


import bg.sofia.uni.fmi.mjt.torrent.server.CommandHandler;
import bg.sofia.uni.fmi.mjt.torrent.server.TorrentServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientThreadRunnable implements Runnable {
    private final CommandHandler commandHandler;
    private final Socket socket;
    private static final String EXIT_COMMAND = "exit";

    public ClientThreadRunnable(CommandHandler commandHandler, Socket socket) {
        this.commandHandler = commandHandler;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                if (Thread.interrupted()) {
                    break;
                }
                String command = reader.readLine();
                if (command == null) continue;
                if (EXIT_COMMAND.equals(command)) {
                    socket.close();
                    return;
                }

                String response = commandHandler.executeAndGetReply(command, socket);
                if (response.equals(TorrentServer.EMPTY_STRING)) continue;
                writer.println(response);
            }
        } catch (IOException e) {
            System.out.println("Client thread error, probably disconnected");
            System.out.println(e.getMessage());
            //e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket");
                System.out.println(e.getMessage());
            }
        }
    }
}
