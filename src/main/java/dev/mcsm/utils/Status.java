package dev.mcsm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class Status {

    public static String removeColorCodes(String input) {
        return input.replaceAll("\u00A7[0-9a-fA-Fk-oK-OrR]", "");
    }

    static ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    public static StatusStorage javaStatus(String ip, int port) {
        URL url = new URL("http://dal-01.springracks.com:2009/api/v1/java?ip=" + ip + "&port=" + port);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println(content);

        JsonNode jsonNode = mapper.readTree(content.toString());

        String description = jsonNode.get("description").asText();
        String favicon = jsonNode.get("favicon").asText();
        JsonNode playersNode = jsonNode.get("players");
        int playersOnline = playersNode.get("online").asInt();
        int maxPlayers = playersNode.get("max").asInt();
        PlayerCount playerCount = new PlayerCount(playersOnline, maxPlayers);
        String version = jsonNode.path("version").get("name").asText();

        return new StatusStorage(ip, port, true, description, favicon, Version.JAVA, version, playerCount);
    }

    public static StatusStorage bedrockStatus(String ip, int port) {
        try {
            URL url = new URL("http://dal-01.springracks.com:2009/api/v1/bedrock?ip=" + ip + "&port=" + port);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            JsonNode jsonNode = mapper.readTree(content.toString());

            JsonNode playersNode = jsonNode.get("players");
            int playersOnline = playersNode.get("online").asInt();
            int maxPlayers = playersNode.get("max").asInt();
            PlayerCount playerCount = new PlayerCount(playersOnline, maxPlayers);
            JsonNode versionNode = jsonNode.get("version");
            String version = versionNode.get("name").asText();
            return new StatusStorage(ip, port, true, Version.BEDROCK, version, playerCount);
        } catch (Exception e) {
            return new StatusStorage(ip, port, false, Version.BEDROCK, "Error", new PlayerCount(0, 0));
        }
    }

}
