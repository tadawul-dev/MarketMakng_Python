package tip.client;

/** Message types and field tags of the TIP text protocol (port of {@code tiputils.h}). */
public final class TipTags {
    private TipTags() {}

    public static final char DELIMITER = ';';
    public static final char END_OF_MESSAGE = '\n';
    public static final char ESCAPE = '\\';

    // message types
    public static final String LOGON = "LGn";
    public static final String LOGOFF = "LGf";
    public static final String END_OF_STREAM = "EOs";
    public static final String GENERAL_REPLY = "GEn";
    public static final String HEARTBEAT = "HBt";
    public static final String REALTIME = "RTr";
    public static final String BASIC_DATA = "BDa";

    // field tags
    public static final String USER_ID = "UId";
    public static final String PASSWORD = "PWd";
    public static final String PROTOCOL_VERSION = "VEr";
    public static final String REPLY_CODE = "Rc";
    public static final String REPLY_TEXT = "Rx";
    public static final String SEQUENCE_NUMBER = "n";
    public static final String REQUEST_DATE = "QDAy";
    public static final String SCOPE = "QSCo";
    public static final String START_SEQ_NO = "Sn";
    public static final String END_SEQ_NO = "En";
    public static final String REQUEST_TYPE = "Qt";
}
