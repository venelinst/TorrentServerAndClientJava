package bg.sofia.uni.fmi.mjt.torrent.server;

import bg.sofia.uni.fmi.mjt.torrent.client.NetworkConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandHandlerTest {
    private static final String UNKNOWN_COMMAND = "Unknown command";
    private static final String INVALID_PARAMETERS = "Invalid parameters";
    private static final String LIST_ROW_TEMPLATE = "%s : %s";
    public static final String FETCH_TEMPLATE = "%s:%s:%s";
    public static final String NO_FILES_REGISTERED_YET = "No files have been registered yet";

    private CommandHandler commandHandler = new CommandHandler();
    @Mock
    private Socket socket;

    @Before
    public void setup() {
        socket = mock(Socket.class);
        commandHandler.clear();
    }


    @Test
    public void registerAndListTest() {
        String request = "register user file";
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        when(socket.getRemoteSocketAddress()).thenReturn(address);
        commandHandler.executeAndGetReply(request, socket);
        //file registered

        String fileList = commandHandler.executeAndGetReply("list-files", socket);
        String expected = String.format(LIST_ROW_TEMPLATE, "file", "user");
        assertEquals(expected, fileList);
    }

    @Test
    public void registerOnAlreadyExistingUserAndListTest() {
        String request = "register user file";
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        when(socket.getRemoteSocketAddress()).thenReturn(address);
        commandHandler.executeAndGetReply(request, socket);
        //file registered
        request = "register user file2";
        commandHandler.executeAndGetReply(request, socket);

        String fileList = commandHandler.executeAndGetReply("list-files", socket);

        String expected = String.format(LIST_ROW_TEMPLATE, "file", "user") + System.lineSeparator()
                + String.format(LIST_ROW_TEMPLATE, "file2", "user");

        assertEquals(expected, fileList);
    }

    @Test
    public void invalidCommandsTest() {
        String response = commandHandler.executeAndGetReply("register user", socket);
        String expected = INVALID_PARAMETERS;
        assertEquals(expected, response);

        response = commandHandler.executeAndGetReply("unregister user", socket);
        expected = INVALID_PARAMETERS;
        assertEquals(expected, response);

        response = commandHandler.executeAndGetReply("upload file user", socket);
        expected = UNKNOWN_COMMAND;
        assertEquals(expected, response);

    }

    @Test
    public void emptyListTest() {
        String request = "list-files";
        String response = commandHandler.executeAndGetReply(request, socket);
        assertEquals(NO_FILES_REGISTERED_YET, response);
    }

    @Test
    public void registerUnregisterAndListTest() {
        String request = "register user file";
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        when(socket.getRemoteSocketAddress()).thenReturn(address);
        commandHandler.executeAndGetReply(request, socket);
        //file registered

        String fileList = commandHandler.executeAndGetReply("list-files", socket);
        String expected = String.format(LIST_ROW_TEMPLATE, "file", "user");
        assertEquals(expected, fileList);

        request = "unregister user file";
        commandHandler.executeAndGetReply(request, socket);

        fileList = commandHandler.executeAndGetReply("list-files", socket);
        expected = NO_FILES_REGISTERED_YET;
        assertEquals(expected, fileList);
    }

    @Test
    public void registerAndFetchTest() throws IOException {
        String request = "register user1 file";
        int user1Port = 8200;//register port = mini server port = client port + 1
        int user2Port = 8300;
        String user1Addr = "127.0.0.1";
        String user2Addr = "127.0.0.2";

        InetSocketAddress address = new InetSocketAddress(user1Addr, user1Port);
        when(socket.getRemoteSocketAddress()).thenReturn(address);
        commandHandler.executeAndGetReply(request, socket);

        request = "register user2 file";
        address = new InetSocketAddress(user2Addr, user2Port);
        when(socket.getRemoteSocketAddress()).thenReturn(address);
        commandHandler.executeAndGetReply(request, socket);

        //file registered
        PipedInputStream pipeInput = new PipedInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(pipeInput));
        BufferedOutputStream out = new BufferedOutputStream(new PipedOutputStream(pipeInput));
        when(socket.getOutputStream()).thenReturn(out);

        request = "fetch";
        commandHandler.executeAndGetReply(request, socket);

        String fetchResult = "";
        String fetchResultBuffer;
        while ((fetchResultBuffer = reader.readLine()) != null && !fetchResultBuffer.equals(NetworkConstants.EMPTY_STRING)) {
            fetchResult = fetchResult + fetchResultBuffer + System.lineSeparator();
        }

        String expected = String.format(FETCH_TEMPLATE, "user1", user1Addr, user1Port + 1) + System.lineSeparator() +
                String.format(FETCH_TEMPLATE, "user2", user2Addr, user2Port + 1) + System.lineSeparator();

        assertEquals(expected, fetchResult);
    }

}
