package tip.client;

import java.io.IOException;

/** A TIP text message that does not follow the tag/value syntax. */
public class TipParseException extends IOException {
    private static final long serialVersionUID = 1L;

    public TipParseException(String message) {
        super(message);
    }
}
