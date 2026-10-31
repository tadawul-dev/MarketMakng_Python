package sa.com.tadawul.website.tip.shared;

import java.sql.Connection;
import java.sql.DriverManager;

import oracle.jdbc.pool.OracleDataSource;

public class DBUtil {

	  private Connection g_oConnection;
	  boolean g_bClassLoaded;
	  
	public  Connection getConnection()
	{
		try
		{
					if (TipConstants.USE_DATA_SOURCE == 1) {
			            OracleDataSource odc =  new OracleDataSource();
			            odc.setURL(TipConstants.DB_URL);
			            odc.setUser(TipConstants.DB_USER);
			            odc.setPassword(TipConstants.DB_PASS);
			            odc.setDriverType("oci6");
			            odc.setNetworkProtocol("ipc");
			            g_oConnection = odc.getConnection();
			           
			            // DB2 DATASOURCE CODE
			            /*db2ds2 = new DB2DataSource();
			            db2ds2.setDatabaseName(TipConstants.DB_URL);
			            db2ds2.setServerName(TipConstants.DB_SERVERNAME);
			            db2ds2.setPortNumber(TipConstants.DB_PORT);
			            db2ds2.setUser(TipConstants.DB_USER);
			            db2ds2.setPassword(TipConstants.DB_PASS);                                                  
			            g_oConnection = db2ds2.getConnection();
			            g_oConnection.setAutoCommit(autoCommitFlag);*/
			            g_bClassLoaded = true;
				} else{
			            // DB2 JDBC CONNECTION
			            Class.forName(TipConstants.JDBC_DRIVER).newInstance();
			            g_oConnection = DriverManager.getConnection(TipConstants.DB_URL, TipConstants.DB_USER, TipConstants.DB_PASS);
			            /*g_oConnection.setTransactionIsolation(g_oConnection.TRANSACTION_READ_COMMITTED);
			            g_oConnection.setAutoCommit(autoCommitFlag);*/
			            g_bClassLoaded = true;
				}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return g_oConnection;
	}
	
	
}
