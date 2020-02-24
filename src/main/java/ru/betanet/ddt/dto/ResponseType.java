package ru.betanet.ddt.dto;

public enum ResponseType {
    OK_CRC("Full response. Interrupted by CRC match"),
    OK_EOS("Full response. Interrupted by end of data stream"),
    SOCKET_TIMEOUT("Possible partial response. Socket timeout"),
    ERROR_MAXLEN("Possible partial response. Interrupted by response max length catcher");

    private String description;

    public String getDescription() {
        return this.description;
    }

    ResponseType(String description) {
        this.description = description;
    }
}
