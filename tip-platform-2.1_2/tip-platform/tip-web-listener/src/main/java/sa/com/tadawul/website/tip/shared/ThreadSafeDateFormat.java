package sa.com.tadawul.website.tip.shared;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Thread-confined wrapper around {@link SimpleDateFormat}.
 * <p>
 * The legacy code shared one static {@code SimpleDateFormat} across threads, which can silently
 * return wrong dates under concurrency. A {@code java.time.DateTimeFormatter} would be the textbook
 * replacement, but it parses strictly, while {@code SimpleDateFormat} is lenient (e.g. it rolls
 * "20250132" over to 1 Feb). To keep exactly the same results for every feed value we keep
 * {@code SimpleDateFormat} and give each thread its own instance.
 */
public final class ThreadSafeDateFormat {

    private final ThreadLocal<SimpleDateFormat> format;

    public ThreadSafeDateFormat(String pattern) {
        this.format = ThreadLocal.withInitial(() -> new SimpleDateFormat(pattern));
    }

    public Date parse(String source) throws ParseException {
        return format.get().parse(source);
    }

    public String format(Date date) {
        return format.get().format(date);
    }
}
