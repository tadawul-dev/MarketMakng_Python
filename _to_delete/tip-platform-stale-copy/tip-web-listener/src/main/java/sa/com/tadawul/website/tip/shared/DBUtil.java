package sa.com.tadawul.website.tip.shared;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Single place that opens Oracle connections. The same ~25 lines used to be copy-pasted into all
 * 14 {@code DBManager*} classes and {@code DBUtil}.
 * <p>
 * The JDBC 4 driver registers itself through {@link java.util.ServiceLoader}, so the deprecated
 * {@code Class.forName(JDBC_DRIVER).newInstance()} call is no longer needed. JDBC_DRIVER is
 * still read from efp.ini and is loaded only if it is not already registered (for example
 * when a non-default driver class is configured).
 */
public class DBUtil {

    private static final Logger logger = LogManager.getLogger(DBUtil.class);

    private Connection g_oConnection;

    /** Legacy instance API used by LoadInitialData. Returns null on failure, as before. */
    public Connection getConnection() {
        try {
            g_oConnection = openConnection();
        } catch (Exception e) {
            logger.error("Unable to open database connection", e);
        }
        return g_oConnection;
    }

    /**
     * Opens a new connection using the efp.ini settings.
     * USE_DATA_SOURCE=1 keeps the legacy OCI / IPC data source path.
     */
    public static Connection openConnection() throws SQLException {
        if (TipConstants.USE_DATA_SOURCE == 1) {
            OracleDataSource odc = new OracleDataSource();
            odc.setURL(TipConstants.DB_URL);
            odc.setUser(TipConstants.DB_USER);
            odc.setPassword(TipConstants.DB_PASS);
            odc.setDriverType("oci6");
            odc.setNetworkProtocol("ipc");
            return odc.getConnection();
        }
        ensureDriverLoaded();
        return DriverManager.getConnection(TipConstants.DB_URL, TipConstants.DB_USER, TipConstants.DB_PASS);
    }

    private static volatile boolean driverChecked;

    private static void ensureDriverLoaded() {
        if (driverChecked) {
            return;
        }
        String driver = TipConstants.JDBC_DRIVER;
        if (driver != null && !driver.isBlank()) {
            try {
                Class.forName(driver.trim());
            } catch (ClassNotFoundException e) {
                logger.error("JDBC_DRIVER class not found: " + driver + " - relying on JDBC service loader", e);
            }
        }
        driverChecked = true;
    }
}
