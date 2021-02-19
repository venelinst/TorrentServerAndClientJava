package bg.sofia.uni.fmi.mjt.torrent.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandHandler {

    private static final String TOKENIZER_REGEX = "\\s+";
    private static final String UNKNOWN_COMMAND = "Unknown command";
    private static final String INVALID_PARAMETERS = "Invalid parameters";
    private static final String LIST_ROW_TEMPLATE = "%s : %s";
    public static final String FILE_LIST_SEPARATOR = ",";
    private static final String FILES_REGISTERED_SUCCESSFULLY = "The files have been registered successfully.";
    private static final String USERNAME_DOES_NOT_EXIST = "Username does not exist";
    private static final String FILES_UNREGISTERED_SUCCESSFULLY = "The files have been unregistered successfully";
    public static final String FETCH_TEMPLATE = "%s:%s:%s";
    public static final String NO_FILES_REGISTERED_YET = "No files have been registered yet";

    private final Map<String, UserData> userMapping = new ConcurrentHashMap<>();

    void clear() {
        userMapping.clear();
    }

    public String executeAndGetReply(String request, Socket identifier) {
        List<String> tokenized = Arrays.asList(request.split(TOKENIZER_REGEX));

        if (tokenized.size() < 1) {
            return UNKNOWN_COMMAND;
        }
        switch (tokenized.get(0)) {
            case "list-files" -> {
                if (tokenized.size() != 1) {
                    return INVALID_PARAMETERS;
                }
                return listFiles();
            }
            case "register" -> {
                if (tokenized.size() != 3) {
                    return INVALID_PARAMETERS;
                }
                return register(identifier, tokenized);
            }
            case "unregister" -> {
                if (tokenized.size() != 3) {
                    return INVALID_PARAMETERS;
                }
                return unregister(tokenized);
            }
            case "fetch" -> {
                if (tokenized.size() != 1) {
                    return INVALID_PARAMETERS;
                }
                fetch(identifier);
                return TorrentServer.EMPTY_STRING;
            }
        }
        return UNKNOWN_COMMAND;
    }

    private void fetch(Socket key) {
        try {
            PrintWriter socketWriter = new PrintWriter(key.getOutputStream(), true);
            userMapping.forEach((user, data) -> {
                socketWriter.println(String.format(FETCH_TEMPLATE, user, data.address(), data.port()));
            });
            socketWriter.println(TorrentServer.EMPTY_STRING);
        } catch (IOException e) {
            System.out.println("Error fetching userdata to client");
            System.out.println(e.getMessage());
        }
    }

    private String unregister(List<String> tokenized) {
        String username = tokenized.get(1);
        List<String> files = Arrays.asList(tokenized.get(2).split(FILE_LIST_SEPARATOR));//file separator


        if (!userMapping.containsKey(username)) {
            return USERNAME_DOES_NOT_EXIST;

        } else {
            files.stream().forEach(file -> {
                userMapping.get(username).files().remove(file);
            });
            if (userMapping.get(username).files().isEmpty()) {
                userMapping.remove(username);
            }
        }
        return FILES_UNREGISTERED_SUCCESSFULLY;
    }

    private String listFiles() {
        if (userMapping.isEmpty()) {
            return NO_FILES_REGISTERED_YET;
        }
        final String[] result = new String[1];
        result[0] = "";

        userMapping.keySet().forEach(username -> {
            UserData userData = userMapping.get(username);
            userData.files().forEach(file -> {
                result[0] = result[0] + String.format(LIST_ROW_TEMPLATE, file, username) + System.lineSeparator();
            });
        });
        return result[0].strip();
    }

    private String register(Socket identifier, List<String> tokenized) {
        String username = tokenized.get(1);
        List<String> files = Arrays.asList(tokenized.get(2).split(FILE_LIST_SEPARATOR));


        if (!userMapping.containsKey(username)) {

            InetSocketAddress sockaddr = (InetSocketAddress) identifier.getRemoteSocketAddress();

            String portString = getPortFromAdress(sockaddr);
            String addressString = getAddress(sockaddr);

            UserData data = new UserData(
                    addressString,
                    portString,
                    new ArrayList<>(files)
            );
            userMapping.put(username, data);

        } else {
            files.stream().forEach(file -> {
                if (!userMapping.get(username).files().contains(file)) {
                    userMapping.get(username).files().add(file);
                }
            });
        }
        return FILES_REGISTERED_SUCCESSFULLY;
    }

    private String getAddress(InetSocketAddress sockaddr) {
        InetAddress inaddr = sockaddr.getAddress();
        Inet4Address in4addr = (Inet4Address) inaddr;
        return in4addr.toString().substring(1);
    }

    private String getPortFromAdress(InetSocketAddress sockaddr) {
        String portString = sockaddr.toString().substring(
                sockaddr.toString().indexOf(":") + 1
        );
        portString = Integer.toString(Integer.valueOf(portString) + 1);
        return portString;
    }
}
