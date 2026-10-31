package tip.xm;

/** Error codes of the XM layer (same numeric values as the C {@code XM_ErrorCode}). */
public enum XmError {
    NO_ERROR(0, "No error"),
    NO_MEMORY(-1, "Out of memory"),
    BAD_PARAM(-2, "Bad parameter"),
    BAD_LENGTH(-3, "Bad length"),
    NO_ROOM(-4, "No room for operation"),
    NO_SERVICE_ENTRIES(-5, "No service entries"),
    NOT_SUPPORTED(-6, "Operation not supported"),
    NETWORK_ERROR(-8, "Network error"),
    TIMEOUT(-9, "Operation timed out"),
    BUFFER_TOO_SMALL(-10, "Buffer is too small"),
    INTERNAL_ERROR(-11, "Internal error"),
    NOT_INITIALIZED(-12, "XT not initialized"),
    NO_MORE_ENTRIES(-13, "Message is out of extension headers"),
    DC_END_OF_FILE(-14, "Decompression - end of file"),
    DC_BAD_SIZE(-15, "Decompression - bad size"),
    DC_BAD_BIT_STREAM(-16, "Decompression - bad bit stream"),
    DC_BAD_OPCODE(-17, "Decompression - bad op code"),
    DC_BAD_HEADER_TYPE(-18, "Decompression - bad header type"),
    DC_BAD_PHRASE_LENGTH(-19, "Decompression - bad phrase length"),
    DC_BAD_PHRASE_OFFSET(-20, "Decompression - bad phrase offset"),
    BAD_ALGORITHM(-21, "Encryption - unknown algorithm"),
    BAD_KEY_LENGTH(-22, "Encryption - unacceptable key length"),
    WEAK_KEY(-23, "Encryption - key too weak");

    private final int code;
    private final String message;

    XmError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }

    public String message() { return message; }
}
