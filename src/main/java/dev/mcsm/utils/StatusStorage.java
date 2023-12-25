package dev.mcsm.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusStorage {

    private String ip;
    private int port;
    private boolean online;
    private String motd;
    private Version type;
    private String versionNum;
    private String favicon;
    private PlayerCount playerCount;
    private double ping;

    public StatusStorage(String ip, int port, boolean online, String motd, String favicon, Version type, String versionNum, PlayerCount playerCount) {
        this.ip = ip;
        this.port = port;
        this.online = online;
        this.motd = motd;
        this.type = type;
        this.versionNum = versionNum;
        this.playerCount = playerCount;
        this.favicon = favicon;
    }

    public StatusStorage(String ip, int port, boolean online, Version version, String versionNum, PlayerCount playerCount) {
        this.ip = ip;
        this.port = port;
        this.online = online;
        this.type = version;
        this.versionNum = versionNum;
        this.playerCount = playerCount;
    }
    
}
