package tip.xm;

import java.io.IOException;

/** Any failure in the XM layer: framing, compression, encryption, network. */
public class XmException extends IOException {
    private static final long serialVersionUID = 1L;

    private final XmError error;

    public XmException(XmError error) {
        super(error.message());
        this.error = error;
    }

    public XmException(XmError error, String detail) {
        super(error.message() + ": " + detail);
        this.error = error;
    }

    public XmException(XmError error, String detail, Throwable cause) {
        super(error.message() + ": " + detail, cause);
        this.error = error;
    }

    public XmError error() { return error; }
}
