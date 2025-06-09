package shared;

public enum Protocol {
    TCP,
    UDP,
    RTP_UDP;

    public static Protocol getDefaultForResolution(String resolution) {
        return switch (resolution) {
            case "240p" -> TCP;
            case "360p", "480p" -> UDP;
            case "720p", "1080p" -> RTP_UDP;
            default -> TCP;
        };
    }
}
