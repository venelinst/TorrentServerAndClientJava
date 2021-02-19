package bg.sofia.uni.fmi.mjt.torrent.client;

import bg.sofia.uni.fmi.mjt.torrent.client.runnables.MiniServer;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TorrentClientTest {

    @Test
    public void fileDownloadAndUploadTest() throws IOException {
        String host = "localhost";
        int port = 8888;
        String filename = "testFile.txt";
        String outputFilename = "testFile.txt";

        Thread mst = new Thread(new MiniServer(port));
        mst.setDaemon(true);
        mst.start();

        Map<String, ClientUserData> map = new HashMap<>();
        map.put("user", new ClientUserData(host, port));
        DownloadCommandHandler downloadCommandHandler = new DownloadCommandHandler(map);

        String exampleContent =
                """
                        random file content
                        line 2
                        javajavajava
                        line4
                        """;
        createTempFileInSeedingPath(exampleContent, filename);

        File outputTestfile = new File(TorrentClient.DOWNLOAD_DIR_PATH + outputFilename);
        if (outputTestfile.exists()) outputTestfile.delete();
        outputTestfile.deleteOnExit();

        String downloadCommand = "download user " + filename + " " + outputFilename;
        downloadCommandHandler.handleDownloadCommand(downloadCommand);

        byte[] f = Files.readAllBytes(outputTestfile.toPath());
        String downloadedFileContent = new String(f);

        assertEquals(exampleContent, downloadedFileContent);

    }

    public void createTempFileInSeedingPath(String content, String filename) throws IOException {
        File testfile = new File(TorrentClient.SEEDING_DIR_PATH + filename);
        if (testfile.exists()) testfile.delete();
        testfile.createNewFile();
        testfile.deleteOnExit();

        try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(testfile))) {
            fileOutputStream.write(content.getBytes());
            fileOutputStream.flush();
        }
    }
}
