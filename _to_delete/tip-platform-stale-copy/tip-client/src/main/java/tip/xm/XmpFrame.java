package tip.xm;

import java.util.Objects;

/** One XMP frame: {@code type | extLen | bodyLen(2) | extension header | body}. */
public record XmpFrame(int type, ExtHeader ext, byte[] body) {
    private static final byte[] NO_BODY = new byte[0];

    public XmpFrame {
        Objects.requireNonNull(ext, "ext");
        body = body == null ? NO_BODY : body;
    }

    public XmpFrame(int type, byte[] body) {
        this(type, ExtHeader.EMPTY, body);
    }

    public int encodedLength() {
        return Xmp.HEADER_SIZE + ext.encodedLength() + body.length;
    }

    /** Port of {@code XMP_CreateMessage}. */
    public byte[] encode() throws XmException {
        if (type < 0 || type > 0xFF) throw new XmException(XmError.BAD_PARAM, "frame type " + type);
        if (body.length == 0 && type != Xmp.Type.NONE) throw new XmException(XmError.BAD_LENGTH, "empty body");
        if (body.length > 0xFFFF) throw new XmException(XmError.BAD_PARAM, "body too long");
        byte[] extBytes = ext.encode();
        byte[] out = new byte[Xmp.HEADER_SIZE + extBytes.length + body.length];
        out[0] = (byte) type;
        out[1] = (byte) extBytes.length;
        out[2] = (byte) (body.length >>> 8);
        out[3] = (byte) body.length;
        System.arraycopy(extBytes, 0, out, Xmp.HEADER_SIZE, extBytes.length);
        System.arraycopy(body, 0, out, Xmp.HEADER_SIZE + extBytes.length, body.length);
        return out;
    }

    /** Total frame length from a 4-byte header at {@code off}. */
    public static int frameLength(byte[] b, int off) {
        return Xmp.HEADER_SIZE + (b[off + 1] & 0xFF) + (((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF));
    }

    /** Decodes the frame at {@code off}; bytes after the frame (e.g. cipher padding) are ignored. */
    public static XmpFrame decode(byte[] b, int off, int available) throws XmException {
        if (available < Xmp.HEADER_SIZE) throw new XmException(XmError.BAD_LENGTH, "truncated frame header");
        int total = frameLength(b, off);
        if (total > available) throw new XmException(XmError.BAD_LENGTH, "truncated frame");
        int extLen = b[off + 1] & 0xFF;
        int bodyLen = total - Xmp.HEADER_SIZE - extLen;
        byte[] body = new byte[bodyLen];
        System.arraycopy(b, off + Xmp.HEADER_SIZE + extLen, body, 0, bodyLen);
        return new XmpFrame(b[off] & 0xFF, ExtHeader.decode(b, off + Xmp.HEADER_SIZE, extLen), body);
    }
}
