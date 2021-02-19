package bg.sofia.uni.fmi.mjt.torrent.server;

import bg.sofia.uni.fmi.mjt.torrent.client.NetworkConstants;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

public class TorrentServerTest {
    private static final String UNKNOWN_COMMAND = "Unknown command";
    private static final String LIST_ROW_TEMPLATE = "%s : %s";
    private static final String FILES_REGISTERED_SUCCESSFULLY = "The files have been registered successfully.";
    private static TorrentServer server;


    @BeforeClass
    public static void initServer() {
        server = new TorrentServer();
    }

    @Before
    public void setup() throws InterruptedException, IOException {
        server.stop();
        Thread.sleep(500);
        server.start();
        Thread.sleep(200);
    }


    @Test
    public void registerAndListTest() throws IOException {
        try (Socket clientSocket = new Socket(NetworkConstants.SERVER_HOST, NetworkConstants.SERVER_PORT);
             PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String request = "register user file";
            clientWriter.println(request);
            String response = clientReader.readLine();

            assertEquals(FILES_REGISTERED_SUCCESSFULLY, response);

            clientWriter.println("list-files");
            response = clientReader.readLine();
            String expected = String.format(LIST_ROW_TEMPLATE, "file", "user");

            assertEquals(expected, response);
        }
    }

    @Test
    public void invalidCommandTest() throws IOException {
        try (Socket clientSocket = new Socket(NetworkConstants.SERVER_HOST, NetworkConstants.SERVER_PORT);
             PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String request = "upload";
            clientWriter.println(request);
            String response = clientReader.readLine();

            assertEquals(UNKNOWN_COMMAND, response);
        }


    }

}
