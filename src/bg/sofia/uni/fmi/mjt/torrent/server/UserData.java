package bg.sofia.uni.fmi.mjt.torrent.server;

import java.util.List;

public record UserData(String address, String port, List<String> files) {
}
