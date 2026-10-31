/** 
 * #---------------------------------------------------------------------------------------------
 * #   This source is part of the Tadawul Web Listener, Saudi Stock Exchange
 * #   Copyright (c) 2009 Saudi Stock Exchange
 * #
 * #
 * #   All rights reserved.  No part of this source or any copyrights subsisting or arising out of these materials may be reproduced, distributed, amended, modified, adapted, stored in a
 * #   retrieval system, adopted or transmitted in any form or by any means,
 * #   electronic, mechanical, photographic, graphic, optic recording or otherwise,
 * #   translated in any language or computer language, without the prior written
 * #   permission of Saudi Stock Exchange
 * #
 * # ---------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------
 * FileName : DataParserForLowVolm.java
 *
 * Description : 
 *
 * Created Date : 23 April 2018
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 23 April 2018            Mohanram                  Initial Version
 *----------------------------------------------------------------------------------------------
*/

package sa.com.tadawul.website.tip.dataanalyser;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

import sa.com.tadawul.website.tip.DBThreads.DBQueueUpdater;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import sa.com.tadawul.website.tip.messages.BasicDataBond;
import sa.com.tadawul.website.tip.messages.BasicDataIndex;
import sa.com.tadawul.website.tip.messages.BasicDataIndexMember;
import sa.com.tadawul.website.tip.messages.BasicDataList;
import sa.com.tadawul.website.tip.messages.BasicDataRight;
import sa.com.tadawul.website.tip.messages.BasicDataShare;
import sa.com.tadawul.website.tip.messages.GeneralReply;
import sa.com.tadawul.website.tip.messages.IndexSummary;
import sa.com.tadawul.website.tip.messages.LowVolmMessageType;
import sa.com.tadawul.website.tip.messages.MarketState;
import sa.com.tadawul.website.tip.messages.OrderBookReferencePrice;
import sa.com.tadawul.website.tip.messages.TadawulObjectCategory;
import sa.com.tadawul.website.tip.messages.TradeHalt;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.DBManagerBond;
import sa.com.tadawul.website.tip.shared.DBManagerBondIndex;
import sa.com.tadawul.website.tip.shared.DBManagerDataAnalyser;
import sa.com.tadawul.website.tip.shared.DBManagerIndex;
import sa.com.tadawul.website.tip.shared.DBManagerSMEIndex;
import sa.com.tadawul.website.tip.shared.DBManagerSMEShare;
import sa.com.tadawul.website.tip.shared.DBManagerShare;
import sa.com.tadawul.website.tip.shared.LoadInitialData;
import sa.com.tadawul.website.tip.shared.TIPCommonFunctions;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BondIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;
import sa.com.tadawul.website.tip.vo.TIP_GetHighLowPriceVO;
import sa.com.tadawul.website.tip.vo.TIP_IndexVO;
import sa.com.tadawul.website.tip.vo.TIP_MarketVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEIndexVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;

public class DataParserLowVolm  implements Runnable{
	
	private static final Logger logger = LogManager.getLogger(DataParserLowVolm.class);
		
	private String message = "";
	String messageValue[];
	private String value [] = new String[2];
	private String messageColumnType = "";
	//This to be removed
	int noOfBDBo;
	int noOfBDLi;
	int noOfBDIn;
	int noOfINs;	
	int noOfBDSh;
	int noOfBDRi;
	int noOfOBRf;
	int noOfTRh;
	int noOfEOs;
	int noOfGEns;
	int noOfHBts;
	int noOfSTas;
	int noOfBDIms;
	
	int totalLowVolumeMessages;
	
	 boolean setPrevious_close_price;
	// boolean isFirstOBsInEvening=true;
		
	  TIP_GetHighLowPriceVO bondHighLowPriceDtVO;
	  
		boolean finished = false;		
		int tmpid=0, tmpMarketId=0,tmpIterId=0,tmpIndexIter=0, tmpRpu=0,tmpRpl=0,tmpUc=0, tmpIdOrderBook=0 , tmpIdSecurityGroup=0, tmpGroupSourceId=0;
		long sequence_no=0;
		String tmpSym=null, tmpNam=null, tmpSymbolId=null,tmpNam_locale=null,tmpIsin=null,tmpoutStandingAmount=null,tmpIssueCurrency=null,tmpNominalValue=null,tmpCouponFrequency=null,tmpDaycountMethod=null,tmpCouponRate=null,tmpMaturityDate=null, tmpRateCalcType=null , tmpListingDate = null, tmpIssueDate=null, tmpSettlementDate =null, tmpIssueAmount = null , updateCode=null;
		long tmpDateLong=0;
		String tmpCAv=null, tmpVi=null, tmpVh=null, tmpVl=null, tmpDd=null,tmpDDn=null	, tmpRPu=null,tmpRPl=null, tmpRPr=null;
		
		String tmpOrderBookSourceId=null;
		String tmpOSt=null, tmpPOf=null, tmpMCa=null, tmpPl=null, tmpLTv=null,tmpPh=null	, tmpLOp=null,tmpACp=null,tmpPf=null, tmpHPw=null,tmpLPw=null;
		
		DBManagerDataAnalyser dbUtilObj  =null;
	  
	public DataParserLowVolm(){		
		try {
			 dbUtilObj = new DBManagerDataAnalyser("DBManagerDataAnalyser");
		} catch (Exception e1) {						
			logger.error("Error Occured in DataParserLowVolm" +e1.getLocalizedMessage());
		}
	
	}

	@Override
	public void run() {
		
		logger.info("DataParserForLowVolm Thread started. " );
		
		while(true)
				{
					
							if(MessageQueue.lowVlmMsgQueue != null) {
								while (MessageQueue.lowVlmMsgQueue.size() > 0) {									
									  try {
										  	message =MessageQueue.lowVlmMsgQueue.pop();									
										  	if(message != null && !message.equals("")) {
										  		messageValue = message.split(TipConstants.SPLITMESSAGE);
										  		if(messageValue != null && messageValue.length > 1) {
										  			if(messageValue[0] != null && messageValue[1] != null && !messageValue[1].equals("") && !messageValue[0].equals("")) {
										  				totalLowVolumeMessages++;
										  				TIPDataObjects.totalMessagesParsed.incrementAndGet();
										  					switch(LowVolmMessageType.getMessageValue(messageValue[0])){
											  					
										  						case 1://BDBo
										  							noOfBDBo++;
										  							parseBasicDataBond(messageValue);
										  						break;
											  					
										  						case 2://BDIn
										  							noOfBDIn++;
										  							parseBasicDataIndex(messageValue);
										  						break;
											  					
										  						case 3://BDLi
										  							noOfBDLi++;
										  							parseBasicDataLists(messageValue);
										  						break;
											  					
										  						case 4://BDRi
										  							noOfBDRi++;
										  							parseBasicDataEquityRights(messageValue);
										  						break;
										  						
										  						case 5://BDSh
										  							noOfBDSh++;
										  							parseBasicDataShare(messageValue);
										  						break;
										  						
										  						case 6://INs
										  							noOfINs++;
										  							parseIndexSummary(messageValue);
										  						break;
										  						
										  						case 7://OBRf
										  							noOfOBRf++;
										  							parseOrderBookReference(messageValue);
										  						break;
										  						
										  						/*case 8://OBs
										  							noOfOBs++;
										  							parseOrderBookSummary(messageValue);
										  						break;*/
										  						
										  						case 9://TRh
										  							noOfTRh++;
										  							parseTradeHalts(messageValue);
										  						break;
										  						
										  						case 10://EOs
										  							noOfEOs++;
										  							parseEndOfSessions(messageValue);
										  						break;
										  		
										  						case 11://GEN
										  							noOfGEns++;
										  							parseGeneralReply(messageValue);
										  						break;
										  						case 12://HBt
										  							noOfHBts++;
										  							//Nothing to do continue
										  						break;
										  						
										  						case 13://STa
										  							noOfSTas++;
										  							parseMarketStates(messageValue);
										  						break;
										  						
										  						case 14://BDIm
										  							noOfBDIms++;
										  							parseBasicDataIndexMember(messageValue,noOfBDIms);
										  						break;
										  						
										  						default:
										  							//logger.info("Default Messages : {}", message);
										  					
										  					}//End of Switch
									
										  			}
										  		}
										  	}
								
									  }//End of Try
									  catch(Exception e)
									  {
										  e.printStackTrace();
									  }
									  
									  //logger.info("Total DataParserForLowVolm Messages received :{}", totalLowVolumeMessages);
									//logger.info("DataParserForLowVolm  Statistics : Total  No of LowVolumeMessages : {} No of BDSh : {} No of BDLi : {} No of BDIn : {} No of INs : {} No of BDRi : {} No of BDBo : {} No of OBRfh : {} No of TRh : {} No of OBs :  {} No Of EOs : {} No Of STas : {}", totalLowVolumeMessages, noOfBDSh, noOfBDLi, noOfBDIn, noOfINs, noOfBDRi, noOfBDBo, noOfOBRf, noOfTRh, noOfOBs, noOfEOs, noOfSTas);
									  
									//Utility.sleepAnalyser(1);
								}//End of InnerWhile
							}
					
							MessageQueue.lowVlmMsgQueue.awaitData(TipConstants.PARSER_QUEUE_IDLE_WAIT_MS); // was: sleep(DATA_ANALYZER_SLEEP_INTERVAL_LOWVOLUME) polling
				}//Outer while
		}//Function run End
		
		
	//Parse for BDSh messages
	private void parseBasicDataShare(String[] messageValue){
		 finished = false;		
		 tmpid=0;
		 tmpMarketId=0;
		 tmpSym=null;
		 tmpNam=null;
		 tmpSymbolId=null;
		 tmpIterId=0;
		 sequence_no = 0;
		 tmpUc=0;
		 tmpListingDate =null;
					
		for (int i=1;i< messageValue.length && !finished; i++) {
			value = Utility.splitTagValue(messageValue[i]);
			if (value != null && value.length > 1) {
				if (value[0] != null && value[1] != null) {
					messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
				}
				else {
					break;
				}
				
							switch(BasicDataShare.getMessageValue(messageColumnType)){				
								case 1 : // IDCODE (1,"i")
								      tmpid=Integer.parseInt(value[1]);									
									break;
								
								case 2 : // MARKETID (2,"m")
									tmpMarketId = Integer.parseInt(value[1]);									
									break;
									
								case 3 : // SOURCEID (3,"Si")
									  tmpSymbolId=value[1];									
									break;
								
								case 4 : // UPDATECODE (4,"Uc"),
									tmpUc = Integer.parseInt(value[1]);	
									break;
								
								case 5 :// Symbol (5,"Sym"),
									tmpSym=value[1];									
									break;
								
								case 6 : // Name (6,"NAm"),
									tmpNam=value[1];
									break;	
								
								case 7 : //LISTINGDATE (7,"LDa"),
									tmpListingDate=value[1];									 
									finished =true;
									break;	
									
								case 8 : // SEQUENCENO(7,"n");
									sequence_no=Long.parseLong(value[1]);	
									TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
									break;
									
								default:
									break;
							}	//End of Switch		
			}
		}//End of For loop	
		
					
		logger.info("BDSH tmpMarketId : {} Symbol :{}", tmpMarketId, tmpSymbolId);
						if(tmpMarketId==TipConstants.SHARE_MARKET_ID)
							{								
							 //To check already available data
			            	 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
			            	 {
			            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
			            	 }else
			            	 {
			            		 TIPDataObjects.tipShare[TIPDataObjects.tipShareIter]=new TIP_ShareVO(); 
			            		 tmpIterId = TIPDataObjects.tipShareIter;
			            		 TIPDataObjects.tipShareIter++; 
			                	TIPDataObjects.tipShare_Symbol_ArrayIndexMap.put(tmpSym, tmpIterId);
			                	logger.info("BDSH Main Markey Array added for Symbol :{}", tmpSym);
			            	 }
								TIPDataObjects.tipShare[tmpIterId].Id=tmpid;
								TIPDataObjects.tipShare[tmpIterId].fk_te_symbol=tmpSym;	
								
								
								//Added for Prevent Historic Data transfer for inactive companies
								if(null != tmpListingDate) 
								{
									try {
										TIPDataObjects.tipShare[tmpIterId].listingDate = new Date(TipConstants.dateFormatter.parse(tmpListingDate).getTime());
									} catch (ParseException e) {
										logger.error("Error Occured in parseBasicDataShare" +e.getLocalizedMessage());
									}
								}
															
								 /* Added for Share RI */
								TIPDataObjects.tipShare[tmpIterId].isShareRI = false;
			                    /******* Added for ETF **********/
			                    try
			                    {
			                    if(LoadInitialData.hmETFSymbols.containsKey(TIPDataObjects.tipShare[tmpIterId].fk_te_symbol))
			                    {
			                    	TIPDataObjects.tipShare[tmpIterId].isShareETF=true;
			                    }
			                    
			                    TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
			                    
			                        // insert any new share symbol to database
			                    if ((LoadInitialData.hmShareSymbols != null) && (LoadInitialData.hmShareSymbols.get(tmpSym) == null)) {
			                    	LoadInitialData.hmShareSymbols.put(tmpSym, tmpSym);			                    
			                        dbUtilObj.insertSymbolData(tmpSym,TipConstants.SHARE);
			                       
			                        /******* Added for ETF *********************/
			                        if(TIPDataObjects.tipShare[tmpIterId].isShareETF)
			                        {
			                        	logger.info("New ETF Symbol received :{}", tmpSym);
			                        	dbUtilObj.insertShareETFData(TIPDataObjects.tipShare[tmpIterId]);
			                        }else
			                        {
			                        	logger.info("New Share Symbol received :{}", tmpSym);
			                        	dbUtilObj.insertShareData(TIPDataObjects.tipShare[tmpIterId]);
			                        }		                        
			                     
			                    }else //Existing symbols. Hence update only listing date
			                    {
			                    	if(TIPDataObjects.tipShare[tmpIterId].isShareETF)
			                        {
			                        	logger.info("Existing ETF Symbol received :{}", tmpSym);
			                        	dbUtilObj.updateETFShareDataListingDate(TIPDataObjects.tipShare[tmpIterId]);
			                        }else
			                        {
			                        	logger.info("Existing Share Symbol received :{}", tmpSym);
			                        	dbUtilObj.updateShareDataListingDate(TIPDataObjects.tipShare[tmpIterId]);
			                        }	
			                    	
			                    }
			                    
			                    }catch(Exception e)
			                    {
			                    	 logger.info("Error :{}", e.getMessage());
			                    	e.printStackTrace();
			                    	
			                    }
								
							}else if(tmpMarketId==TipConstants.SME_MARKET_ID)
				{
							 //To check already available data
				            	 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }else
				            	 {
				            		 TIPDataObjects.tipSMEShare[TIPDataObjects.tipShareSMEIter]=new TIP_SMEShareVO();
				            		 tmpIterId = TIPDataObjects.tipShareSMEIter;
				            		 TIPDataObjects.tipShareSMEIter++; 
				                	TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.put(tmpSym, tmpIterId);
				            	 }
							TIPDataObjects.tipSMEShare[tmpIterId].Id=tmpid;
							TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol=tmpSym;	
							//Added for Prevent Historic Data transfer for inactive companies
							if(null != tmpListingDate) 
							{
								try {
									TIPDataObjects.tipSMEShare[tmpIterId].listingDate = new Date(TipConstants.dateFormatter.parse(tmpListingDate).getTime());
								} catch (ParseException e) {
									logger.error("Error Occured in parseBasicDataShare" +e.getLocalizedMessage());
								}
							}	
							
							 /* Added for Share RI */
							TIPDataObjects.tipSMEShare[tmpIterId].isShareRI = false;
		                    /******* Added for ETF **********/
		                    try
		                    {
		                        // insert any new share symbol to database
		                    if ((LoadInitialData.hmSMEShareSymbols != null) && (LoadInitialData.hmSMEShareSymbols.get(tmpSym) == null)) {
		                    	LoadInitialData.hmSMEShareSymbols.put(tmpSym, tmpSym);
		                    	dbUtilObj.insertSymbolData(tmpSym,TipConstants.SME);	
		                        logger.info("New SME Share Symbol received :{}", tmpSym);
		                        dbUtilObj.insertSMEShareData(TIPDataObjects.tipSMEShare[tmpIterId]);
		                    }else  //Existing symbols. Hence update only listing date
		                    {	logger.info("Existing ShareSME Symbol received :{}", tmpSym);
	                        	dbUtilObj.updateSMEShareDataListingDate(TIPDataObjects.tipSMEShare[tmpIterId]);}
		                    
		                    }catch(Exception e)
		                    {
		                    	
		                    	
		                    }
						}else   //Added for PTTP Project CRQ22398
						{
									if(tmpMarketId==TipConstants.OTC_MARKET_ID)
									{					
									logger.info("OTCD  BDSH Received for symbol :{} UpdateCode :{}", tmpSymbolId, tmpUc);
									 //To check already available data
					            	 if(TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
					            	 {
					            		 if(tmpUc == 3) //Deleted
					            		 {
					            			 logger.info("Update code 3 Received. Hence the symbol will be removed");
					            			 TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.remove(tmpSymbolId);
					            		 }else
					            		 {
					            			 tmpIterId = (int)TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
					            		 }
					            	 }else
					            	 {
					            		 logger.info("OTC BDSH Received BDSH tmpMarketId : {}", tmpSym);
					            		 
					            		 if(tmpUc ==1 || tmpUc ==2 || tmpUc ==4)  //1.Insert 2.Update 3.Unchanged
					            		 {
					            			 TIPDataObjects.tipOTCShare[TIPDataObjects.tipOTCShareIter]=new TIP_ShareVO(); 
					            			 tmpIterId = TIPDataObjects.tipOTCShareIter;
					            			 TIPDataObjects.tipOTCShareIter++; 
					            			 TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.put(tmpSym, tmpIterId);
					            			 logger.info("OTC BDSH Received BDSH TIPDataObjects.tipOTCShareIter : {}", TIPDataObjects.tipOTCShareIter);
					            		 }
					            	 
										TIPDataObjects.tipOTCShare[tmpIterId].Id=tmpid;
										TIPDataObjects.tipOTCShare[tmpIterId].fk_te_symbol=tmpSym;
					            	 }
										
									}
						}
	}
	
	
	//Parse for BDBo messages
		private void parseBasicDataBond(String[] messageValue){
			 finished = false;
			 tmpid=0;
			 tmpSym=null;
			 tmpNam=null;
			 tmpSymbolId=null;
			 tmpIsin=null;
			tmpIterId=0;
			sequence_no=0;
			tmpMarketId =0;
			tmpoutStandingAmount=null;tmpIssueCurrency=null;tmpNominalValue=null;tmpCouponFrequency=null;tmpDaycountMethod=null;tmpCouponRate=null;tmpMaturityDate=null;
			 tmpListingDate = null;  tmpIssueDate=null ; tmpSettlementDate =null; tmpIssueAmount =null; ;updateCode=null;
			tmpRateCalcType =null;
			
			for (int i=1;i< messageValue.length ; i++) {
				value = Utility.splitTagValue(messageValue[i]);
				if (value != null && value.length > 1) {
					if (value[0] != null && value[1] != null) {
						messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
					}
					else {
						break;
					}
					 //For Bond, both listed and unlisted are in same table and hence no need to segregate
								switch(BasicDataBond.getMessageValue(messageColumnType)){				
									case 1 : // IDCODE (1,"i")									
										  tmpid=Integer.parseInt(value[1]);		
										break;
										
									case 2 : // SOURCEID (2,"Si")
										  tmpSymbolId=value[1];										 
										break;	
										
									case 3 : // MARKETID (3,"m")
										tmpMarketId = Integer.parseInt(value[1]);									
										break;
									
									case 4 :// Symbol (4,"Sym"),
										tmpSym=value[1];		
										break;
									
									case 5 : // Name (5,"NAm"),
										tmpNam=value[1];			
										//finished =true;
										break;	
										
									case 6 : // SEQUENCENO(6,"n");
										sequence_no=Long.parseLong(value[1]);	
										TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
										break;	
										
									case 7 : // ISIN(7,"ISn");
										tmpIsin=value[1];
										break;	
										
									case 8 : //OUTSTANDINGAMOUNT(8,"AOs"),
										tmpoutStandingAmount=value[1];
										break;	
										
									case 9 : //ISSUANCECURRENCY(9,"CUi"),
										tmpIssueCurrency=value[1];
										break;	
										
									case 10 :// ROUNDLOTVALUE(10,"LOt"),
										tmpNominalValue=value[1];
										break;	
										
									case 11 : // COUPONFREQUENCY(11,"CFq"),
										tmpCouponFrequency=value[1];
										break;	
										
									case 12 : // DAYCOUNTMETHOD(12,"DCm"),
										tmpDaycountMethod=value[1];
										break;	
										
									case 13 : // COUPONRATE(13,"RCp"),
										tmpCouponRate=value[1];
										break;	
										
									case 14 : // MATURITYDATE(14,"DMa");
										tmpMaturityDate=value[1];
										break;	
													
									case 15 : // RATECALCTYPE(15,"RCt");
										tmpRateCalcType=value[1];
										break;										
										
									case 16 : // LISTINGDATE(16,"LDa"),
										tmpListingDate=value[1];
										break;											
										
									case 17 : //ISSUEAMOUNT(17,"AIs"),
										tmpIssueAmount=value[1];
										break;											
										
									case 18 : // ISSUEDATE(18,"DIs"),
										tmpIssueDate=value[1];
										break;											
										
									case 19 : // SETTLENTDATE(19,"STd");
										tmpSettlementDate=value[1];
										break;	
									
									case 20 : // UPDATECODE (3,"Uc"),
										updateCode = value[1];
										break;
										
									default:
										break;
								}	//End of Switch
				}
			}//End of For loop	
			
			
			logger.info("BDBO MarketID :{}", tmpMarketId);
			logger.info("TipConstants.BOND_MARKET_ID :{}", TipConstants.BOND_MARKET_ID);
			logger.info("TipConstants.BOND_OTC_MARKET_ID :{}", TipConstants.BOND_OTC_MARKET_ID);
			if(tmpMarketId==TipConstants.BOND_MARKET_ID)
			{		
						//To check already available data
				       	 if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				       	 {
				       		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				       	 }else
				       	 {
				       		TIPDataObjects.tipBond[TIPDataObjects.tipBondIter]=new TIP_BondVO();
				       		tmpIterId=TIPDataObjects.tipBondIter;
				       		TIPDataObjects.tipBondIter++;
				           	TIPDataObjects.tipBond_Symbol_ArrayIndexMap.put(tmpSym, tmpIterId);
				       	 }
					
				     logger.info("Normal Bond received for symbol :{}", tmpSym);
				       	 
					TIPDataObjects.tipBond[tmpIterId].BondId=tmpid;
					if(null != tmpSym) TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol=tmpSym;	
					if(null != tmpIsin) TIPDataObjects.tipBond[tmpIterId].isin=tmpIsin;	
					
					//Added for Debt Market EnhancementsCRQ21357
					if(null != tmpoutStandingAmount) TIPDataObjects.tipBond[tmpIterId].outstandingAmount=Math.round(Double.parseDouble(tmpoutStandingAmount) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					if(null != tmpIssueCurrency) TIPDataObjects.tipBond[tmpIterId].issueCurrency=tmpIssueCurrency;	
					if(null != tmpNominalValue) TIPDataObjects.tipBond[tmpIterId].parValue=Math.round(Double.parseDouble(tmpNominalValue) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
					if(null != tmpCouponFrequency) TIPDataObjects.tipBond[tmpIterId].couponFrequency=Math.round(Double.parseDouble(tmpCouponFrequency) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					if(null != tmpDaycountMethod) TIPDataObjects.tipBond[tmpIterId].dayCountMethod=Integer.parseInt(tmpDaycountMethod);	
					if(null != tmpRateCalcType) TIPDataObjects.tipBond[tmpIterId].rateCalcType=Integer.parseInt(tmpRateCalcType);	
					if(null != tmpCouponRate) TIPDataObjects.tipBond[tmpIterId].couponRate=Math.round(Double.parseDouble(tmpCouponRate) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
				
					try
					{
						if(null != updateCode) TIPDataObjects.tipBond[tmpIterId].updateCode = Integer.parseInt(updateCode);
					}catch(Exception e)
					{
						logger.error("Error occured in parsing the updateCode", e);
					}
					
					if(null != tmpMaturityDate) 
					{
						try {
							TIPDataObjects.tipBond[tmpIterId].maturityDate = new Date(TipConstants.dateFormatter.parse(tmpMaturityDate).getTime());
						} catch (ParseException e) {
							logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
						}
					}
					
					//Added for OTC Project PTTP
					if(null != tmpListingDate) 
					{
						try {
							TIPDataObjects.tipBond[tmpIterId].listingDate = new Date(TipConstants.dateFormatter.parse(tmpListingDate).getTime());
						} catch (ParseException e) {
							logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
						}
					}
					
					if(null != tmpIssueDate) 
					{
						try {
							TIPDataObjects.tipBond[tmpIterId].issueDate = new Date(TipConstants.dateFormatter.parse(tmpIssueDate).getTime());
						} catch (ParseException e) {
							logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
						}
					}
					
					if(null != tmpSettlementDate) 
					{
						try {
							TIPDataObjects.tipBond[tmpIterId].settlementDate = new Date(TipConstants.dateFormatter.parse(tmpSettlementDate).getTime());
						} catch (ParseException e) {
							logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
						}
					}
					
					if(null != tmpIssueAmount) TIPDataObjects.tipBond[tmpIterId].issueAmount=Math.round(Double.parseDouble(tmpIssueAmount) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
						
					
					TIPDataObjects.tipBond[tmpIterId].sequence_no=sequence_no;
					// insert any new bond symbol to database
                    if ((LoadInitialData.hmBondSymbols != null) && (LoadInitialData.hmBondSymbols.get(tmpSym) == null)) {  
                    	logger.info("New Bond Symbol Received :{}", tmpSym);
                    	LoadInitialData.hmBondSymbols.put(tmpSym, tmpSym);   
                    	//ToDo
                    	 dbUtilObj.insertSymbolData(tmpSym,TipConstants.BOND);   
                    	 logger.info("dbUtilObj.insertSymbolData(tmpSym,TipConstants.BOND) is completed:{}", tmpSym);
                        dbUtilObj.insertBondData(TIPDataObjects.tipBond[tmpIterId]);
                        logger.info(" dbUtilObj.insertBondData(TIPDataObjects.tipBond[tmpIterId])is completed:{}", TIPDataObjects.tipBond[tmpIterId]);
                        dbUtilObj.commitTrans(true);
                        //tipBondQ.push(new Integer(tmpIter2));
                    }else
                    {
                    	
                    	//To verify that the Bonds Exists in EFP.FI_STOCK
                    	if(dbUtilObj.bondExistInFIStock(tmpSym))
                    	{
                    		logger.info("Bond data found in EFP.FI_STOCK table. Hence the data will be updated");
                    	  dbUtilObj.updateBondBasicData(TIPDataObjects.tipBond[tmpIterId]);
                    	}else
                    	{
                    		logger.info("Bond data is not found in EFP.FI_STOCK table. Hence the data will be inserted");
                    		  dbUtilObj.insertBondData(TIPDataObjects.tipBond[tmpIterId]);
                    	}	
                    	
                    }
                    
                    dbUtilObj.updateBondSubType(TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipBond[tmpIterId].rateCalcType);
                    
                    switch(TIPDataObjects.tipBond[tmpIterId].updateCode)
	                   {
	                   	
	                   		case TipConstants.UPDATE_CODE_DELETE :	                   			
	                   			logger.info("Bonds  Basic Data update  UPDATE_CODE_DELETE : - Symbol : "+tmpSym +" Code :"+ +TIPDataObjects.tipBond[tmpIterId].updateCode);	                   		
	                   		 	TIPDataObjects.tipBond_Symbol_ArrayIndexMap.remove(tmpSym);	                   			
	                   			logger.info("Bonds  Basic Data update UPDATE_CODE_DELETE ");
                				 break;
                				 
	                   		case TipConstants.UPDATE_CODE_UNCHANGED :
	                   		//No action is required
                				 break;		
                				 
	                   		case TipConstants.UPDATE_CODE_INSERT :	                   			
	                   			logger.info("Bonds  Basic Data update LISTED UPDATE_CODE_INSERT : {}", TIPDataObjects.tipBond[tmpIterId].updateCode);
	                   			dbUtilObj.updateBondType(TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol, TipConstants.BOND_TYPE_LISTED);
	                   			logger.info("Bonds  Basic Data update LISTED UPDATE_CODE_INSERT ");
                				 break;
	                   }
                    
			}else if(tmpMarketId==TipConstants.BOND_OTC_MARKET_ID)
				{	
					
				logger.info("Inside tmpMarketId==TipConstants.BOND_OTC_MARKET_ID :{}", tmpSymbolId);
							//To check already available data
					       	 if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
					       	 {
					       		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
					       	 }else
					       	 {
					       		TIPDataObjects.tipOTCBond[TIPDataObjects.tipOTCBonditer]=new TIP_BondVO();
					       		tmpIterId=TIPDataObjects.tipOTCBonditer;
					       		TIPDataObjects.tipOTCBonditer++;
					           	TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.put(tmpSym, tmpIterId);
					       	 }
						logger.info("OTC Bond received for symbol :{}", tmpSym);
					     
						TIPDataObjects.tipOTCBond[tmpIterId].BondId=tmpid;
						if(null != tmpSym) TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol=tmpSym;	
						if(null != tmpIsin) TIPDataObjects.tipOTCBond[tmpIterId].isin=tmpIsin;	
						
						//Added for Debt Market EnhancementsCRQ21357
						if(null != tmpoutStandingAmount) TIPDataObjects.tipOTCBond[tmpIterId].outstandingAmount=Math.round(Double.parseDouble(tmpoutStandingAmount) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
						if(null != tmpIssueCurrency) TIPDataObjects.tipOTCBond[tmpIterId].issueCurrency=tmpIssueCurrency;	
						if(null != tmpNominalValue) TIPDataObjects.tipOTCBond[tmpIterId].parValue=Math.round(Double.parseDouble(tmpNominalValue) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
						if(null != tmpCouponFrequency) TIPDataObjects.tipOTCBond[tmpIterId].couponFrequency=Math.round(Double.parseDouble(tmpCouponFrequency) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
						if(null != tmpDaycountMethod) TIPDataObjects.tipOTCBond[tmpIterId].dayCountMethod=Integer.parseInt(tmpDaycountMethod);	
						if(null != tmpRateCalcType) TIPDataObjects.tipOTCBond[tmpIterId].rateCalcType=Integer.parseInt(tmpRateCalcType);	
						if(null != tmpCouponRate) TIPDataObjects.tipOTCBond[tmpIterId].couponRate=Math.round(Double.parseDouble(tmpCouponRate) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
						
						try
						{
							if(null != updateCode) TIPDataObjects.tipOTCBond[tmpIterId].updateCode = Integer.parseInt(updateCode);
						}catch(Exception e)
						{
							logger.error("Error occured in parsing the updateCode", e);
						}
						
						if(null != tmpMaturityDate) 
						{
							try {
								TIPDataObjects.tipOTCBond[tmpIterId].maturityDate = new Date(TipConstants.dateFormatter.parse(tmpMaturityDate).getTime());
							} catch (ParseException e) {
								logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
							}
						}
						
						//Added for OTC Project PTTP
						if(null != tmpListingDate) 
						{
							try {
								TIPDataObjects.tipOTCBond[tmpIterId].listingDate = new Date(TipConstants.dateFormatter.parse(tmpListingDate).getTime());
							} catch (ParseException e) {
								logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
							}
						}
						
						if(null != tmpIssueDate) 
						{
							try {
								TIPDataObjects.tipOTCBond[tmpIterId].issueDate = new Date(TipConstants.dateFormatter.parse(tmpIssueDate).getTime());
							} catch (ParseException e) {
								logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
							}
						}
						
						if(null != tmpSettlementDate) 
						{
							try {
								TIPDataObjects.tipOTCBond[tmpIterId].settlementDate = new Date(TipConstants.dateFormatter.parse(tmpSettlementDate).getTime());
							} catch (ParseException e) {
								logger.error("Error Occured in parseBasicDataBond" +e.getLocalizedMessage());
							}
						}
						
						if(null != tmpIssueAmount) TIPDataObjects.tipOTCBond[tmpIterId].issueAmount=Math.round(Double.parseDouble(tmpIssueAmount) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
							
						
						TIPDataObjects.tipOTCBond[tmpIterId].sequence_no=sequence_no;
						// insert any new bond symbol to database
	                    if ((LoadInitialData.hmOTCBondSymbols != null) && (LoadInitialData.hmOTCBondSymbols.get(tmpSym) == null)) {  
	                    	logger.info("New OTC Bond Symbol Received :{}", tmpSym);
	                    	LoadInitialData.hmOTCBondSymbols.put(tmpSym, tmpSym);   
	                    	//ToDo
	                    	 dbUtilObj.insertSymbolData(tmpSym,TipConstants.BOND_OTC);   
	                    	 logger.info("dbUtilObj.insertSymbolData(tmpSym,TipConstants.BOND) is completed:{}", tmpSym);
	                        dbUtilObj.insertOTCBondData(TIPDataObjects.tipOTCBond[tmpIterId]);
	                        logger.info(" dbUtilObj.insertBondData(TIPDataObjects.tipOTCBond[tmpIterId])is completed:{}", TIPDataObjects.tipOTCBond[tmpIterId]);
	                        dbUtilObj.commitTrans(true);
	                        //tipBondQ.push(new Integer(tmpIter2));
	                    }else
	                    {
	                    	logger.info("Existing OTC Bond Symbol Received :{}", tmpSym);
	                    	//To verify that the Bonds Exists in EFP.FI_STOCK
	                    	if(dbUtilObj.bondExistInFIStockUnlisted(tmpSym))
	                    	{
	                    		logger.info("Bond data found in EFP.FI_STOCK table. Hence the data will be updated");
	                    	 // dbUtilObj.updateBondBasicData(TIPDataObjects.tipOTCBond[tmpIterId]);
	                    	  dbUtilObj.updateOTCBondBasicData(TIPDataObjects.tipOTCBond[tmpIterId]);
	                    	}else
	                    	{
	                    		logger.info("Bond data is not found in EFP.FI_STOCK table. Hence the data will be inserted");
	                    		  dbUtilObj.insertOTCBondData(TIPDataObjects.tipOTCBond[tmpIterId]);
	                    	}
	                    }
	                    
	                    dbUtilObj.updateBondSubType(TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipOTCBond[tmpIterId].rateCalcType);
	                    
	                    logger.info("Bonds  Basic Data update Code for symbol :{} is :{}", TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipOTCBond[tmpIterId].updateCode);
	                    
	                    switch(TIPDataObjects.tipOTCBond[tmpIterId].updateCode)
		                   {
		                   		
		                   		case TipConstants.UPDATE_CODE_DELETE :	                   			
		                   			logger.info("Bonds  Basic Data update OTC UPDATE_CODE_DELETE - Symbol : {} Code :{}", tmpSym, TIPDataObjects.tipOTCBond[tmpIterId].updateCode);
		                   		 	TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.remove(tmpSym);	                   			
		                   			logger.info("Bonds  Basic Data update OTC UPDATE_CODE_DELETE ");
	                				 break;
	                				 
		                   		case TipConstants.UPDATE_CODE_UNCHANGED :
		                   		//No action is required
	                				 break;		
	                				 
		                   		case TipConstants.UPDATE_CODE_INSERT :	                   			
		                   			logger.info("Bonds  Basic Data update OTC UPDATE_CODE_INSERT : {}", TIPDataObjects.tipOTCBond[tmpIterId].updateCode);
		                   			dbUtilObj.updateBondType(TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol, TipConstants.BOND_TYPE_UNLISTED);	
		                   			logger.info("Bonds  Basic Data update OTC UPDATE_CODE_INSERT ");
	                				 break;
		                   }
	                    
				}
                    
		}
		
		//Parse for BDIn messages
		private void parseBasicDataIndex(String[] messageValue){
			finished = false;
			tmpid=0;
			tmpSym=null;
			tmpNam=null;
			tmpNam_locale=null;
			tmpSymbolId=null	;
			tmpIndexIter=0;
			sequence_no=0;
			String tmpVSt=null;
			
			for (int i=1;i< messageValue.length && !finished; i++) {
				value = Utility.splitTagValue(messageValue[i]);
				if (value != null && value.length > 1) {
					if (value[0] != null && value[1] != null) {
						messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
					}
					else {
						break;
					}
					
										
								switch(BasicDataIndex.getMessageValue(messageColumnType)){				
									case 1 : // IDCODE (1,"i")
										 tmpid=Integer.parseInt(value[1]);	
										break;
									
									case 2 : // SOURCEID (2,"Si")
										  tmpSymbolId=value[1];	
										 break;
									
									case 3 : // Symbol (3,"Sym"),
										tmpSym=value[1];											 
										break;
									
									case 4 :// Name (4,"NAm"),
										tmpNam=value[1];		
										break;
									
									case 5 : // NameLocale (5,"NAl"),
										tmpNam_locale = value[1];										
										break;	
										
									case 6 :// 	SEQUENCENO(6,"n");
										sequence_no=Long.parseLong(value[1]);	
										TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
										break;
									
									case 7 :// 	INITIAL_PRICE(7,"VSt");
										tmpVSt=value[1];	
										break;
									default:
										break;
								}	//End of Switch
				}
			}//End of For loop	
			
			 logger.info("BDIn Ind Sym :{}", tmpSym);
					 //Main Market
		             if ((LoadInitialData.hmMainMarketIndex != null) && (LoadInitialData.hmMainMarketIndex.get(tmpSym) != null))
		             {
		            	    logger.info("BDIn Ind Sym ibside hmMainMarketIndex:{}", tmpSym);
		            	 
			            	 //To check already available data
			            	 if(TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSym) !=null)
			            	 {
			            		 tmpIndexIter = (int)TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSym);
			            		
			            	 }else
			            	 {
			                	TIPDataObjects.tipIndex[TIPDataObjects.tipIndexIter]=new TIP_IndexVO();	     
			                	tmpIndexIter = TIPDataObjects.tipIndexIter;
			                	TIPDataObjects.tipIndexIter++; 
			                	TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.put(tmpSym, tmpIndexIter);
			                	
			            	 }
			            	 
			            	 //Add Index Also 
			            	 	TIPDataObjects.tipIndexConsolidated_IdCodeToSymbolMap.put(tmpid,tmpSym);
			            	 	logger.info("tipIndexConsolidated_IdCodeToSymbolMap hmMainMarketIndex Added: tmpid :{} IndexSymbol :{}", tmpid, tmpSym);
			            	 
			            	 	TIPDataObjects.tipIndex[tmpIndexIter].ListId=tmpid;
			 					if(null != tmpSym) TIPDataObjects.tipIndex[tmpIndexIter].Fk_te_symbol=tmpSym;	
			 					if(null != tmpNam) TIPDataObjects.tipIndex[tmpIndexIter].Description_EN=tmpNam;	            
			 				//	logger.info("TIPDataObjects.tipIndex[tipIndexIter] : Index {} Symbol: {} ID: {}", tmpIndexIter, TIPDataObjects.tipIndex[tmpIndexIter].Fk_te_symbol, TIPDataObjects.tipIndex[tmpIndexIter].ListId);
							      		
			 					TIPDataObjects.tipIndex[tmpIndexIter].sequence_no = sequence_no;
							 // insert any new share symbol to database
		                    if ((LoadInitialData.hmShareSymbols != null) && (LoadInitialData.hmShareSymbols.get(tmpSym) == null)) {
		                    	LoadInitialData.hmShareSymbols.put(tmpSym, tmpSym);
		                       
		                    	//ToDo
		                    	dbUtilObj.insertSymbolData(tmpSym,TipConstants.SHARE);
		                        dbUtilObj.insertIndexData(TIPDataObjects.tipIndex[tmpIndexIter]);
		                        //dbUtilObj.commitTrans();
		                        
		                    }
		             }else if((LoadInitialData.hmSMEMarketIndex != null) && (LoadInitialData.hmSMEMarketIndex.get(tmpSym) != null))
		             {
		            	 logger.info("Ind Sym :inside hmSMEMarketIndex{}", tmpSym);
			            	//To check already available data
			            	 if( TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSym) !=null)
			            	 {
			            		 tmpIndexIter = (int)TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSym);
			            	 }else
			            	 {
			            		 TIPDataObjects.tipSMEIndex[TIPDataObjects.tipSMEIndexIter]=new TIP_SMEIndexVO();
			            		  tmpIndexIter = TIPDataObjects.tipSMEIndexIter;
			            		  TIPDataObjects.tipSMEIndexIter++; 
			            		  TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.put(tmpSym, tmpIndexIter);
			            		  
			            	 }
			            	//Add Index Also 
			            	 	TIPDataObjects.tipIndexConsolidated_IdCodeToSymbolMap.put(tmpid,tmpSym);
			            	 	logger.info("tipIndexConsolidated_IdCodeToSymbolMap hmSMEMarketIndex Added: tmpid :{} IndexSymbol :{}", tmpid, tmpSym);
			            	 	
			  					TIPDataObjects.tipSMEIndex[tmpIndexIter].ListId=tmpid;
			  					if(null != tmpSym)TIPDataObjects.tipSMEIndex[tmpIndexIter].Fk_te_symbol=tmpSym;	
			  					if(null != tmpNam)TIPDataObjects.tipSMEIndex[tmpIndexIter].Description_EN=tmpNam;	
		  					
			  					TIPDataObjects.tipSMEIndex[tmpIndexIter].sequence_no = sequence_no;
			  					
		            		//	logger.info("TIPDataObjects.tipSMEIndex[tmpIndexIter] : Index {} Symbol: {} ID: {}", tmpIndexIter, TIPDataObjects.tipSMEIndex[tmpIndexIter].Fk_te_symbol, TIPDataObjects.tipSMEIndex[tmpIndexIter].ListId);
						   
							 // insert any new share symbol to database
		                    if ((LoadInitialData.hmSMEShareSymbols != null) && (LoadInitialData.hmSMEShareSymbols.get(tmpSym) == null)) {
		                    	LoadInitialData.hmSMEShareSymbols.put(tmpSym, tmpSym);
		                        //ToDo
		                   // 	 logger.info("Ind Sym :inside insert sysmbol");
		                    	 dbUtilObj.insertSymbolData(tmpSym,TipConstants.SME);
		                         dbUtilObj.insertSMEIndexData(TIPDataObjects.tipSMEIndex[tmpIndexIter]);
		                        // dbUtilObj.commitTrans();
		                       
		                    }
		             }else if((LoadInitialData.hmBondMarketIndex != null) && (LoadInitialData.hmBondMarketIndex.get(tmpSym) != null))
		             {
		            	 logger.info("Ind Sym :inside hmBondMarketIndex{}", tmpSym);
		            	 try
		            	 {
			            	//To check already available data
			            	 if( TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSym) !=null)
			            	 {
			            		 tmpIndexIter = (int)TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSym);
			            		 logger.info("Ind Sym :inside hmBondMarketIndex inside If . Match Found{}", tmpSym);
			            	 }else
			            	 {
			            		 logger.info("Ind Sym :inside hmBondMarketIndex 1 {}", tmpSym);
			            		 TIPDataObjects.tipBondIndex[TIPDataObjects.tipBondIndexIter]=new TIP_BondIndexVO();
			            		 logger.info("Ind Sym :inside hmBondMarketIndex 2 {}", tmpSym);
			            		  tmpIndexIter = TIPDataObjects.tipBondIndexIter;
			            		  TIPDataObjects.tipBondIndexIter++; 
			            		  TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.put(tmpSym, tmpIndexIter);
			            		  logger.info("Ind Sym :inside hmBondMarketIndex inside If . Match Not Found{} tmpIndexIter:{}", tmpSym, tmpIndexIter);
			            		 
			            	 }	
		            	 }catch(Exception e)
			            {
		            		 logger.error("hmBondMarketIndex Error Occured", e);
			            }
			            	
			            	//Add Index Also 
			            	 try
			            	 {
			            	 logger.info("Ind Sym :inside hmBondMarketIndex 1{} tmpId {}", tmpSym, tmpid);
			            	 	TIPDataObjects.tipIndexConsolidated_IdCodeToSymbolMap.put(tmpid,tmpSym);
			            	 	 logger.info("Ind Sym :inside hmBondMarketIndex 2{} tmpId {}", tmpSym, tmpid);
			            	 	logger.info("tipIndexConsolidated_IdCodeToSymbolMap hmBondMarketIndex Added: tmpid :{} IndexSymbol :{}", tmpid, tmpSym);
			            	 	
								TIPDataObjects.tipBondIndex[tmpIndexIter].ListId=tmpid;
								if(null != tmpSym)TIPDataObjects.tipBondIndex[tmpIndexIter].Fk_te_symbol=tmpSym;	
								if(null != tmpNam)TIPDataObjects.tipBondIndex[tmpIndexIter].Description_EN=tmpNam;
								if(null != tmpVSt)TIPDataObjects.tipBondIndex[tmpIndexIter].open_price= Math.round(Double.parseDouble(tmpVSt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
							
								TIPDataObjects.tipBondIndex[tmpIndexIter].sequence_no = sequence_no;
		            	 	logger.info("TIPDataObjects.tipBondIndex[tmpIndexIter] : Index {} Symbol: {} ID: {}", tmpIndexIter, TIPDataObjects.tipBondIndex[tmpIndexIter].Fk_te_symbol, TIPDataObjects.tipBondIndex[tmpIndexIter].ListId);
						   
							// insert any new Bond symbol to database
		                    if ((LoadInitialData.hmBondSymbols != null) && (LoadInitialData.hmBondSymbols.get(tmpSym) == null)) {
		                    	LoadInitialData.hmBondSymbols.put(tmpSym, tmpSym);
		                        //ToDo
		                    	dbUtilObj.insertSymbolData(tmpSym,TipConstants.BOND);
		                        dbUtilObj.insertBondIndexData(TIPDataObjects.tipBondIndex[tmpIndexIter]);
		                       // dbUtilObj.commitTrans();
		                    }
			            }catch(Exception e)
			            	 {
			            	    logger.error("hmBondMarketIndex Error Occured", e);
			            	 }
		             }	
		}
	
		//Parse for BDLi messages
				private void parseBasicDataLists(String[] messageValue){
					finished = false;
					tmpid=0;
					tmpSym=null;
					tmpNam=null;
					tmpSymbolId=null;			
					String tmpTipIndexID=null;
					tmpIndexIter=0;
					sequence_no=0;
				
					for (int i=1;i< messageValue.length && !finished; i++) {
						value = Utility.splitTagValue(messageValue[i]);
						if (value != null && value.length > 1) {
							if (value[0] != null && value[1] != null) {
								messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
							}
							else {
								break;
							}
							
							// logger.info("messageColumnType in BDLi :{}", messageColumnType);
							
										switch(BasicDataList.getMessageValue(messageColumnType)){				
											case 1 : // IDCODE (1,"i")
												 tmpid=Integer.parseInt(value[1]);	
												break;										
											
											case 2 : // SOURCEID (2,"Si")
												  tmpSymbolId=value[1];													
												break;
											
											case 3 : // Symbol (4,"Sym"),
												tmpSym=value[1];													
												break;
											
											case 4 :// Name (5,"NAm"),
												tmpNam=value[1];														
												  finished =true;
												break;
											
											case 5 : // 	SEQUENCENO(5,"n");
												sequence_no=Long.parseLong(value[1]);	
												TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
												break;	
												
											default:
												break;
										}	//End of Switch
						}
					}//End of For loop			
					
					
					 tmpTipIndexID = LoadInitialData.hmListToIndexMap.get(tmpSym);
					
					 //Main Market
                     if ((LoadInitialData.hmMainMarketIndex != null) && (LoadInitialData.hmMainMarketIndex.get(tmpTipIndexID) != null))
                     {
                    	 //To check already available data
                    	 if(TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpTipIndexID) !=null)
                    	 {
                    		 tmpIndexIter = (int)TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpTipIndexID);
                    	 }else
                    	 {
                        	TIPDataObjects.tipIndex[TIPDataObjects.tipIndexIter]=new TIP_IndexVO();	     
	                    	tmpIndexIter = TIPDataObjects.tipIndexIter;
	                    	TIPDataObjects.tipIndexIter++; 
	                    	TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.put(tmpTipIndexID, tmpIndexIter);
	                    	
                    	 }
                    	 
                    	//Add Index Also 
		            	 	TIPDataObjects.tipIndexConsolidated_IdCodeToSymbolMap.put(tmpid,tmpSym);
		            	 	logger.info("tipIndexConsolidated_IdCodeToSymbolMap Added: tmpid :{} IndexSymbol :{}", tmpid, tmpSym);
                    	 
                    	 	TIPDataObjects.tipIndex[tmpIndexIter].ListId=tmpid;
	     					if(null != tmpSym) TIPDataObjects.tipIndex[tmpIndexIter].Fk_te_symbol=tmpTipIndexID;	
	     					if(null != tmpNam) TIPDataObjects.tipIndex[tmpIndexIter].Description_EN=tmpNam;	
	     					
	     					TIPDataObjects.tipIndex[tmpIndexIter].sequence_no = sequence_no;
     				//	logger.info("TIPDataObjects.tipIndex[tipIndexIter] : Index {} Symbol: {} ID: {}", tmpIndexIter, TIPDataObjects.tipIndex[tmpIndexIter].Fk_te_symbol, TIPDataObjects.tipIndex[tmpIndexIter].ListId);
     					    		
                     }else if((LoadInitialData.hmSMEMarketIndex != null) && (LoadInitialData.hmSMEMarketIndex.get(tmpTipIndexID) != null))
                     {                    
                    	//To check already available data
                    	 if( TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpTipIndexID) !=null)
                    	 {
                    		 tmpIndexIter = (int)TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpTipIndexID);
                    	 }else
                    	 {
                    		 TIPDataObjects.tipSMEIndex[TIPDataObjects.tipSMEIndexIter]=new TIP_SMEIndexVO();
                    		  tmpIndexIter = TIPDataObjects.tipSMEIndexIter;
                    		  TIPDataObjects.tipSMEIndexIter++; 
                    		  TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.put(tmpTipIndexID, tmpIndexIter);
                    		
                    	 }	 
                    	//Add Index Also 
		            	 	TIPDataObjects.tipIndexConsolidated_IdCodeToSymbolMap.put(tmpid,tmpSym);
		            	 	logger.info("tipIndexConsolidated_IdCodeToSymbolMap Added: tmpid :{} IndexSymbol :{}", tmpid, tmpSym);
	      					TIPDataObjects.tipSMEIndex[tmpIndexIter].ListId=tmpid;
	      					if(null != tmpSym) TIPDataObjects.tipSMEIndex[tmpIndexIter].Fk_te_symbol=tmpTipIndexID;	
	      					if(null != tmpNam) TIPDataObjects.tipSMEIndex[tmpIndexIter].Description_EN=tmpNam;	
	      					
	      					TIPDataObjects.tipSMEIndex[tmpIndexIter].sequence_no = sequence_no;
      					//logger.info("TIPDataObjects.tipSMEIndex[tipSMEIndexIter] : Index {} Symbol: {} ID: {}", tmpIndexIter, TIPDataObjects.tipSMEIndex[tmpIndexIter].Fk_te_symbol, TIPDataObjects.tipSMEIndex[tmpIndexIter].ListId);
      					   
                     }else if((LoadInitialData.hmBondMarketIndex != null) && (LoadInitialData.hmBondMarketIndex.get(tmpTipIndexID) != null))
                     {
                         //To check already available data
                    	 if( TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpTipIndexID) !=null)
                    	 {
                    		 tmpIndexIter = (int)TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpTipIndexID);
                    	 }else
                    	 {
                    		 TIPDataObjects.tipBondIndex[TIPDataObjects.tipBondIndexIter]=new TIP_BondIndexVO();
                    		  tmpIndexIter = TIPDataObjects.tipBondIndexIter;
                    		  TIPDataObjects.tipBondIndexIter++; 
                    		 
                    	 }	 
                    	//Add Index Also 
		            	 	TIPDataObjects.tipIndexConsolidated_IdCodeToSymbolMap.put(tmpid,tmpSym);
		            	 	logger.info("tipIndexConsolidated_IdCodeToSymbolMap Added: tmpid :{} IndexSymbol :{}", tmpid, tmpSym);
                    	
      					TIPDataObjects.tipBondIndex[tmpIndexIter].ListId=tmpid;
      					if(null != tmpSym) TIPDataObjects.tipBondIndex[tmpIndexIter].Fk_te_symbol=tmpTipIndexID;	
      					if(null != tmpNam) TIPDataObjects.tipBondIndex[tmpIndexIter].Description_EN=tmpNam;	
      					
      					TIPDataObjects.tipBondIndex[tmpIndexIter].sequence_no = sequence_no;
      					//logger.info("TIPDataObjects.tipBondIndex[tipBondIndexIter] : Index {} Symbol: {} ID: {}", tmpIndexIter, TIPDataObjects.tipBondIndex[tmpIndexIter].Fk_te_symbol, TIPDataObjects.tipBondIndex[tmpIndexIter].ListId);
      					   
                     }					
				}
				
				//Parse for BDRi messages
				private void parseBasicDataEquityRights(String[] messageValue){
					finished = false;					
					tmpid=0;
					tmpMarketId=0;
					tmpSym=null;
					tmpNam=null;
					tmpSymbolId=null;
					tmpIterId=0;
					tmpRpu=0;
					tmpRpl=0;
					tmpListingDate = null;
					sequence_no=0;
					
									
					for (int i=1;i< messageValue.length && !finished; i++) {
						value = Utility.splitTagValue(messageValue[i]);
						if (value != null && value.length > 1) {
							if (value[0] != null && value[1] != null) {
								messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
							}
							else {
								break;
							}
							
												
										switch(BasicDataRight.getMessageValue(messageColumnType)){				
											case 1 : // IDCODE (1,"i")
												 tmpid=Integer.parseInt(value[1]);												
												break;
											
											case 2 : // SOURCEID (2,"Si")
												tmpSymbolId=value[1];												  
												break;
											
											case 3 : // UPDATECODE (3,"Uc"),
												break;
											
											case 4 :// Symbol (4,"Sym"),
												tmpSym=value[1];												
												break;
											
											case 5 : // Name (5,"NAm"),
												tmpNam=value[1];												  											
												break;	
											
											case 6 : // ReferncePrice_Upper (6,"RPu"),
												tmpRpu=Integer.parseInt(value[1]);
												break;	
											
											case 7 : // ReferncePrice_Lower (7,"RPl"),
												tmpRpl=Integer.parseInt(value[1]);										
												finished =true;
												break;	
												
											case 8 : // marketcode (8,"m"),
												tmpMarketId = Integer.parseInt(value[1]);											
												break;	
												
											case 9 : // Listing_Date (9,"LDa"),
												tmpListingDate = value[1];											
												break;
												
											case 10 : // SEQUENCENO(10,"n");
												sequence_no = Long.parseLong(value[1]);		
												TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
												break;	
												
											default:
												break;
										}	//End of Switch
						}
					}//End of For loop		
					
								if(tmpMarketId==TipConstants.SHARE_MARKET_ID)
								{
								 //To check already available data
					            	 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
					            	 {
					            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
					            	 }else
					            	 {
					            		 TIPDataObjects.tipShare[TIPDataObjects.tipShareIter]=new TIP_ShareVO(); 
					            		 tmpIterId = TIPDataObjects.tipShareIter;
					            		 TIPDataObjects.tipShareIter++; 
					                	TIPDataObjects.tipShare_Symbol_ArrayIndexMap.put(tmpSym, tmpIterId);
					            	 }
									
									TIPDataObjects.tipShare[tmpIterId].Id=tmpid;
									if(null != tmpSym)TIPDataObjects.tipShare[tmpIterId].fk_te_symbol=tmpSym;	
									TIPDataObjects.tipShare[tmpIterId].reference_price_upperlimit=tmpRpu;
									TIPDataObjects.tipShare[tmpIterId].reference_price_lowerlimit=tmpRpl;
									TIPDataObjects.tipShare[tmpIterId].isShareRI=true;									
									
									TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
									
									//Added for Prevent Historic Data transfer for inactive companies
									if(null != tmpListingDate) 
									{
										try {
											TIPDataObjects.tipShare[tmpIterId].listingDate = new Date(TipConstants.dateFormatter.parse(tmpListingDate).getTime());
										} catch (ParseException e) {
											logger.error("Error Occured in parseBasicDataRight" +e.getLocalizedMessage());
										}
									}
									
				                    try
				                    { 
				                        // insert any new share symbol to database
				                    if ((LoadInitialData.hmShareSymbols != null) && (LoadInitialData.hmShareSymbols.get(tmpSym) == null)) {
				                    	LoadInitialData.hmShareSymbols.put(tmpSym, tmpSym);
				                    	//Todo				                    	
				                    	 dbUtilObj.insertSymbolData(tmpSym,TipConstants.SHARE);					                      
					                     dbUtilObj.insertShareRIData(TIPDataObjects.tipShare[tmpIterId]);
					                    //    dbUtilObj.commitTrans();
				                    }else  //Update only LIsting_Date
				                    {
				                    	dbUtilObj.updateRIShareDataListingDate(TIPDataObjects.tipShare[tmpIterId]);
				                    }
				                    
				                    }catch(Exception e)
				                    {
				                    	 logger.info("Error :{}", e.getMessage());
				                    	e.printStackTrace();
				                    	
				                    }
									
								}else if(tmpMarketId==TipConstants.SME_MARKET_ID)
								{
								//logger.info("TIPDataObjects.tipSMEShare[indexOfShare]  matched:{}: {} Index :{}", tmpMarketId, TipConstants.SME_MARKET_ID, indexOfShareSME);
									//To check already available data
					            	 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
					            	 {
					            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
					            	 }else
					            	 {
					            		 TIPDataObjects.tipSMEShare[TIPDataObjects.tipShareSMEIter]=new TIP_SMEShareVO();
					            		 tmpIterId = TIPDataObjects.tipShareSMEIter;
					            		 TIPDataObjects.tipShareSMEIter++; 
					                	TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.put(tmpSym, tmpIterId);
					            	 }
							
								TIPDataObjects.tipSMEShare[tmpIterId].Id=tmpid;
								if(null != tmpSym) TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol=tmpSym;		
								TIPDataObjects.tipSMEShare[tmpIterId].reference_price_upperlimit=tmpRpu;
								TIPDataObjects.tipSMEShare[tmpIterId].reference_price_lowerlimit=tmpRpl;
								TIPDataObjects.tipSMEShare[tmpIterId].isShareRI=true;
								
								TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no;
								
								//Added for Prevent Historic Data transfer for inactive companies
								if(null != tmpListingDate) 
								{
									try {
										TIPDataObjects.tipShare[tmpIterId].listingDate = new Date(TipConstants.dateFormatter.parse(tmpListingDate).getTime());
									} catch (ParseException e) {
										logger.error("Error Occured in parseBasicDataRight" +e.getLocalizedMessage());
									}
								}
								
						
			                    try
			                    {
			                        // insert any new share symbol to database
			                    if ((LoadInitialData.hmSMEShareSymbols != null) && (LoadInitialData.hmSMEShareSymbols.get(tmpSym) == null)) {
			                    	LoadInitialData.hmSMEShareSymbols.put(tmpSym, tmpSym);
			                      
			                    	//Todo		
			                    	 dbUtilObj.insertSymbolData(tmpSym,TipConstants.SME);				                   
				                     dbUtilObj.insertSMEShareRIData(TIPDataObjects.tipSMEShare[tmpIterId]);
				                     //dbUtilObj.commitTrans();
			                    }else  //Update only LIsting_Date
			                    {
			                    	dbUtilObj.updateRISMEShareDataListingDate(TIPDataObjects.tipSMEShare[tmpIterId]);
			                    }
			                    
			                    }catch(Exception e)
			                    {
			                    	
			                    	
			                    }
							}
				}
				
				//Parse for INs messages
				private void parseIndexSummary(String[] messageValue){
					//finished = false;
					tmpid=0;									
					tmpSymbolId=null;
					tmpIterId=0;
				    tmpDateLong=0;
					tmpCAv=null; tmpVi=null; tmpVh=null; tmpVl=null; tmpDd=null;tmpDDn=null;
					sequence_no = 0;
					double openPrice_temp;
                	double indexPrice_temp;
					
					for (int i=1;i< messageValue.length ; i++) {
						value = Utility.splitTagValue(messageValue[i]);
						if (value != null && value.length > 1) {
							if (value[0] != null && value[1] != null) {
								messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
							}
							else {
								break;
							}
							
										switch(IndexSummary.getMessageValue(messageColumnType)){				
											case 1 : // IDCODE (1,"i")
												 tmpid=Integer.parseInt(value[1]);											
												break;
											
											case 2 : // SOURCEID (2,"Si")												
												tmpSymbolId=value[1];											
												break;
											
											case 3 : // Date (3,"Dt"),												
												tmpDateLong=  Long.parseLong(value[1]);												
												break;
											
											case 4 :// TIME_EXEC (4,"Tx"),
																								
												break;
											
											case 5 : // CURRENTADJUSTEDVALUE (5,"CAv"),
												tmpCAv = 	value[1];		
											//	logger.info(" INs Message tmpCAv :{}", tmpCAv);
												break;	
												
											case 6 :// INITIALVALUE (4,"Vi"),
												tmpVi = value[1];											
												 finished =true;
												break;
												
											case 7 :// HIGHVALUE (4,"Vh"),
												tmpVh=value[1];										
												break;
												
											case 8 :// LOWVALUE (4,"Vl"),
												tmpVl=value[1];										
												break;
												
											case 9 :// DIFFDAYPER (4,"Dd"),
												tmpDd=value[1];									
												break;
												
												
											case 10 :// DIFFNETCHANGE (4,"DDn"),
												tmpDDn=value[1];											
												break;
											
											case 11 :// SEQUENCENO(11,"n");
												sequence_no=Long.parseLong(value[1]);	
												TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
												break;
												
											default:
												break;
										}	//End of Switch
						}
					}//End of For loop

					setPrevious_close_price = false;
					 if (TipConstants.TARGIN_FEED_REQ_DATE != tmpDateLong)
                         setPrevious_close_price = true;
										
				
					 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getIndexMessageCategoty(tmpSymbolId);
					 
					switch(OBJ_TYPE)
					{
						case BONDINDEX:
							//Find Index
							 if(TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
			            	 {
			            		 tmpIterId = (int)TIPDataObjects.tipBondIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
			            	 }							
							 if(null != tmpCAv)
							 {
								 indexPrice_temp = Math.round(Double.parseDouble(tmpCAv) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;	;
								 								 
								 if(indexPrice_temp > 0.0)
		                        	{
		                        	//tipBondIndex[tmpIter2].index_price = Math.round(Double.parseDouble(curMsg.values[tmpIter1]) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
									 TIPDataObjects.tipBondIndex[tmpIterId].index_price = indexPrice_temp;
		                        	
		                        	}else
		                        	{
		                        		TIPDataObjects.tipBondIndex[tmpIterId].index_price = TIPDataObjects.tipBondIndex[tmpIterId].open_price;
		                        	}
							 }
								 
								// TIPDataObjects.tipBondIndex[tmpIterId].index_price=Math.round(Double.parseDouble(tmpCAv) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;	;
							 TIPDataObjects.tipBondIndex[tmpIterId].sequence_no = sequence_no;
							 if (setPrevious_close_price) {
								 TIPDataObjects.tipBondIndex[tmpIterId].previous_index_price=TIPDataObjects.tipBondIndex[tmpIterId].index_price;
								 TIPDataObjects.tipBondIndex[tmpIterId].open_price=TIPDataObjects.tipBondIndex[tmpIterId].index_price;
			                    	
			                    }else
			                    {
			                    	if( null != tmpVi)
			                    	{
			                    		openPrice_temp=Math.round(Double.parseDouble(tmpVi) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    		if( openPrice_temp > 0.0)
			                    			TIPDataObjects.tipBondIndex[tmpIterId].open_price=Math.round(Double.parseDouble(tmpVi) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	}
			                    	if( null != tmpVh) TIPDataObjects.tipBondIndex[tmpIterId].high_price=Math.round(Double.parseDouble(tmpVh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpVl) TIPDataObjects.tipBondIndex[tmpIterId].low_price=Math.round(Double.parseDouble(tmpVl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpDd) TIPDataObjects.tipBondIndex[tmpIterId].percent_change=Math.round(Double.parseDouble(tmpDd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpDDn) TIPDataObjects.tipBondIndex[tmpIterId].net_change=Math.round(Double.parseDouble(tmpDDn) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	 if (! TIPDataObjects.tipBondIndex[tmpIterId].isUpdated) {
			                    		 TIPDataObjects.tipBondIndex[tmpIterId].isUpdated = true;
			                    		 TIPDataObjects.totBondIndexUpdated.incrementAndGet();
				                        }
			                    	 
			                    	 TIP_BondIndexVO indexToQ = new TIP_BondIndexVO(TIPDataObjects.tipBondIndex[tmpIterId]);
  			                         MessageDatabaseQueue.tipBondIndexIntraQ.setFrame(indexToQ);
			                    }
							 
							
							// logger.info("INS Bond TIPDataObjects.tipBondIndex[tmpIterId] : Index {} Symbol: {} ID: {} Index Price :{} High Price :{} Low Price :{} Percent Change :{} Net Change :{}", tmpIterId, TIPDataObjects.tipBondIndex[tmpIterId].Fk_te_symbol, TIPDataObjects.tipBondIndex[tmpIterId].ListId, TIPDataObjects.tipBondIndex[tmpIterId].index_price, TIPDataObjects.tipBondIndex[tmpIterId].high_price, TIPDataObjects.tipBondIndex[tmpIterId].low_price, TIPDataObjects.tipBondIndex[tmpIterId].percent_change, TIPDataObjects.tipBondIndex[tmpIterId].net_change);
						break;
							
						case SHAREINDEX:
							//Find Index
							 if(TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
			            	 {
			            		 tmpIterId = (int)TIPDataObjects.tipIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
			            	 }							
							 if(null != tmpCAv) 
								 {
								 	TIPDataObjects.tipIndex[tmpIterId].index_price=Math.round(Double.parseDouble(tmpCAv) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;	;
								 }
						   
						//	 logger.info("TIPDataObjects.tipIndex[tmpIterId].index_price={}", TIPDataObjects.tipIndex[tmpIterId].Fk_te_symbol);
						//	 logger.info("TIPDataObjects.tipIndex[tmpIterId].index_price={}", TIPDataObjects.tipIndex[tmpIterId].index_price);
							 
							 TIPDataObjects.tipIndex[tmpIterId].sequence_no = sequence_no;
							 
							 if (setPrevious_close_price) {
								 TIPDataObjects.tipIndex[tmpIterId].previous_index_price=TIPDataObjects.tipIndex[tmpIterId].index_price;
								// AS open price should be zero during pre-opening price								 
								// if( null != tmpVi) TIPDataObjects.tipIndex[tmpIterId].open_price_Tmp=Math.round(Double.parseDouble(tmpVi) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;;
			                    				                    	
			                    }else
			                    {
			                    	//This is updated for Taking first index value for Index Open Price
			                    	//if( null != tmpVi)TIPDataObjects.tipIndex[tmpIterId].open_price=Math.round(Double.parseDouble(tmpVi) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpVh) TIPDataObjects.tipIndex[tmpIterId].high_price=Math.round(Double.parseDouble(tmpVh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpVl) TIPDataObjects.tipIndex[tmpIterId].low_price=Math.round(Double.parseDouble(tmpVl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpDd) TIPDataObjects.tipIndex[tmpIterId].percent_change=Math.round(Double.parseDouble(tmpDd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpDDn) TIPDataObjects.tipIndex[tmpIterId].net_change=Math.round(Double.parseDouble(tmpDDn) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	 if (! TIPDataObjects.tipIndex[tmpIterId].isUpdated) {
			                    		 TIPDataObjects.tipIndex[tmpIterId].isUpdated = true;
			                    		 TIPDataObjects.totIndexUpdated.incrementAndGet();
				                        }
			                    	 
			                    	  TIP_IndexVO indexToQ = new TIP_IndexVO(TIPDataObjects.tipIndex[tmpIterId]);
				                      // tipIndexIntraQ.setFrame(indexToQ);
				                      // MessageDatabaseQueue.this.tipIndexIntraQ.setFrame(indexToQ);
				                       MessageDatabaseQueue.tipIndexIntraQ.setFrame(indexToQ);
			                    	 
			                    }
						   
						 // logger.info("INS TIPDataObjects.tipIndex[tmpIterId] : Index {} Symbol: {} ID: {} Index Price :{} High Price :{} Low Price :{} Percent Change :{} Net Change :{}", tmpIterId, TIPDataObjects.tipIndex[tmpIterId].Fk_te_symbol, TIPDataObjects.tipIndex[tmpIterId].ListId, TIPDataObjects.tipIndex[tmpIterId].index_price, TIPDataObjects.tipIndex[tmpIterId].high_price, TIPDataObjects.tipIndex[tmpIterId].low_price, TIPDataObjects.tipIndex[tmpIterId].percent_change, TIPDataObjects.tipIndex[tmpIterId].net_change);
						    
						break;
						
						case SMESHAEINDEX:
							//Find Index
							 if(TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
			            	 {
			            		 tmpIterId = (int)TIPDataObjects.tipSMEIndex_Symbol_ArrayIndexMap.get(tmpSymbolId);
			            	 }							
							 if(null != tmpCAv) TIPDataObjects.tipSMEIndex[tmpIterId].index_price=Math.round(Double.parseDouble(tmpCAv) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;	;
							 TIPDataObjects.tipSMEIndex[tmpIterId].sequence_no = sequence_no;
							 if (setPrevious_close_price) {
								 TIPDataObjects.tipSMEIndex[tmpIterId].previous_index_price=TIPDataObjects.tipSMEIndex[tmpIterId].index_price;
			                    	
			                    }else
			                    {
			                    	//This is updated for Taking first index value for Index Open Price
			                    	//if( null != tmpVi)TIPDataObjects.tipSMEIndex[tmpIterId].open_price=Math.round(Double.parseDouble(tmpVi) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpVh) TIPDataObjects.tipSMEIndex[tmpIterId].high_price=Math.round(Double.parseDouble(tmpVh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpVl) TIPDataObjects.tipSMEIndex[tmpIterId].low_price=Math.round(Double.parseDouble(tmpVl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpDd) TIPDataObjects.tipSMEIndex[tmpIterId].percent_change=Math.round(Double.parseDouble(tmpDd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	if( null != tmpDDn) TIPDataObjects.tipSMEIndex[tmpIterId].net_change=Math.round(Double.parseDouble(tmpDDn) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
			                    	 if (! TIPDataObjects.tipSMEIndex[tmpIterId].isUpdated) {
			                    		 TIPDataObjects.tipSMEIndex[tmpIterId].isUpdated = true;
			                    		 TIPDataObjects.totSMEIndexUpdated.incrementAndGet();
				                        }
			                    	 TIP_SMEIndexVO indexToQ = new TIP_SMEIndexVO(TIPDataObjects.tipSMEIndex[tmpIterId]);
  			                         MessageDatabaseQueue.tipSMEIndexIntraQ.setFrame(indexToQ);			                    	 
			                    }
							
							
							// logger.info("INS SME TIPDataObjects.tipSMEIndex[tmpIterId] : Index {} Symbol: {} ID: {} Index Price :{} High Price :{} Low Price :{} Percent Change :{} Net Change :{}", tmpIndexIter, TIPDataObjects.tipSMEIndex[tmpIterId].Fk_te_symbol, TIPDataObjects.tipSMEIndex[tmpIterId].ListId, TIPDataObjects.tipSMEIndex[tmpIterId].index_price, TIPDataObjects.tipSMEIndex[tmpIterId].high_price, TIPDataObjects.tipSMEIndex[tmpIterId].low_price, TIPDataObjects.tipSMEIndex[tmpIterId].percent_change, TIPDataObjects.tipSMEIndex[tmpIterId].net_change);
							 
						break;
					
						default:
							break;
					}					 
					 
				}
				
				
				//Parse for OBRf messages
				private void parseOrderBookReference(String[] messageValue){
					finished = false;
					tmpid=0;									
					tmpSymbolId="";
				    tmpIterId=0;
				    tmpRPu=null;tmpRPl=null; tmpRPr=null;
				    sequence_no = 0;
					
					for (int i=1;i< messageValue.length && !finished; i++) {
						value = Utility.splitTagValue(messageValue[i]);
						if (value != null && value.length > 1) {
							if (value[0] != null && value[1] != null) {
								messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
							}
							else {
								break;
							}
							
						
										switch(OrderBookReferencePrice.getMessageValue(messageColumnType)){				
											case 1 : // IDCODE (1,"i")
												 tmpid=Integer.parseInt(value[1]);	
												break;
											
											case 2 : // SOURCEID (2,"Si")												
												tmpSymbolId=value[1];	
												break;
											
											case 3 : // REFERENCEPRICE (3,"RPr"),													
												tmpRPr=value[1];	
												break;
											
											case 4 :// REFERENCEPRICELOWERLIMIT (4,"RPl"),
												tmpRPl=value[1];														 
												  finished =true;
												break;
											
											case 5 : // REFERENCEPRICEUPPERLIMIT (5,"RPu"),
												tmpRPu=value[1];												
												break;	
												
											case 6 : // SEQUENCENO(6,"n");
												sequence_no = Long.parseLong(value[1]);		
												TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
												break;	
												
											default:
												break;
										}	//End of Switch
						}
					}//End of For loop		
					
					 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);
					 switch(OBJ_TYPE)
						{
							case SHARE:
								//Find Index
								 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }
								
									 if( null != tmpRPu)TIPDataObjects.tipShare[tmpIterId].reference_price_upperlimit=Integer.parseInt(tmpRPu);
									 if( null != tmpRPl) TIPDataObjects.tipShare[tmpIterId].reference_price_lowerlimit=Integer.parseInt(tmpRPl);
									 if( null != tmpRPr) TIPDataObjects.tipShare[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpRPr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;					
									 if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
										 TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
										 TIPDataObjects.totShareUpdated.incrementAndGet();
									 }
									
									 TIPDataObjects.tipShare[tmpIterId].sequence_no  = sequence_no ;
									 
							break;
								
							case SMESHARE:
								//Find Index
								 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }							
								 if( null != tmpRPu) TIPDataObjects.tipSMEShare[tmpIterId].reference_price_upperlimit=Integer.parseInt(tmpRPu);
								 if( null != tmpRPl) TIPDataObjects.tipSMEShare[tmpIterId].reference_price_lowerlimit=Integer.parseInt(tmpRPl);
								 if( null != tmpRPr) TIPDataObjects.tipSMEShare[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpRPr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	;					
								 if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
									 TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
									 TIPDataObjects.totSMEShareUpdated.incrementAndGet();
								 }
								 
								 TIPDataObjects.tipSMEShare[tmpIterId].sequence_no  = sequence_no ;
							break;
							
							case BOND:
								//Find Index
								 if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }							
								 if( null != tmpRPr) TIPDataObjects.tipBond[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpRPr) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;		
								
								 if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
									 TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
									 TIPDataObjects.totBondUpdated.incrementAndGet();
								 }
								 
								 TIPDataObjects.tipBond[tmpIterId].sequence_no  = sequence_no ;
							break;
							
							case OTCBOND:
								//Find Index
								 if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }							
								 if( null != tmpRPr) TIPDataObjects.tipOTCBond[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpRPr) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;		
								
								 if (! TIPDataObjects.tipOTCBond[tmpIterId].isUpdated) {
									 TIPDataObjects.tipOTCBond[tmpIterId].isUpdated = true;
									 TIPDataObjects.totOTCBondUpdated.incrementAndGet();
								 }
								 
								 TIPDataObjects.tipOTCBond[tmpIterId].sequence_no  = sequence_no ;
							break;
							
							default:
								break;
						}		
				}
				
				/*//Parse for OBs messages
				private void parseOrderBookSummary(String[] messageValue){
					finished = false;
					tmpid=0;									
					String tmpSymbolId="",tmpLPd=null,tmpTime=null;
					tmpOSt=null;
					tmpIterId=0;
					long tmpDateLong=0;
					tmpLTv=null;
					tmpACp=null; tmpPl=null; tmpPf=null; tmpMCa=null; tmpPOf=null;tmpPh=null;tmpLOp=null;tmpHPw=null;tmpLPw=null;
					setPrevious_close_price = false;
					sequence_no = 0;						
				
					for (int i=1;i< messageValue.length && !finished; i++) {
						value = Utility.splitTagValue(messageValue[i]);
						if (value != null && value.length > 1) {
							if (value[0] != null && value[1] != null) {
								messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
							}
							else {
								break;
							}
								
										switch(OrderBookSummary.getMessageValue(messageColumnType)){				
											case 1 : // IDCODE (1,"i")
												 tmpid=Integer.parseInt(value[1]);												
												break;
											
											case 2 : // SOURCEID (2,"Si")
												tmpSymbolId=value[1];												
												break;
											
											case 3 : // Date (3,"Dt"),
												tmpDateLong=  Long.parseLong(value[1]);												
												break;
											
											case 4 :// EXECUTIONDATETIME (4,"Tx"),
												tmpTime =value[1];								
												break;
											
											case 5 : // ORDERBOOKSTATECODE (5,"OSt"),
												tmpOSt=value[1];									 
												break;	
												
											case 6 :// ADJUSTEDCLOSINGPRICE (6,"ACp"),
												tmpACp = value[1];													
												break;
												
											case 7 :// LASTPRICE (7,"Pl"),
												tmpPl=value[1];												  
												break;
												
											case 8 :// FIRSTPRICE (8,"Pf"),
												tmpPf=value[1];	
												break;
												
											case 9 :// MARKETCAPITAL (9,"MCa"),
												tmpMCa=value[1];													 
												break;
												
											case 10 :// LASTPAIDDATE (10,"LPd"),
												tmpLPd=value[1];												 
												break;
												
											case 11 :// OFFICIALCLOSINGPRICE (11,"POf"),
												tmpPOf=value[1];													 
												break;
												
											case 12 :// LASTTRADEVOLUME (12,"LTv"),
												tmpLTv=value[1];												
												break;
												
											case 13 :// HIGHPRICE (13,"Ph"),
												tmpPh=value[1];													
												break;
												
											case 14 :// LOWPRICE (14,"LOp"),
												tmpLOp=value[1];													 
												break;
											case 15 :// HIGHPRICE52WEEK (15,"HPw"),
												tmpHPw=value[1];													 
												break;
											case 16 ://LOWPRICE52WEEK (16,"LPw")
												tmpLPw=value[1];													 
												break;
											case 17 ://SEQUENCENO(17,"n");
												sequence_no=Long.parseLong(value[1]);	
												TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
												break;
											
											default:
												break;
										}	//End of Switch
						}
					}//End of For loop			
					
					 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);
					 if (TipConstants.TARGIN_FEED_REQ_DATE != tmpDateLong)
					 {
                         setPrevious_close_price = true;                        
					 }else
					 {
						 if(isFirstOBsInEvening)
                         {//To fix the OBS wait for TRD and TRD to complete
							 logger.info("Sleep wait for OBS Thread :{}", TipConstants.SLEEPWAIT_FOR_OBS_BEFORE_PROCESSING);
                        	 try {                        		 
								Thread.sleep(TipConstants.SLEEPWAIT_FOR_OBS_BEFORE_PROCESSING);
							} catch (InterruptedException e) {
								logger.error("Error occured in OBS processing :"+e.getMessage());
							}
                         }
                         isFirstOBsInEvening=false;
					 }
					 
					 switch(OBJ_TYPE)
						{
							case SHARE:
								//Find Index
								 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }
								 	if( null != tmpOSt) TIPDataObjects.tipShare[tmpIterId].market_status=TIPCommonFunctions.setState(TIPDataObjects.tipShare[tmpIterId].market_status, Integer.parseInt(tmpOSt));
								 	 if(null!=tmpTime)
								 	 TIPDataObjects.tipShare[tmpIterId].last_update_time=Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
								 	 
								 	 if(null !=tmpPOf) TIPDataObjects.tipShare[tmpIterId].todays_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
								 	 
								 	if (setPrevious_close_price)
								 	 {
								 		if(null != tmpMCa)TIPDataObjects.tipShare[tmpIterId].market_cap=Math.round(Double.parseDouble(tmpMCa) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 		if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
								 		if(tmpLPd!=null)
								 			TIPDataObjects.tipShare[tmpIterId].transaction_date=Timestamp.valueOf(Utility.formatDate(tmpLPd)+" 00:00:00");
								 	 }								 		 
								 	 else
								 	 {
								 		
								 		if(null != tmpPl)TIPDataObjects.tipShare[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 		if(null != tmpLTv)TIPDataObjects.tipShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
								 		
								 		if(null != tmpPf)TIPDataObjects.tipShare[tmpIterId].Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 		//if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].last_trade_price = tmpPl;
								 		
								 		if(null != tmpPh)TIPDataObjects.tipShare[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;;
								 		if(null != tmpLOp)TIPDataObjects.tipShare[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	;
								 		if(TIPDataObjects.tipShare[tmpIterId].no_of_trades >0)
								 		{
								 			 if (logger.isDebugEnabled()) logger.debug("Before setting Normal Trade as Last Trade - Symbol: {} last_trade_price: {} last_trade_quantity: {} transaction_date: {}", TIPDataObjects.tipShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipShare[tmpIterId].last_trade_price, TIPDataObjects.tipShare[tmpIterId].last_trade_quantity, TIPDataObjects.tipShare[tmpIterId].transaction_date);
								 			if(null != tmpACp)TIPDataObjects.tipShare[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 			if(null != tmpLTv) TIPDataObjects.tipShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
				                               TIPDataObjects.tipShare[tmpIterId].transaction_date = TIPDataObjects.tipShare[tmpIterId].lastNormalTradeTime;
				                                if (logger.isDebugEnabled()) logger.debug("After setting Normal Trade as Last Trade - Symbol: {} last_trade_price: {} last_trade_quantity: {} transaction_date: {}", TIPDataObjects.tipShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipShare[tmpIterId].last_trade_price, TIPDataObjects.tipShare[tmpIterId].last_trade_quantity, TIPDataObjects.tipShare[tmpIterId].transaction_date);
								 		}
								 		
								 		if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
								 			TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
								 			TIPDataObjects.totShareUpdated.incrementAndGet();
			                            }
								 		
								 		//Commented to take from STa
								 		 if (TIPDataObjects.tipShare[tmpIterId].fk_te_symbol.equals(TipConstants.SHARE_MARKET_SYMBOL_REFERENCE)) {
				                            	
								 			TIPDataObjects.isShareEOD = true;
				                            }								 		
								 	 }								 
								 
								 	TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
							break;
								
							case SMESHARE:
								//Find Index
								 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }
								 if( null != tmpOSt) TIPDataObjects.tipSMEShare[tmpIterId].market_status=TIPCommonFunctions.setState(TIPDataObjects.tipSMEShare[tmpIterId].market_status, Integer.parseInt(tmpOSt));
							 	 if(null!=tmpTime)
							 	 TIPDataObjects.tipSMEShare[tmpIterId].last_update_time=Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
							 	 
							 	 if(null !=tmpPOf) TIPDataObjects.tipSMEShare[tmpIterId].todays_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
							
								 	if (setPrevious_close_price)
								 	 {
								 		if(null != tmpMCa)TIPDataObjects.tipSMEShare[tmpIterId].market_cap=Math.round(Double.parseDouble(tmpMCa) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 		if(null != tmpPOf)TIPDataObjects.tipSMEShare[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
								if(tmpLPd!=null)
								 			TIPDataObjects.tipSMEShare[tmpIterId].transaction_date=Timestamp.valueOf(Utility.formatDate(tmpLPd)+" 00:00:00");;
								 	 }								 		 
								 	 else
								 	 {
								 		if(null != tmpPl)TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
								 		if(null != tmpPf)TIPDataObjects.tipSMEShare[tmpIterId].Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 		//if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].last_trade_price = tmpPl;
								 		if(null != tmpLTv)TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
								 		if(null != tmpPh)TIPDataObjects.tipSMEShare[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;;
								 		if(null != tmpLOp)TIPDataObjects.tipSMEShare[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	;
							if(TIPDataObjects.tipSMEShare[tmpIterId].no_of_trades >0)
								 		{
								 			 if (logger.isDebugEnabled()) logger.debug("Before setting Normal Trade as Last Trade - Symbol: {} last_trade_price: {} last_trade_quantity: {} transaction_date: {}", TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price, TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity, TIPDataObjects.tipSMEShare[tmpIterId].transaction_date);
								 			if(null != tmpACp)TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 			if(null != tmpLTv) TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
				                            TIPDataObjects.tipSMEShare[tmpIterId].transaction_date = TIPDataObjects.tipSMEShare[tmpIterId].lastNormalTradeTime;
				                                if (logger.isDebugEnabled()) logger.debug("After setting Normal Trade as Last Trade - Symbol: {} last_trade_price: {} last_trade_quantity: {} transaction_date: {}", TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price, TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity, TIPDataObjects.tipSMEShare[tmpIterId].transaction_date);
								 		}		
							
											if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
									 			TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
									 			TIPDataObjects.totSMEShareUpdated.incrementAndGet();
				                            }
							
											//Commented to take from STa
								 		 if (TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol.equals(TipConstants.SME_MARKET_SYMBOL_REFERENCE)) {
				                            	
								 			TIPDataObjects.isSMEShareEOD = true;
				                            }							 		
								 	 }	
								
								 	TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no;
							break;
							
							case BOND:
								//Find Index
								 if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }							
								 if( null != tmpOSt) TIPDataObjects.tipBond[tmpIterId].market_status=TIPCommonFunctions.setState(TIPDataObjects.tipBond[tmpIterId].market_status, Integer.parseInt(tmpOSt));
									 if(null!=tmpTime)
							 	 TIPDataObjects.tipBond[tmpIterId].last_update_time=Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
								if(null !=tmpPOf) TIPDataObjects.tipBond[tmpIterId].todays_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
										
								if(null !=tmpHPw)TIPDataObjects.tipBond[tmpIterId].high_52_weeks_price=Math.round(Double.parseDouble(tmpHPw) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;;
								if(null !=tmpLPw)TIPDataObjects.tipBond[tmpIterId].low_52_weeks_price=Math.round(Double.parseDouble(tmpLPw) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;;
								 	if(LoadInitialData.hmBondHighLowPriceDtMap != null){
	                                    bondHighLowPriceDtVO = (TIP_GetHighLowPriceVO)LoadInitialData.hmBondHighLowPriceDtMap.get(TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol);
	                                    if(bondHighLowPriceDtVO != null){
	                                        if(TIPDataObjects.tipBond[tmpIterId].high_52_weeks_price != bondHighLowPriceDtVO.high_52_weeks_price){
	                                            logger.error("High52WeeksPrice is not matching for Bond Symbol "+ TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol
	                                                    + " Current: " + bondHighLowPriceDtVO.high_52_weeks_price + " Incoming: " + TIPDataObjects.tipBond[tmpIterId].high_52_weeks_price);
	                                        }
	                                        TIPDataObjects.tipBond[tmpIterId].high_52_weeks_date = bondHighLowPriceDtVO.high_52_weeks_date;
	                                       
	                                        if(TIPDataObjects.tipBond[tmpIterId].low_52_weeks_price != bondHighLowPriceDtVO.low_52_weeks_price){
                                                logger.error("Low52WeeksPrice is not matching for Bond Symbol "+ TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol
                                                        + " Current: " + bondHighLowPriceDtVO.low_52_weeks_price + " Incoming: " + TIPDataObjects.tipBond[tmpIterId].low_52_weeks_price);
                                            }
	                                        TIPDataObjects.tipBond[tmpIterId].low_52_weeks_date = bondHighLowPriceDtVO.low_52_weeks_date;
	                                    }
	                                }
							 	 
							 	if (setPrevious_close_price)
							 	 {
							 		//TIPDataObjects.tipBond[tmpIterId].market_cap=tmpMCa;
							 		 if(null !=tmpPOf) TIPDataObjects.tipBond[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
							 		
							 		if(tmpLPd!=null)
							 			TIPDataObjects.tipBond[tmpIterId].transaction_date=Timestamp.valueOf(Utility.formatDate(tmpLPd)+" 00:00:00");;
							 	 }								 		 
							 	 else
							 	 {
							 		if(TIPDataObjects.tipBond[tmpIterId].no_of_trades >0)
							 		{
							 		if(null != tmpPl)TIPDataObjects.tipBond[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
							 		if(null != tmpLTv)TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
							 		}
							 		if(null != tmpPf)TIPDataObjects.tipBond[tmpIterId].Todays_open_price = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
							 		//if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].last_trade_price = tmpPl;
							 		if(null != tmpLTv)TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
							 		if(null != tmpPh)TIPDataObjects.tipBond[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;;
							 		if(null != tmpLOp)TIPDataObjects.tipBond[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	;
			
							 		if(TIPDataObjects.tipBond[tmpIterId].no_of_trades >0)
							 		{
							 			 if (logger.isDebugEnabled()) logger.debug("Before setting Normal Trade as Last Trade - Symbol: {} last_trade_price: {} last_trade_quantity: {} transaction_date: {}", TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipBond[tmpIterId].last_trade_price, TIPDataObjects.tipBond[tmpIterId].last_trade_quantity, TIPDataObjects.tipBond[tmpIterId].transaction_date);
								 			if(null != tmpACp)TIPDataObjects.tipBond[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
								 			if(null != tmpLTv) TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
								 			if(null != tmpACp)TIPDataObjects.tipBond[tmpIterId].todays_close_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
										                               
			                                if (logger.isDebugEnabled()) logger.debug("After setting Normal Trade as Last Trade - Symbol: {} last_trade_price: {} last_trade_quantity: {} transaction_date: {}", TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipBond[tmpIterId].last_trade_price, TIPDataObjects.tipBond[tmpIterId].last_trade_quantity, TIPDataObjects.tipBond[tmpIterId].transaction_date);
							 		}			
							 		
							 		if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
							 			TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
							 			TIPDataObjects.totBondUpdated.incrementAndGet();
		                            }
							 		//ToDo
							 		 if (TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol.equals(TipConstants.SME_MARKET_SYMBOL_REFERENCE)) {
			                            	
			                                isSMEShareEOD = true;
			                            }							 		
							 	 }		
							 	
							 	TIPDataObjects.tipBond[tmpIterId].sequence_no = sequence_no;
							break;
					
							default:
								break;
						}		
				}*/
				
				
				//Parse for TRh messages
				private void parseTradeHalts(String[] messageValue){
					 finished = false;
					 tmpid=0;									
					tmpSymbolId="";
					tmpIterId=0;
					String tmpTx=null;
					int tmpOBt=0,tmpSRe=0;
					sequence_no = 0;
						
					for (int i=1;i< messageValue.length && !finished; i++) {
						value = Utility.splitTagValue(messageValue[i]);
						if (value != null && value.length > 1) {
							if (value[0] != null && value[1] != null) {
								messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
							}
							else {
								break;
							}
							
						
										switch(TradeHalt.getMessageValue(messageColumnType)){				
											case 1 : // IDCODE (1,"i")
												 tmpid=Integer.parseInt(value[1]);
												break;
											
											case 2 : // SOURCEID (2,"Si")
												tmpSymbolId=value[1];
												break;
											
											case 3 : // TIMEEXEC (3,"Tx"),
												tmpTx=value[1];
												break;
											
											case 4 :// OBJECTTYPE (4, "OBt"),
												tmpOBt=Integer.parseInt(value[1]);
												break;
											
											case 5 : // SUSPRESUME (5, "SRe"),
												tmpSRe=Integer.parseInt(value[1]);
												break;	
												
											case 6 :// StopCodes (6, "STc");,
																								
												break;
												
											case 7 :// SEQUENCENO(7,"n");
												sequence_no = Long.parseLong(value[1]);
												TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
												break;
												 
											default:
												break;
										}	//End of Switch
						}
					}//End of For loop			
					
					switch(tmpOBt)
					{
						case 4://means this msg is for market
							if (tmpid == TipConstants.SHARE_MARKET_ID) {
	                            if (tmpSRe == 1) { // Suspended
	                            	TIPDataObjects.tipMarket[0].market_status = TipConstants.TIP_DB_STATE_SUSPENDED;
	                                if (logger.isDebugEnabled()) logger.debug("Share Market Suspended, Current State: {} Current Market Status: {}", TIPDataObjects.tipMarket[0].current_state, TIPDataObjects.tipMarket[0].market_status);
	                            }
	                            if (tmpSRe == 2) { // Resumed
	                            	TIPDataObjects.tipMarket[0].market_status = TIPDataObjects.tipMarket[0].current_state;
	                                if (logger.isDebugEnabled()) logger.debug("Share Market Resumed, Current State: {} Current Market Status: {}", TIPDataObjects.tipMarket[0].current_state, TIPDataObjects.tipMarket[0].market_status);
	                            }
	                            TIPDataObjects.tipMarket[0].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 
	                            
	                            if (! TIPDataObjects.tipMarket[0].isUpdated) {
	                            	TIPDataObjects.tipMarket[0].isUpdated = true;
	                            	TIPDataObjects.totMarketUpdated.incrementAndGet();
	                            }
	                            
	                            TIPDataObjects.tipMarket[0].sequence_no = sequence_no;
	                            
							}else if(tmpid == TipConstants.SME_MARKET_ID)
							{
								 if (tmpSRe == 1) { // Suspended
		                            	TIPDataObjects.tipMarket[2].market_status = TipConstants.TIP_DB_STATE_SUSPENDED;
		                                if (logger.isDebugEnabled()) logger.debug("Share Market Suspended, Current State: {} Current Market Status: {}", TIPDataObjects.tipMarket[2].current_state, TIPDataObjects.tipMarket[2].market_status);
		                            }
		                            if (tmpSRe == 2) { // Resumed
		                            	TIPDataObjects.tipMarket[2].market_status = TIPDataObjects.tipMarket[2].current_state;
		                                if (logger.isDebugEnabled()) logger.debug("Share Market Resumed, Current State: {} Current Market Status: {}", TIPDataObjects.tipMarket[2].current_state, TIPDataObjects.tipMarket[2].market_status);
		                            }
		                            TIPDataObjects.tipMarket[2].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 
		                            
		                            if (! TIPDataObjects.tipMarket[2].isUpdated) {
		                            	TIPDataObjects.tipMarket[2].isUpdated = true;
		                            	TIPDataObjects.totMarketUpdated.incrementAndGet();
		                            }
		                            
		                            TIPDataObjects.tipMarket[2].sequence_no = sequence_no;
							}else if(tmpid == TipConstants.BOND_MARKET_ID)
							{
								 if (tmpSRe == 1) { // Suspended
		                            	TIPDataObjects.tipMarket[1].market_status = TipConstants.TIP_DB_STATE_SUSPENDED;
		                                if (logger.isDebugEnabled()) logger.debug("Share Market Suspended, Current State: {} Current Market Status: {}", TIPDataObjects.tipMarket[1].current_state, TIPDataObjects.tipMarket[1].market_status);
		                            }
		                            if (tmpSRe == 2) { // Resumed
		                            	TIPDataObjects.tipMarket[1].market_status = TIPDataObjects.tipMarket[1].current_state;
		                                if (logger.isDebugEnabled()) logger.debug("Share Market Resumed, Current State: {} Current Market Status: {}", TIPDataObjects.tipMarket[1].current_state, TIPDataObjects.tipMarket[1].market_status);
		                            }
		                            TIPDataObjects.tipMarket[1].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 
		                            
		                            if (! TIPDataObjects.tipMarket[1].isUpdated) {
		                            	TIPDataObjects.tipMarket[1].isUpdated = true;
		                            	TIPDataObjects.totMarketUpdated.incrementAndGet();
		                            }
		                            
		                            TIPDataObjects.tipMarket[1].sequence_no = sequence_no;
							}
							
						break;
						
						case 1: //means this msg is for order book (i.e. symbol)						
							TadawulObjectCategory objType= TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);
							
							switch(objType)
							{
							case SHARE:
								 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }

								 if (tmpSRe == 1) { // Suspended
									 TIPDataObjects.tipShare[tmpIterId].market_status = TipConstants.TIP_DB_STATE_SUSPENDED;		                                
		                                if (logger.isDebugEnabled()) logger.debug("Share {} Suspended, Current State: {} Current Market Status: {}", TIPDataObjects.tipShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipShare[tmpIterId].current_state, TIPDataObjects.tipShare[tmpIterId].market_status);
		                            }
		                            if (tmpSRe == 2) { // Resumed
		                            	TIPDataObjects.tipShare[tmpIterId].market_status = TIPDataObjects.tipShare[tmpIterId].current_state;
		                                if (logger.isDebugEnabled()) logger.debug("Share {} Resumed, Current State: {} Current Market Status: {}", TIPDataObjects.tipShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipShare[tmpIterId].current_state, TIPDataObjects.tipShare[tmpIterId].market_status);
		                            }		                            
		                            TIPDataObjects.tipShare[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 	    
		                            
		                            if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
		                            	TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
		                            	TIPDataObjects.totShareUpdated.incrementAndGet();
		                            }
		                            
		                            TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
								break;
								
							case SMESHARE:
								 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }
								 
								 if (tmpSRe == 1) { // Suspended
									 TIPDataObjects.tipSMEShare[tmpIterId].market_status = TipConstants.TIP_DB_STATE_SUSPENDED;		                                
		                                if (logger.isDebugEnabled()) logger.debug("Share SME {} Suspended, Current State: {} Current Market Status: {}", TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipSMEShare[tmpIterId].current_state, TIPDataObjects.tipSMEShare[tmpIterId].market_status);
		                            }
		                            if (tmpSRe == 2) { // Resumed
		                            	TIPDataObjects.tipSMEShare[tmpIterId].market_status = TIPDataObjects.tipSMEShare[tmpIterId].current_state;
		                                if (logger.isDebugEnabled()) logger.debug("Share {} Resumed, Current State: {} Current Market Status: {}", TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipSMEShare[tmpIterId].current_state, TIPDataObjects.tipSMEShare[tmpIterId].market_status);
		                            }
		                            TIPDataObjects.tipSMEShare[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 
		                            if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
		                            	TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
		                            	TIPDataObjects.totSMEShareUpdated.incrementAndGet();
		                            }
		                            
		                            TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no;
								break;
							
							case BOND:
								if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }
								 
								 if (tmpSRe == 1) { // Suspended
									 TIPDataObjects.tipBond[tmpIterId].market_status = TipConstants.TIP_DB_STATE_SUSPENDED;		                                
		                                if (logger.isDebugEnabled()) logger.debug("Bonds {} Suspended, Current State: {} Current Market Status: {}", TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipBond[tmpIterId].current_state, TIPDataObjects.tipBond[tmpIterId].market_status);
		                            }
		                            if (tmpSRe == 2) { // Resumed
		                            	TIPDataObjects.tipBond[tmpIterId].market_status = TIPDataObjects.tipBond[tmpIterId].current_state;
		                                if (logger.isDebugEnabled()) logger.debug("Share {} Resumed, Current State: {} Current Market Status: {}", TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipBond[tmpIterId].current_state, TIPDataObjects.tipBond[tmpIterId].market_status);
		                            }
		                            TIPDataObjects.tipBond[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 		
		                            if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
		                            	TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
		                            	TIPDataObjects.totBondUpdated.incrementAndGet();
		                            }
		                            
		                            TIPDataObjects.tipBond[tmpIterId].sequence_no = sequence_no;
								break;
							case OTCBOND:
								if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 }
								 
								 if (tmpSRe == 1) { // Suspended
									 TIPDataObjects.tipOTCBond[tmpIterId].market_status = TipConstants.TIP_DB_STATE_SUSPENDED;		                                
		                                if (logger.isDebugEnabled()) logger.debug("Bonds {} Suspended, Current State: {} Current Market Status: {}", TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipBond[tmpIterId].current_state, TIPDataObjects.tipBond[tmpIterId].market_status);
		                            }
		                            if (tmpSRe == 2) { // Resumed
		                            	TIPDataObjects.tipOTCBond[tmpIterId].market_status = TIPDataObjects.tipBond[tmpIterId].current_state;
		                                if (logger.isDebugEnabled()) logger.debug("Share {} Resumed, Current State: {} Current Market Status: {}", TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol, TIPDataObjects.tipBond[tmpIterId].current_state, TIPDataObjects.tipBond[tmpIterId].market_status);
		                            }
		                            TIPDataObjects.tipOTCBond[tmpIterId].last_update_time = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 		
		                            if (! TIPDataObjects.tipOTCBond[tmpIterId].isUpdated) {
		                            	TIPDataObjects.tipOTCBond[tmpIterId].isUpdated = true;
		                            	TIPDataObjects.totOTCBondUpdated.incrementAndGet();
		                            }
		                            
		                            TIPDataObjects.tipOTCBond[tmpIterId].sequence_no = sequence_no;
								break;
						
								
								
								default:
									break;
							}
							
						break;//Break for outer switch
						
					}
					
				}
				
				//Parse for EOs messages
				private void parseEndOfSessions(String[] messageValue){
					
					 logger.info("EOS Message Received.");
					 if( TIPDataObjects.replayMode)
					 {
						 logger.info("End Of Stream in replay mode...ignoring");
		                   
		                  return;
					 }else
					 {
						 logger.info("End Of Stream in Live mode...shutdown the TIP Listener");
						 
						 while (true)
						 {
							 if(!DBQueueUpdater.isDBQueueAllEmpty())
							 {
								 logger.info("End Of Stream : Pending DB Queue Messages , Quence Wait for Terminate");
								 try {
									Thread.sleep(TipConstants.SLEEPWAIT_FOR_EOS_BEFORE_SHUTDOWN);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								 
							 }else
							 {
								 logger.info("End Of Stream :  DB Queue Messages are Empty , However this is waiting for 3 minute to make to DB Updates are completed");
								 
								 try {
										Thread.sleep(TipConstants.SLEEPWAIT_FOR_EOD_BEFORE_PROCESSING_ACTUAL);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								 
								 logger.info("Eod of session : History table copy starts ");
								 DBManagerShare dbUtilObjShare=  null;
								 DBManagerSMEShare dbUtilObjSMEShare=  null;
								 DBManagerBond dbUtilObjBond=  null;
								 DBManagerIndex dbUtilObjIndex=  null;
								 DBManagerSMEIndex dbUtilObjSMEIndex=  null;
								 DBManagerBondIndex dbUtilObjBondIndex=  null;
					
								 try
								 {
									 dbUtilObjShare = new DBManagerShare("DBManagerShareEOS");
									 dbUtilObjSMEShare = new DBManagerSMEShare("DBManagerSMEShareEOS");
									 dbUtilObjIndex = new DBManagerIndex("DBManagerIndexEOS");
									 dbUtilObjSMEIndex = new DBManagerSMEIndex("DBManagerSMEIndexEOS");
									 dbUtilObjBond = new DBManagerBond("DBManagerBondEOS");
									 dbUtilObjBondIndex = new DBManagerBondIndex("DBManagerBondIndexEOS");
									 logger.info("Eod of session : History table copy starts - Intialization completed"); 
								 }catch(Exception e)
								 {
									 logger.error("Error Occured in EOS Sessions :", e);
								 }
								 dbUtilObjShare.EODforShare();
								 dbUtilObjShare.EODforShare_Adj();
								 
								 logger.info("Eod of session : History table copy Share completed ");
								
								 dbUtilObjSMEShare.EODforSMEShare();
								 dbUtilObjSMEShare.EODforSMEShare_Adj();
								 logger.info("Eod of session : History table copy SMEShare completed ");
								 								 
								 dbUtilObjIndex.EODforIndex();
								 dbUtilObjIndex.EODforIndex_Adj();
								 logger.info("Eod of session : History table copy Index completed ");
								 
								 dbUtilObjSMEIndex.EODforSMEIndex();
								 dbUtilObjSMEIndex.EODforSMEIndex_Adj();
								 logger.info("Eod of session : History table copy SME Index completed ");
								 
								 dbUtilObjBond.EODforBond();
								 dbUtilObjBondIndex.EODforBondIndex();
								 logger.info("Eod of session : History table copy Bond completed ");
								 
								 //This is needed to make sure all EOD Jobs are completed
								 try {
										Thread.sleep(TipConstants.SLEEPWAIT_FOR_EOS_BEFORE_SHUTDOWN_ACTUAL);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								 
								 logger.info("End Of Stream : No Pending DB Queue Messages , Hence Terminate");
								 TipConstants.shutdownFlag = true;
								 return;
							 }
						 }
						 
						//return;
					 }
								}


private void parseGeneralReply(String[]  messageValue){

	String replycode="";
	String requestType="";
	
	for (int i=1;i< messageValue.length ; i++) {
		value = Utility.splitTagValue(messageValue[i]);
		if (value != null && value.length > 1) {
			if (value[0] != null && value[1] != null) {
				messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
			}
			else {
				break;
			}
			
							switch(GeneralReply.getMessageValue(messageColumnType)){				
							case 1 :  //ReplyCode (1, "Rc")
								replycode = value[1];								 
								break;
							case 2 : //RequestType (2, "Qt"),
								requestType= value[1];
								break;
							case 3 : // ReplyText (3, "Rx");
								//replyText = value[1];								
								break;
							
								
							default:
								break;
						}	//End of Switch
		}
	}//End of For loop		
		
		 logger.info("General Reply received .....");
		// logger.info("General Reply ")
		 if( TIPDataObjects.replayMode)
		 {
			 logger.info("General Reply received in replay mode...ignoring");
               
              return;
		 }else
		 {
			 if(replycode==null )
			 {
				 logger.info("General Reply received is NULL...shutdown the TIP Listener");
				 TipConstants.shutdownFlag = true;
			 }else if ( TipConstants.TIP_MSGTYPE_LOGON.equals(requestType) && Integer.parseInt(replycode) == 0 )
			 {
				  logger.info("Logon Error. Returning true for {}. Hence shutdown the Listener", TipConstants.TIP_MSGTYPE_GENERALREPLY);
				  TipConstants.shutdownFlag = true;
			 }else if ( TipConstants.TIP_MSGTYPE_REALTIME.equals(requestType) && Integer.parseInt(replycode) == 13 )
			 {
				  logger.info("Real Time data is not found. Hence shutdown the Listener {}", TipConstants.TIP_MSGTYPE_REALTIME);
				  TipConstants.shutdownFlag = true;
			 }
			 
			 return;
		 }
	
}


private void parseMarketStates(String[]  messageValue){

	tmpid=0;		
	tmpSymbolId="";
	String tmpTx="";
	int tmpMSt=0;
	int newState=0;
	
	for (int i=1;i< messageValue.length ; i++) {
		value = Utility.splitTagValue(messageValue[i]);
		if (value != null && value.length > 1) {
			if (value[0] != null && value[1] != null) {
				messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
			}
			else {
				break;
			}
			
							switch(MarketState.getMessageValue(messageColumnType)){				
							case 1 :  // IDCODE (1, "i"),
								tmpid = Integer.parseInt(value[1]);								 
								break;
							case 2 : //SOURCEID (2, "Si"),	 
								tmpSymbolId=value[1];
								break;
							case 3 :  //TIMEEXEC (3, "Tx"),
								tmpTx=value[1];							 
								break;
							case 4 :  // MARKETSTATECODE (4, "MSt"),
								tmpMSt = Integer.parseInt(value[1]);								 
								break;
							case 5 :  // SEQUENCENUMBER (5, "n");
								sequence_no = Long.parseLong(value[1]);		
								TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
								break;	
							default:
								break;
						}	//End of Switch
		}
	}//End of For loop		
		

//logger.info("STa message called ");
	
	if (tmpid == TipConstants.SHARE_MARKET_ID) {
		newState = TIPCommonFunctions.setStateBySTaMessage(TIPDataObjects.tipMarket[0].current_state, tmpMSt);

		// Show market status as closed while processing new TIP messages for negotiated deals.
		java.time.LocalTime riyadhNow = Utility.riyadhTime(); // cached ZoneId, no Calendar/TimeZone allocation per message
		int hour = riyadhNow.getHour();
		int minute = riyadhNow.getMinute();

		//Changes for SR2 Extented Negdeals
		// Check if time is within 4:00 PM to 9:30 AM (inclusive). This is needed to keep the market status as 'CLOSED' after 4:00 PM and before 9:30 AM
		boolean isInNegDealsRange = (hour >= 16) || (hour < 9) || (hour == 9 && minute < 30);

		if (isInNegDealsRange) {
			newState = TipConstants.TIP_DB_STATE_CLOSED;
		}

		//Changes for SR2 Extented Negdeals
		// Donot consider STa message with 13 or 14 within timeframe 9:30 AM and before 3:20 PM . Both setting to Neg Deals from 9:30 AM before 3;20 PM
		//10AM to 2 PM
		//9:30 AM and above
		//3:20 PM and below
		//In those timins if any NegDeals sessions received, Donpt consider
		boolean isStaNegDealsFoundinRange = ( ((hour > 9) && (hour < 15)) || (hour == 9 && minute >= 30 ) || (hour == 15 && minute < 20 ) ) && (newState == TipConstants.TIP_DB_STATE_NEGDEALSONLY);
        
		logger.info("STA Message received MAIN Market: {}", newState);
		logger.info("STA Message isStaNegDealsFoundinRange MAIN Market: {}", isStaNegDealsFoundinRange);
		logger.info("STA Message isInNegDealsRange MAIN Market: {}", isInNegDealsRange);
		
		//logger.info("STa message called for main market ");		
		if (TIPDataObjects.tipMarket[0].current_state != newState && !isStaNegDealsFoundinRange ) {
               if (logger.isDebugEnabled()) logger.debug("Share Market State Change Received, Current State: {} Current Market Status: {} New State: {}", TIPDataObjects.tipMarket[0].current_state, TIPDataObjects.tipMarket[0].market_status, newState);
               TIPDataObjects.tipMarket[0].current_state = newState;
               if (TIPDataObjects.tipMarket[0].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
               	TIPDataObjects.tipMarket[0].market_status = TIPDataObjects.tipMarket[0].current_state;
               TIPDataObjects.tipMarket[0].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));              
             
               // BOD logic for Share
               if (TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_PREOPENED) {
               	TIPDataObjects.isShareBODPreOpen = true;
               	TIPDataObjects.isAfterPreOpen = true;
            	logger.info("TipConstants.TIP_DB_STATE_PREOPENED BOD received");
               }
               
               if (TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_OPENED) {
                  	TIPDataObjects.isShareBODOpen = true;
                  	logger.info("TipConstants.TIP_DB_STATE_OPENED BOD received");
                  }
               
               if(TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_PRECLOSED )
               {
               logger.info("TipConstants.TIP_DB_STATE_PRECLOSED EOD received");
               }
               
               if(TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_CLOSED )
               {
               logger.info("TipConstants.TIP_DB_STATE_CLOSED EOD received");
               }
               
               if(TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_CLOSING_AUCTION )
               {
               logger.info("TipConstants.TIP_DB_STATE_CLOSING_AUCTION EOD received");
               }
               
               if(TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_NEGDEALSONLY )
               {
               logger.info("TipConstants.TIP_DB_STATE_NEGDEALSONLY EOD received");
               }
               
               // EOD logic for Share
             //  if (TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_PRECLOSED   || TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_CLOSED || TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_CLOSING_AUCTION) {
               if (TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_CLOSED ) {
               	TIPDataObjects.isShareEOD = true;
               }
              
               if (! TIPDataObjects.tipMarket[0].isUpdated) {
               	TIPDataObjects.tipMarket[0].isUpdated = true;					                                	
               	TIPDataObjects.totMarketUpdated.incrementAndGet();
              // 	logger.debug("totMarketUpdated in OBK :{}", TIPDataObjects.totMarketUpdated.get());
               }
           }
           TIPDataObjects.tipMarket[0].sequence_no = sequence_no;
        
	}else if(tmpid == TipConstants.SME_MARKET_ID)
	{
		newState = TIPCommonFunctions.setStateBySTaMessage(TIPDataObjects.tipMarket[2].current_state, tmpMSt);

		// Show market status as closed while processing new TIP messages for negotiated deals.
		java.time.LocalTime riyadhNow = Utility.riyadhTime(); // cached ZoneId, no Calendar/TimeZone allocation per message
		int hour = riyadhNow.getHour();
		int minute = riyadhNow.getMinute();
		
		//Changes for SR2 Extented Negdeals
		// Check if time is within 4:00 PM to 9:30 AM (inclusive)
		boolean isInNegDealsRange = (hour >= 16) || (hour < 9) || (hour == 9 && minute < 30);

		if (isInNegDealsRange) {
			newState = TipConstants.TIP_DB_STATE_CLOSED;
		}
		
		// Donot consider STa message with 13 or 14 within timeframe 9:30 AM and before 3:20 PM . Both setting to Neg Deals from 9:30 AM before 3;20 PM . 
		//10AM to 2 PM
		//9:30 AM and above
		//3:20 PM and below
		//In those timins if any NegDeals sessions received, Donpt consider
		boolean isStaNegDealsFoundinRange = ( ((hour > 9) && (hour < 15)) || (hour == 9 && minute >= 30 ) || (hour == 15 && minute < 20 ) ) && (newState == TipConstants.TIP_DB_STATE_NEGDEALSONLY);
		        
		logger.info("STA Message received NOMU Market : {}", newState);
		logger.info("STA Message isStaNegDealsFoundinRange NOMU Market: {}", isStaNegDealsFoundinRange);
		logger.info("STA Message isInNegDealsRange NOMU Market: {}", isInNegDealsRange);
				
			

		 if (TIPDataObjects.tipMarket[2].current_state != newState  && !isStaNegDealsFoundinRange) {
             if (logger.isDebugEnabled()) logger.debug("SME Share Market State Change Received, Current State: {} Current Market Status: {} New State: {}", TIPDataObjects.tipMarket[2].current_state, TIPDataObjects.tipMarket[2].market_status, newState);
             TIPDataObjects.tipMarket[2].current_state = newState;
             if (TIPDataObjects.tipMarket[2].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
             	TIPDataObjects.tipMarket[2].market_status = TIPDataObjects.tipMarket[2].current_state;
             TIPDataObjects.tipMarket[2].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx));  
           
             // BOD logic for Share
             if (TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_PREOPENED) {
             	//logger.info("SME Share Market State Change Received :isSMEShareBOD TRUE");
             	TIPDataObjects.isSMEShareBODPreOpen = true;
             	logger.info("SME Market TipConstants.TIP_DB_STATE_PREOPENED BOD received");
             }
             
             if (TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_OPENED) {
               	TIPDataObjects.isSMEShareBODOpen = true;
               	logger.info("SME Market TipConstants.TIP_DB_STATE_OPENED BOD received");
               }
             
             if(TIPDataObjects.tipMarket[0].market_status == TipConstants.TIP_DB_STATE_NEGDEALSONLY )
             {
             logger.info("TipConstants.TIP_DB_STATE_NEGDEALSONLY EOD received");
             }
             
             
             // EOD logic for Share
             //if (TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_PRECLOSED    || TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_CLOSED|| TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_CLOSING_AUCTION) {
             if (TIPDataObjects.tipMarket[2].market_status == TipConstants.TIP_DB_STATE_CLOSED) {
             	TIPDataObjects.isSMEShareEOD = true;
             }
             
             if (! TIPDataObjects.tipMarket[2].isUpdated) {
             	TIPDataObjects.tipMarket[2].isUpdated = true;
             	TIPDataObjects.totMarketUpdated.incrementAndGet();
             	if (logger.isDebugEnabled()) logger.debug("totMarketUpdated in OBK :{}", TIPDataObjects.totMarketUpdated.get());
             }
            
         }
         TIPDataObjects.tipMarket[2].sequence_no = sequence_no;
     }	 
	 else if(tmpid == TipConstants.BOND_MARKET_ID)
	{
		newState = TIPCommonFunctions.setStateBySTaMessage(TIPDataObjects.tipMarket[1].current_state, tmpMSt);

		// Show market status as closed while processing new TIP messages for negotiated deals.
		java.time.LocalTime riyadhNow = Utility.riyadhTime(); // cached ZoneId, no Calendar/TimeZone allocation per message
		int hour = riyadhNow.getHour();
		int minute = riyadhNow.getMinute();

		// Check if time is within 4:00 PM to 9:30 AM (inclusive)
		boolean isInNegDealsRange = (hour >= 16) || (hour < 9) || (hour == 9 && minute < 30);

		if (isInNegDealsRange) {
			newState = TipConstants.TIP_DB_STATE_CLOSED;
		}

		  if (TIPDataObjects.tipMarket[1].current_state != newState) {
              if (logger.isDebugEnabled()) logger.debug("Bond Market State Change Received, Current State: {} Current Market Status: {} New State: {}", TIPDataObjects.tipMarket[2].current_state, TIPDataObjects.tipMarket[1].market_status, newState);
              TIPDataObjects.tipMarket[1].current_state = newState;
              if (TIPDataObjects.tipMarket[1].market_status != TipConstants.TIP_DB_STATE_SUSPENDED)
              	TIPDataObjects.tipMarket[1].market_status = TIPDataObjects.tipMarket[1].current_state;
             
              TIPDataObjects.tipMarket[1].lasttradeddate = Timestamp.valueOf(Utility.appendTxWithToday(tmpTx)); 
            
              // BOD logic for Share
              if (TIPDataObjects.tipMarket[1].market_status == TipConstants.TIP_DB_STATE_PREOPENED) {
              	TIPDataObjects.isBondBODPreOpen = true;
              	logger.info("Bond Market TipConstants.TIP_DB_STATE_PREOPENED BOD received");
              }
              
              if (TIPDataObjects.tipMarket[1].market_status == TipConstants.TIP_DB_STATE_OPENED) {
                 	TIPDataObjects.isBondBODOpen = true;
                 	logger.info("Bond Market TipConstants.TIP_DB_STATE_OPENED BOD received");
                 }
              
              // EOD logic for Share
              if ( TIPDataObjects.tipMarket[1].market_status == TipConstants.TIP_DB_STATE_CLOSED) {
              	TIPDataObjects.isBondEOD = true;
              }
             
              if (! TIPDataObjects.tipMarket[1].isUpdated) {
              	TIPDataObjects.tipMarket[1].isUpdated = true;
              	TIPDataObjects.totMarketUpdated.incrementAndGet();
              }
              
          }
          TIPDataObjects.tipMarket[1].sequence_no = sequence_no;
      } 
	
}



	//Parse for BDIm messages
	private void parseBasicDataIndexMember(String[] messageValue, int noOfBDIms){
		 sequence_no = 0;
		 tmpUc=0;
		 tmpIdOrderBook=0;
		 tmpIdSecurityGroup =0;
		 tmpOrderBookSourceId=null;
		 tmpGroupSourceId=0;
		 String index=null;
		 String symbol=null;
					
		for (int i=1;i< messageValue.length; i++) {
			value = Utility.splitTagValue(messageValue[i]);
			if (value != null && value.length > 1) {
				if (value[0] != null && value[1] != null) {
					messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
				}
				else {
					break;
				}
				
							switch(BasicDataIndexMember.getMessageValue(messageColumnType)){				
								case 1 : //SEQUENCENO(1,"n")
									sequence_no=Long.parseLong(value[1]);	
									TIPDataObjects.last_Sequencer_Analyzer=sequence_no;								
									break;
								
								case 2 : // IDORDERBOOK (2,"IDo")
									tmpIdOrderBook = Integer.parseInt(value[1]);											
									break;
									
								case 3 : // SECURITYGROUPID(3,"CDi"),
									tmpIdSecurityGroup = Integer.parseInt(value[1]);									
									break;
									
								case 4 :// ORDERBOOKSOURCEID (4,"OSi"),
									tmpOrderBookSourceId=value[1];								
									break;
								
								case 5 :// GROUPSOURCEID (5,"GSi"),
									tmpGroupSourceId=Integer.parseInt(value[1]);								
									break;
								
								case 6 : // UPDATECODE (4,"Uc"),
									tmpUc = Integer.parseInt(value[1]);	
									break;
								
								default:
									break;
							}	//End of Switch		
			}
		}//End of For loop	
		
		
		if(noOfBDIms == 1) //First BDIM message , hence clear the table
		{
			dbUtilObj.deleteIndexComponentsTable();
		}
		
		index  = TIPDataObjects.tipIndexConsolidated_IdCodeToSymbolMap.get(tmpGroupSourceId);
		symbol =tmpOrderBookSourceId;
		
		logger.info("******** BDIm Called *************  Symbol:{}  Index :{} Update Code :{}", symbol, index, updateCode);
        if(null !=symbol && null != index)
        switch(tmpUc)
        {                                        	
        case 1://Insert
        	logger.info("BDIm insertIndexComponents Symbol:{}  Index :{} Update Code :{}", symbol, index, updateCode);
        	dbUtilObj.insertIndexComponents(symbol, index);
        	break;
        case 2://Update
        	logger.info("BDIm unchanged action Symbol:{}  Index :{} Update Code :{}", symbol, index, updateCode);
        	if(!dbUtilObj.isIndexComponentsAdded(symbol, index))
        		dbUtilObj.insertIndexComponents(symbol, index);        	
        	break;
        case 3://Remove
        	logger.info("BDIm deleteIndexComponents Symbol:{}  Index :{} Update Code :{}", symbol, index, updateCode);
        	dbUtilObj.deleteIndexComponents(symbol, index);
        	break;
        case 4://Unchanged
        	logger.info("BDIm unchanged action Symbol:{}  Index :{} Update Code :{}", symbol, index, updateCode);
        	if(!dbUtilObj.isIndexComponentsAdded(symbol, index))
        		dbUtilObj.insertIndexComponents(symbol, index);
        	break;
        } 
		
	}
	

public static void main(String args[])
{
	
	TipConstants.SHARE_MARKET_ID=2;
	
	
	TIPDataObjects.tipMarket[0] = new TIP_MarketVO();
	TIPDataObjects. tipMarket[0].MarketId = TipConstants.SHARE_MARKET_ID;
	TIPDataObjects. tipMarket[0].Pk_exchange_code = TipConstants.SHARE_MARKET_CODE;
 
	TIPDataObjects.tipMarket[1] = new TIP_MarketVO();
	TIPDataObjects.tipMarket[1].MarketId = TipConstants.BOND_MARKET_ID;
	TIPDataObjects.tipMarket[1].Pk_exchange_code = TipConstants.BOND_MARKET_CODE;
 //Added for SME
	TIPDataObjects.tipMarket[2] = new TIP_MarketVO();
	TIPDataObjects.tipMarket[2].MarketId = TipConstants.SME_MARKET_ID;
	TIPDataObjects. tipMarket[2].Pk_exchange_code = TipConstants.SME_MARKET_CODE;
	
	DataParserLowVolm obj = new DataParserLowVolm();
	
	
	
	String message ="STa;n499523;i2;SiSAEQ;s1;Tx151005;MSt4;"	;								
  	if(message != null && !message.equals("")) 
  		obj.messageValue = message.split(TipConstants.SPLITMESSAGE);
	
	obj.parseMarketStates(obj.messageValue );
}

}
