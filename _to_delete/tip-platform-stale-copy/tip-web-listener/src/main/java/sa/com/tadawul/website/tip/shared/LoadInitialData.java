package sa.com.tadawul.website.tip.shared;

import java.util.Arrays;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.vo.TIP_GetHighLowPriceVO;
import sa.com.tadawul.website.tip.vo.TIP_WEBMappingVO;

public class LoadInitialData {
	
	private static final Logger logger = LogManager.getLogger(LoadInitialData.class);
	
	private static final float LOADFACTOR=0.75f;
	
	 public static final Map<String,String> hmShareSymbols = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_SHARES/LOADFACTOR +1) );
     public static final Map<String,String> hmBondSymbols = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_BONDS/LOADFACTOR +1) );
     public static final Map<String,String> hmOTCBondSymbols = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_BONDS/LOADFACTOR +1) );
     //Added for SME
     public static final Map<String,String> hmSMEShareSymbols = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_SME_SHARES/LOADFACTOR +1) );
     public static final Map<String,String> hmRISMESymbols = new SymbolMap<>(20);
     //Added for ETF
     public static final Map<String,String> hmETFSymbols = new SymbolMap<>(20);
     public static final Map<String,String> hmRISymbols = new SymbolMap<>(20);
     public static final Map<String,TIP_GetHighLowPriceVO> hmBondHighLowPriceDtMap = new SymbolMap<>(120);
     
     //Added for SME
     public static final Map<String,TIP_WEBMappingVO> hmMainMarketIndex = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_INDICES/LOADFACTOR +1) );
     public static final Map<String,TIP_WEBMappingVO> hmSMEMarketIndex = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_SME_INDICES/LOADFACTOR +1) );
     public static final Map<String,TIP_WEBMappingVO> hmBondMarketIndex = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_BOND_INDICES/LOADFACTOR +1) );
     
     public static final Map<String,String> hmMainMarketList = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_INDICES/LOADFACTOR +1) );
     public static final Map<String,String> hmSMEMarketList = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_SME_INDICES/LOADFACTOR +1) );              
     public static final Map<String,String> hmBondMarketList = new SymbolMap<>( (int) Math.ceil (TipConstants.TIP_MAX_NO_BOND_INDICES/LOADFACTOR +1) );
          
     public static final Map<String,String> hmListToIndexMap = new SymbolMap<>(30);
     
     private Connection g_oConnection;
	
     static String strLastUpdDateForShareIndex;
     static String strLastUpdDateForSMEShareIndex;
     static String strLastUpdDateForBond;
     public static String dtShareIndexLastUpdateTime;
     public static String dtSMEShareIndexLastUpdateTime;
     public static String dtBondLastUpdateTime;
     public static String dtFeedReqDate;
     
	public void loadHashMap()
	{
		
		 logger.debug("inside LoadInitialData loadHashMap function");
         String strLoadTeSymbolsQuery;
         //Added for SME
         String strLoadSMESymbolsQuery;
         String strLoadLastUpdDateForSMEShareQuery;
         String strLoadRISMESymbolsQuery;
         String strLoadBondSymbolsQuery;
         //Added for ETF
         String strLoadETFSymbolsQuery;
         String strLoadRISymbolsQuery;
         String strLoadBondHighLowPriceDtQuery;
         String strLoadLastUpdDateForShareQuery;
         String strLoadLastUpdDateForBondQuery;
         String strLoadIndexQuery;
         
         try
         {
        	 DBUtil dbutil = new DBUtil();
        	 g_oConnection = dbutil.getConnection();
        	   strLoadTeSymbolsQuery = "SELECT PK_TE_SYMBOL, SHORT_DESC FROM EFP.TE_SYMBOL";
               Statement stTeSymbol = g_oConnection.createStatement();
               ResultSet rsTeSymbol = stTeSymbol.executeQuery(strLoadTeSymbolsQuery);
               String strTeSymbol;
               String strTeSymDescription;
               
               while(rsTeSymbol.next()) {
            	   strTeSymbol = rsTeSymbol.getString(1);
                   strTeSymDescription = rsTeSymbol.getString(2);
                   hmShareSymbols.put(strTeSymbol.trim(), strTeSymDescription);
            	}
               rsTeSymbol.close();
               stTeSymbol.close();
               
               
               strLoadBondSymbolsQuery = "SELECT PK_FI_SYMBOL, SHORT_DESC FROM EFP.FI_SYMBOL";
               Statement stBondSymbol = g_oConnection.createStatement();
               ResultSet rsBondSymbol = stBondSymbol.executeQuery(strLoadBondSymbolsQuery);
               String strBondSymbol;
               String strBondSymDescription;
               
               while(rsBondSymbol.next()) {
            	   strBondSymbol = rsBondSymbol.getString(1);
            	   strBondSymDescription = rsBondSymbol.getString(2);
            	   hmBondSymbols.put(strBondSymbol.trim(), strBondSymDescription);
            	}
               
               //Added for OTC Trades unlisted
               strLoadBondSymbolsQuery = "SELECT PK_FI_SYMBOL, SHORT_DESC FROM EFP.FI_SYMBOL_UNLISTED";
             
                rsBondSymbol = stBondSymbol.executeQuery(strLoadBondSymbolsQuery);
               
               
               while(rsBondSymbol.next()) {
            	   strBondSymbol = rsBondSymbol.getString(1);
            	   strBondSymDescription = rsBondSymbol.getString(2);
            	   hmOTCBondSymbols.put(strBondSymbol.trim(), strBondSymDescription);
            	}
               
               rsBondSymbol.close();
               stBondSymbol.close();
               
               strLoadETFSymbolsQuery = "SELECT PK_ETF_COMPANY, DESCRIPTION_EN FROM WEB.ETF_COMPANY";
               Statement stETFSymbol = g_oConnection.createStatement();
               ResultSet rsETFSymbol = stETFSymbol.executeQuery(strLoadETFSymbolsQuery);
               String strETFSymbol;
               String strETFSymDescription;
               
               while(rsETFSymbol.next()) {
            	   strETFSymbol = rsETFSymbol.getString(1);
            	   strETFSymDescription = rsETFSymbol.getString(2);
            	   hmETFSymbols.put(strETFSymbol.trim(), strETFSymDescription);
            	}               
               rsETFSymbol.close();
               stETFSymbol.close();
               
               strLoadRISymbolsQuery = "SELECT PK_RF_COMPANY, DESCRIPTION_EN FROM WEB.RI_COMPANY";
               Statement stRISymbol = g_oConnection.createStatement();
               ResultSet rsRISymbol = stRISymbol.executeQuery(strLoadRISymbolsQuery);
               String strRISymbol;
               String strRISymDescription;
               while(rsRISymbol.next()) {
            	   strRISymbol = rsRISymbol.getString(1);
                   strRISymDescription = rsRISymbol.getString(2);
                   hmRISymbols.put(strRISymbol.trim(),  strRISymDescription);
            	}   
               rsRISymbol.close();
               stRISymbol.close();
               
               
               //Added for SME
               strLoadSMESymbolsQuery = "SELECT PK_TE_SYMBOL, SHORT_DESC FROM EFP.TE_SME_SYMBOL";
               Statement stSMESymbol = g_oConnection.createStatement();
               ResultSet rsSMESymbol = stSMESymbol.executeQuery(strLoadSMESymbolsQuery);
               String strSMESymbol;
               String strSMESymDescription;
               while(rsSMESymbol.next()) {
            	 	strSMESymbol = rsSMESymbol.getString(1);
            	 	 strSMESymDescription = rsSMESymbol.getString(2);
            	 	hmSMEShareSymbols.put(strSMESymbol.trim(),strSMESymDescription);
               }
              
               if(null !=rsSMESymbol) rsSMESymbol.close();
               if(null !=stSMESymbol) stSMESymbol.close();
              
               
             //Added for RI SME
               //To load the initial HashMap to distinguish from Normal Symbol
               strLoadRISMESymbolsQuery = "SELECT PK_RF_COMPANY, DESCRIPTION_EN FROM WEB.RI_SME_COMPANY";
               Statement stRISMESymbol = g_oConnection.createStatement();
               ResultSet rsRISMESymbol = stRISMESymbol.executeQuery(strLoadRISMESymbolsQuery);
               String strRISMESymbol;
               
               
               while(rsRISMESymbol.next()) {
            	   strRISMESymbol = rsRISMESymbol.getString(1);               
                   hmRISMESymbols.put(strRISMESymbol.trim(),strRISMESymbol);
              }
               
               rsRISMESymbol.close();
               stRISMESymbol.close();
               
               
               //ToLoad the Main Market List
               strLoadIndexQuery = "SELECT MARKET_CODE,WEB_SECTOR_ID,BDLI_ID,BDLI_NAME,BDIN_ID,BDIN_NAME,INDEX_CATEGORY FROM WEB.TIP_WEB_SECTOR_MAPPING ";
               Statement stIndex = g_oConnection.createStatement();
               ResultSet rsIndex = stIndex.executeQuery(strLoadIndexQuery);
               String indexCategory="";
               TIP_WEBMappingVO tip_webMapping =null;
               while(rsIndex.next()) {               
               				
               				tip_webMapping = new TIP_WEBMappingVO();
               				tip_webMapping.setMarketId(rsIndex.getString("MARKET_CODE"));
               				tip_webMapping.setWebsite_SectorID(rsIndex.getInt("WEB_SECTOR_ID"));
               				tip_webMapping.setBDLi(rsIndex.getString("BDLI_NAME"));
               				tip_webMapping.setBDLiId(rsIndex.getString("BDLI_ID"));
               				tip_webMapping.setBDIn(rsIndex.getString("BDIN_NAME"));
               				tip_webMapping.setBDInId(rsIndex.getString("BDIN_ID"));
               				indexCategory = rsIndex.getString("INDEX_CATEGORY");
               				hmListToIndexMap.put(tip_webMapping.getBDLiId(), tip_webMapping.getBDInId());
               				switch(indexCategory)
               				{
               				case "M":
               					hmMainMarketIndex.put(tip_webMapping.getBDInId(),tip_webMapping);
               					break;
               				case "F":
               					hmBondMarketIndex.put(tip_webMapping.getBDInId(),tip_webMapping);
               					break;
               				case "S":
               					hmSMEMarketIndex.put(tip_webMapping.getBDInId(),tip_webMapping);
               					break;
               				}
               }
               rsIndex.close();
               if(null!= stIndex) stIndex.close();
               
               
               strLoadBondHighLowPriceDtQuery = "SELECT HS.FK_FI_SYMBOL AS FK_FI_SYMBOL, MAX(HS.HIGH_52_WEEKS_PRICE) AS HIGH_52_WEEKS_PRICE, " +
                       "MAX(HS.HIGH_52_WEEKS_DATE) AS HIGH_52_WEEKS_DATE, MIN(HS.LOW_52_WEEKS_PRICE) AS LOW_52_WEEKS_PRICE, " +
                       "MAX(HS.LOW_52_WEEKS_DATE)  AS LOW_52_WEEKS_DATE FROM WEB.HS_FI_STOCK HS, (SELECT HS1.FK_FI_SYMBOL," +
                       "MAX(HIGH_52_WEEKS_PRICE) AS HIGH_52_WEEKS_PRICE, MIN(LOW_52_WEEKS_PRICE) AS LOW_52_WEEKS_PRICE FROM WEB.HS_FI_STOCK HS1 " +
                       "WHERE cast(HS1.TRANSACTION_DATE as date) >=(add_months(sysdate,-12)) and LOW_52_WEEKS_PRICE != 0 AND HIGH_52_WEEKS_PRICE != 0 " +
                       "group by HS1.FK_FI_SYMBOL) HS1 WHERE HS.FK_FI_SYMBOL = HS1.FK_FI_SYMBOL AND HS.HIGH_52_WEEKS_PRICE = HS1.HIGH_52_WEEKS_PRICE " +
                       "AND HS.LOW_52_WEEKS_PRICE = HS1.LOW_52_WEEKS_PRICE GROUP BY HS.FK_FI_SYMBOL";
                                               Statement stBondHighLowPriceDt = g_oConnection.createStatement();
                                               ResultSet rsBondHighLowPriceDt = stBondHighLowPriceDt.executeQuery(strLoadBondHighLowPriceDtQuery);
                                               String strBondSymbolKey;
                                               while(rsBondHighLowPriceDt.next()) {                                                 
                                                               TIP_GetHighLowPriceVO bondHighLowPriceDtVO = new TIP_GetHighLowPriceVO();
                                                               strBondSymbolKey = rsBondHighLowPriceDt.getString("FK_FI_SYMBOL");
                                                               bondHighLowPriceDtVO.high_52_weeks_price = rsBondHighLowPriceDt.getDouble("HIGH_52_WEEKS_PRICE");
                                                               bondHighLowPriceDtVO.high_52_weeks_date = rsBondHighLowPriceDt.getDate("HIGH_52_WEEKS_DATE");
                                                               bondHighLowPriceDtVO.low_52_weeks_price = rsBondHighLowPriceDt.getDouble("LOW_52_WEEKS_PRICE");
                                                               bondHighLowPriceDtVO.low_52_weeks_date = rsBondHighLowPriceDt.getDate("LOW_52_WEEKS_DATE");
                                                               hmBondHighLowPriceDtMap.put(strBondSymbolKey.trim(), bondHighLowPriceDtVO);
                                               }
                                               rsBondHighLowPriceDt.close();
                                               stBondHighLowPriceDt.close();
               
               
                                            // Getting LastTradedDate for Share and Index
                                               strLoadLastUpdDateForShareQuery = "SELECT to_char(LASTTRADEDDATE,'yyyy-mm-dd') AS LASTTRADEDDATE FROM EFP.TE_MARKET WHERE PK_EXCHANGE_CODE = '"+TipConstants.SHARE_MARKET_CODE+"'";
                                               Statement stLastUpdDateForShare = g_oConnection.createStatement();
                                               ResultSet rsLastUpdDateForShare = stLastUpdDateForShare.executeQuery(strLoadLastUpdDateForShareQuery);
                                               if(rsLastUpdDateForShare != null && rsLastUpdDateForShare.next())
                                                               strLastUpdDateForShareIndex = rsLastUpdDateForShare.getString("LASTTRADEDDATE").split("\\s+")[0];
                                              
                                               if (logger.isDebugEnabled()) logger.debug("TipConstants strLastUpdDateForShareIndex = {}", TipConstants.DB_FEED_REQ_DATE);
                                               if(strLastUpdDateForShareIndex != null && !strLastUpdDateForShareIndex.equals("")){
                                                               if (logger.isDebugEnabled()) logger.debug("if strLastUpdDateForShareIndex = {}", strLastUpdDateForShareIndex);
                                                               dtShareIndexLastUpdateTime = strLastUpdDateForShareIndex.trim();
                                                               if (logger.isDebugEnabled()) logger.debug("dtShareLastUpdateTime = {}", dtShareIndexLastUpdateTime);
                                               } else {
                                                               strLastUpdDateForShareIndex = TipConstants.DB_FEED_REQ_DATE;
                                                               if (logger.isDebugEnabled()) logger.debug("else strLastUpdDateForShareIndex = {}", strLastUpdDateForShareIndex);
                                                               dtShareIndexLastUpdateTime = strLastUpdDateForShareIndex;
                                                               if (logger.isDebugEnabled()) logger.debug("dtShareLastUpdateTime = {}", dtShareIndexLastUpdateTime);
                                               }
                                               rsLastUpdDateForShare.close();
                                               stLastUpdDateForShare.close();
                                               
                                               // Getting LastTradedDate for Bond
                                               strLoadLastUpdDateForBondQuery = "SELECT to_char(LASTTRADEDDATE,'yyyy-mm-dd') AS LASTTRADEDDATE FROM EFP.TE_MARKET WHERE PK_EXCHANGE_CODE = '"+TipConstants.BOND_MARKET_CODE+"'";
                                               Statement stLastUpdDateForBond = g_oConnection.createStatement();
                                               ResultSet rsLastUpdDateForBond = stLastUpdDateForBond.executeQuery(strLoadLastUpdDateForBondQuery);
                                               if(rsLastUpdDateForBond != null && rsLastUpdDateForBond.next())
                                                               strLastUpdDateForBond = rsLastUpdDateForBond.getString("LASTTRADEDDATE");
                                              
                                               if(strLastUpdDateForBond != null && !strLastUpdDateForBond.equals("")){
                                                               if (logger.isDebugEnabled()) logger.debug("strLastUpdDateForBond = {}", strLastUpdDateForBond);
                                                               dtBondLastUpdateTime = strLastUpdDateForBond.trim();
                                                               if (logger.isDebugEnabled()) logger.debug("dtBondLastUpdateTime = {}", dtBondLastUpdateTime);
                                               } else {
                                                               strLastUpdDateForBond = TipConstants.DB_FEED_REQ_DATE;
                                                               if (logger.isDebugEnabled()) logger.debug("strLastUpdDateForBond = {}", strLastUpdDateForBond);
                                                               dtBondLastUpdateTime = strLastUpdDateForBond;
                                                               if (logger.isDebugEnabled()) logger.debug("dtBondLastUpdateTime = {}", dtBondLastUpdateTime);
                                               }
                                               rsLastUpdDateForBond.close();
                                               stLastUpdDateForBond.close();
                                              
                                               // Getting LastTradedDate for SME
                                               strLoadLastUpdDateForSMEShareQuery = "SELECT to_char(LASTTRADEDDATE,'yyyy-mm-dd') AS LASTTRADEDDATE FROM EFP.TE_MARKET WHERE PK_EXCHANGE_CODE = '"+TipConstants.SME_MARKET_CODE+"'";
                                               Statement stLastUpdDateForSMEShare = g_oConnection.createStatement();
                                               ResultSet rsLastUpdDateForSMEShare = stLastUpdDateForSMEShare.executeQuery(strLoadLastUpdDateForSMEShareQuery);
                                               if(rsLastUpdDateForSMEShare != null && rsLastUpdDateForSMEShare.next())
                                                               strLastUpdDateForSMEShareIndex = rsLastUpdDateForSMEShare.getString("LASTTRADEDDATE").split("\\s+")[0];
                                              
                                               if (logger.isDebugEnabled()) logger.debug("TipConstants strLastUpdDateForSMEShareIndex = {}", TipConstants.DB_FEED_REQ_DATE);
                                               if(strLastUpdDateForSMEShareIndex != null && !strLastUpdDateForSMEShareIndex.equals("")){
                                                               if (logger.isDebugEnabled()) logger.debug("if strLastUpdDateForSMEShareIndex = {}", strLastUpdDateForSMEShareIndex);
                                                               dtSMEShareIndexLastUpdateTime = strLastUpdDateForSMEShareIndex.trim();
                                                               if (logger.isDebugEnabled()) logger.debug("dtSMEShareLastUpdateTime = {}", dtSMEShareIndexLastUpdateTime);
                                               } else {
                                                               strLastUpdDateForSMEShareIndex = TipConstants.DB_FEED_REQ_DATE;
                                                               if (logger.isDebugEnabled()) logger.debug("else strLastUpdDateForSMEShareIndex = {}", strLastUpdDateForSMEShareIndex);
                                                               dtSMEShareIndexLastUpdateTime = strLastUpdDateForSMEShareIndex;
                                                               if (logger.isDebugEnabled()) logger.debug("dtSMEShareLastUpdateTime = {}", dtSMEShareIndexLastUpdateTime);
                                               }
                                               rsLastUpdDateForSMEShare.close();
                                               if(null!=stLastUpdDateForSMEShare) stLastUpdDateForSMEShare.close();
                                               
                                               if(null != TipConstants.SHARED_LIST_MAPPING && !"".equals(TipConstants.SHARED_LIST_MAPPING))
                                               {
                                            	   Arrays.stream(TipConstants.SHARED_LIST_MAPPING.split(","))
                                            	         .map(entry -> entry.split(":"))
                                            	         .forEach(mappingValues -> {
                                            	             TIPDataObjects.SharedList_Mapping_TLU.put(mappingValues[0], mappingValues[1]);
                                            	             logger.info("TIPDataObjects.SharedList_Mapping_TLU mappingValues[0]= {}   mappingValues[1] :{}", mappingValues[0], mappingValues[1]);
                                            	         });
                                            	  
                                               }
                                               
                                             /*  TIPDataObjects.SharedList_Mapping_TLU.put("TASI", "TASC");
                                               TIPDataObjects.SharedList_Mapping_TLU.put("TENI", "TENC");
                                               TIPDataObjects.SharedList_Mapping_TLU.put("NOMU", "NOMC");*/
                                               
                                               if (!g_oConnection.getAutoCommit()) g_oConnection.commit();
                                               logger.debug("Exit LoadInitialData loadHashMap function");
         }catch(Exception e)
         {
        	 e.printStackTrace();
         }
         
         
		
		
	}

}
