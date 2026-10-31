package tip.xm;

/** Constants of the XMP framing protocol (port of {@code xmapi.h}). */
public final class Xmp {
    private Xmp() {}

    /** type(1) + extension header length(1) + body length(2, big endian). */
    public static final int HEADER_SIZE = 4;
    /** No frame (after decompression/decryption) may be larger than this. */
    public static final int MAX_MESSAGE_LENGTH = 3072;
    public static final int MAX_EXT_HDR_SIZE = 255;
    public static final int MAX_CRYPT_KEY_SIZE = 16;

    /** Frame types (first header byte). */
    public static final class Type {
        private Type() {}
        public static final int NONE = 0x00;
        public static final int XTP_2 = 0x04;
        /** The TIP text protocol travels in this frame type. */
        public static final int BINARY_RECORD = 0x05;
        public static final int BINARY_TAGGED = 0x06;
        public static final int KEY_EXCHANGE = 0x07;
        public static final int ENCRYPTED = 0x81;
        public static final int COMPRESSED = 0x82;
    }

    /** Extension header tags. */
    public static final class Tag {
        private Tag() {}
        public static final int NONE = 0;
        public static final int CLIENT_TXN_ID = 1;
        public static final int SERVICE_TIME = 2;
        public static final int KEEP_ALIVE = 3;
        public static final int TIMESTAMP = 4;
        public static final int KEY_ID = 5;
        public static final int QUEUE_ALERT = 6;
        public static final int COMPRESSION_ALGORITHM = 7;
        public static final int ENCRYPTION_ALGORITHM = 8;
        public static final int SESSION_STATE = 9;
        public static final int PROTOCOL_VERSION = 0xa;
    }

    /** Compression algorithm codes (extension header {@link Tag#COMPRESSION_ALGORITHM}). */
    public static final class Compression {
        private Compression() {}
        public static final int NONE = 0;
        public static final int LZ1 = 1;
    }

    /** Session state reported by the server (extension header {@link Tag#SESSION_STATE}). */
    public enum SessionState {
        NONE, OFFLINE, CATCHUP, ONLINE, UNKNOWN;

        public static SessionState of(int code) {
            return switch (code) {
                case 0 -> NONE;
                case 1 -> OFFLINE;
                case 2 -> CATCHUP;
                case 3 -> ONLINE;
                default -> UNKNOWN;
            };
        }
    }
}
