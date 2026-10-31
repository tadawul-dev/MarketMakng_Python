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

import sa.com.tadawul.website.tip.log.Log;
import sa.com.tadawul.website.tip.log.LogFactory;
import sa.com.tadawul.website.tip.messages.OrderBookSummary;
import sa.com.tadawul.website.tip.messages.TStAndTRdMessageType;
import sa.com.tadawul.website.tip.messages.TadawulObjectCategory;
import sa.com.tadawul.website.tip.messages.Trades;
import sa.com.tadawul.website.tip.messages.TradesStatistics;
import sa.com.tadawul.website.tip.queue.MessageDatabaseQueue;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.LoadInitialData;
import sa.com.tadawul.website.tip.shared.TIPCommonFunctions;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;
import sa.com.tadawul.website.tip.vo.TIP_BondVO;
import sa.com.tadawul.website.tip.vo.TIP_GetHighLowPriceVO;
import sa.com.tadawul.website.tip.vo.TIP_SMEShareVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;

public class DataParserTrdAndTSt  implements Runnable{
	
	private final static Log logger = LogFactory.getLog(DataParserTrdAndTSt.class);
	
	private String message = "";
	String messageValue[];
	private String value [] = new String[2];
	private String messageColumnType = ""; 
	
	int totalTradeStatisticsMessages;
	int totalTradeUpdatesMessages;
	int totalOrderBookSummaryMessages;
		
	int tmpid=0;	
	String tmpSymbolId="";		
	int tmpIterId=0;
	long sequence_no=0;
	String tmpPd=null,tmpANt=null,tmpPLp=null,tmpCAp=null,tmpBr=null,tmpPr=null,tmpAq=null,tmpRq=null,tmpAt=null,tmpRt=null,tmpYl=null,tmpPh=null,tmpLOp=null,tmpPf=null ;
	String tmpTime=null,tmpTq=null,tmpUl=null,tmpTp=null,tmpTt=null,tmpTy;
	String tmpOSt=null, tmpLTv=null,tmpACp=null,tmpMCa=null,tmpPl=null,tmpPOf=null,tmpLPw=null,tmpHPw=null;
	String tmpBYl=null,tmpAYa=null;
	String tmpb=null,tmpVb=null,tmpa=null,tmpVa=null;
	boolean setPrevious_close_price;
	boolean isFirstOBsInEvening=true;
	TIP_GetHighLowPriceVO bondHighLowPriceDtVO;
    long accumlatedVolume=0;
    long reportedVolume=0;
    double accumlatedTurnover=0;
    double reportedTurnover=0;
    boolean finished = false;
    
	public DataParserTrdAndTSt(){
		}

	@Override
	public void run() {
		
		logger.info("DataParserTrades andTradeStatisticsUpdate Thread started. " );
		
		while(true)
				{					
							if(MessageQueue.tRdAndTStQueue != null) {
								while (MessageQueue.tRdAndTStQueue.size() > 0) {									
									  try {
										  	message =(String) MessageQueue.tRdAndTStQueue.pop();									
										  	if(message != null && !message.equals("")) {
										  		messageValue = message.split(TipConstants.SPLITMESSAGE);
										  		if(messageValue != null && messageValue.length > 1) {
										  			if(messageValue[0] != null && messageValue[1] != null && !messageValue[1].equals("") && !messageValue[0].equals("")) {								  			
										  			
										  					switch(TStAndTRdMessageType.getMessageValue(messageValue[0])){											  					
										  						case 1://TRd										  							
										  							parseTradeData(messageValue);
										  						break;											  					
										  						case 2://TSt										  							
										  							parseTradeStatistics(messageValue);
										  						break;
										  						case 3://OBs										  							
										  							parseOrderBookSummary(messageValue);
										  						break;
										  						default:
										  							//logger.info("Default Messages : "+ message);
										  					
										  					}//End of Switch
									
										  			}
										  		}
										  	}
								
									  }//End of Try
									  catch(Exception e)
									  {
										  e.printStackTrace();
									  }									  
									}//End of InnerWhile
							}
					
							Utility.sleepAnalyser(TipConstants.DATA_ANALYZER_SLEEP_INTERVAL_TRD);
				}//Outer while
		}//Function run End
		
		
		
		
		
	private void parseTradeStatistics(String[] messageValue){	
		
									totalTradeStatisticsMessages++;
									TIPDataObjects.totalMessagesParsed++;
									  try {										 	
										  //	message =(String) MessageQueue.tradeStatisticsQueue.pop();		
										  	tmpPd=null;tmpANt=null;tmpPLp=null;tmpCAp=null;tmpBr=null;tmpPr=null;tmpAq=null;tmpRq=null;tmpAt=null;tmpRt=null;tmpYl=null;tmpPh=null;tmpLOp=null;tmpPf=null ;
										  	sequence_no=0;										
										 
										  	for (int i=1;i< messageValue.length ; i++) {
												value = messageValue[i].split(TipConstants.SPLITVALUE, 2);
												if (value != null && value.length > 1) {
													if (value[0] != null && value[1] != null) {
														messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
													}
													else {
														break;
													}
															
																	switch(TradesStatistics.getMessageValue(messageColumnType)){				
																		case 1 : // IDCODE (1,"i")
																			 tmpid=Integer.parseInt(value[1]);	
																			break;
																		
																		case 2 : // SOURCEID (2,"Si")
																			  tmpSymbolId=value[1];	
																			 break;
																		
																		case 3 :  //  DIFFLASTPRICE (3, "Pd"),	
																			tmpPd =value[1];											 
																			break;
																		
																		case 4 ://  NUMBEROFTRADES (4, "ANt"),
																			tmpANt=value[1];		
																			break;
																		
																		case 5 : // DIFFLASTPRICEPERCENT (5, "PLp"),
																			tmpPLp = value[1];																				
																			break;	
																			
																		case 6 ://   MARKETCAP (6, "CAp"),
																			tmpCAp=value[1];		
																			break;
																			
																		case 7 ://   BOOKRATIO (7, "Br"),
																			tmpBr=value[1];		
																			break;	
																			
																		case 8 ://    PRICEEARNINGSRATIO (8, "Pr"),
																			tmpPr=value[1];		
																			break;
																			
																		case 9 ://   ACCUMULATEDVOLUME (9, "Aq"),
																			tmpAq=value[1];		
																			break;
																			
																		case 10 ://     REPORTEDVOLUME (10, "Rq"),
																			tmpRq=value[1];		
																			break;
																			
																		case 11 ://     ACCUMULATEDTURNOVER (11, "At"),
																			tmpAt=value[1];		
																			break;
																			
																		case 12 ://    REPORTEDTURNOVER (12, "Rt"),
																			tmpRt=value[1];		
																			break;
																			
																		case 13 ://     LASTYIELD (13, "Yl"),
																			tmpYl=value[1];		
																			break;
																			
																		case 14 ://    HIGHPRICE (14, "Ph"),
																			tmpPh=value[1];		
																			break;
																			
																		case 15 ://   LOWPRICE (15, "LOp"),
																			tmpLOp=value[1];		
																			break;
																			
																		case 16 ://   FIRSTPRICE (16, "Pf");,
																			tmpPf=value[1];		
																			break;
																	
																		case 17 ://     SEQUENCENO(17,"n");
																			sequence_no=Long.parseLong(value[1]);		
																			TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
																			break;
																		default:
																			break;
																	}	//End of Switch
													}
												}//End of For loop		
												
											
												  accumlatedVolume=0;
												  reportedVolume=0;
												  accumlatedTurnover=0;
												  reportedTurnover=0;
												
												 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);
												 
												 
												 switch(OBJ_TYPE)
													{
														case SHARE:
															//Find Index
															 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
											            	 {
											            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
											            	 }
 														//Not Special Traded. Normal Trades
															 if(!TIPDataObjects.tipShare[tmpIterId].isSpecialShare)	
															    {
																 	if(null!=tmpPd)  TIPDataObjects.tipShare[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpANt) TIPDataObjects.tipShare[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
																    if(null!=tmpPLp)  TIPDataObjects.tipShare[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpCAp)  TIPDataObjects.tipShare[tmpIterId].market_cap = Math.round(Double.parseDouble(tmpCAp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpBr)  TIPDataObjects.tipShare[tmpIterId].Pbr = Math.round(Double.parseDouble(tmpBr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpPr)  TIPDataObjects.tipShare[tmpIterId].Per = Math.round(Double.parseDouble(tmpPr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
																    if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
																    if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
																    if(null!=tmpYl)  TIPDataObjects.tipShare[tmpIterId].yield = Math.round(Double.parseDouble(tmpYl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpPh)  TIPDataObjects.tipShare[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpLOp)  TIPDataObjects.tipShare[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpPf)  TIPDataObjects.tipShare[tmpIterId].Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    TIPDataObjects.tipShare[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
																    TIPDataObjects.tipShare[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
																    
																    if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
																    	TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
																    	TIPDataObjects.totShareUpdated++;
											                        }
																   															    
																	logger.info("TradeStatisticsUpdate Trade Type ::"+TIPDataObjects.tipShare[tmpIterId].isSpecialShare );
																 
																	TIP_ShareVO shareToQ = new TIP_ShareVO(TIPDataObjects.tipShare[tmpIterId]);
																	MessageDatabaseQueue.tipShareIntraQ.setFrame(shareToQ);
																	
																	//Normal TIP Updates
																	if(!TipConstants.IS_NEGDEALS_EARLY_START)
																	{
																		MessageDatabaseQueue.tipShareIntraQ.setFrame(shareToQ);
																	}
																	
																	//Inase NegDeals Early start
																	if(TipConstants.IS_NEGDEALS_EARLY_START && TIPDataObjects.isAfterPreOpen && Utility.shouldUpdatedShares())
																		{
																				MessageDatabaseQueue.tipShareIntraQ.setFrame(shareToQ);
																		}else
																		{
																				logger.info(" Share IntraQueue Received Before Market Preopen for sysmbol " + TIPDataObjects.tipShare[tmpIterId]);
																		  }
															    }
															//If Special Traded
															 if(TIPDataObjects.tipShare[tmpIterId].isSpecialShare)	
															    {
																 logger.info("Special Trade Received for Symbol :" + TIPDataObjects.tipShare[tmpIterId]);
																 TIP_ShareVO shareToQ = new TIP_ShareVO(TIPDataObjects.tipShare[tmpIterId]);
																 	if(null!=tmpPd)  shareToQ.net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpANt) shareToQ.no_of_trades = Long.parseLong(tmpANt);
																    if(null!=tmpPLp)  shareToQ.percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpCAp)  shareToQ.market_cap = Math.round(Double.parseDouble(tmpCAp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpBr)  shareToQ.Pbr = Math.round(Double.parseDouble(tmpBr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpPr)  shareToQ.Per = Math.round(Double.parseDouble(tmpPr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
																    if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
																    if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
																    if(null!=tmpYl)  shareToQ.yield = Math.round(Double.parseDouble(tmpYl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpPh)  shareToQ.high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpLOp)  shareToQ.low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    if(null!=tmpPf)  shareToQ.Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
																    shareToQ.volume_traded = accumlatedVolume + reportedVolume;
																    shareToQ.turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
																    
																    //To update the Historical Page. This issue is to fix the last trade of Negdeals issue
																    TIPDataObjects.tipShare[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
																    TIPDataObjects.tipShare[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
																    if(null!=tmpANt) TIPDataObjects.tipShare[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
														
																    
																    if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
																    	TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
																    	TIPDataObjects.totShareUpdated++;
											                        }
																   				
																    shareToQ.sequence_no = sequence_no;
																    
																	logger.info("Special TradeStatisticsUpdate Trade Type ::"+shareToQ.isSpecialShare );
																 
																	logger.info("MessageDatabaseQueue.tipShareSpecialIntraQ Added ::"+shareToQ.fk_te_symbol );
															    	MessageDatabaseQueue.tipShareSpecialIntraQ.setFrame(shareToQ);	
															    	logger.info("MessageDatabaseQueue.tipShareSpecialIntraQ Added ::"+MessageDatabaseQueue.tipShareSpecialIntraQ.size() );
															    }
															
															    	TIPDataObjects.tipShare[tmpIterId].isSpecialShare = false;
															    
															    TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
															 // logger.info("TSt Main  Iter ID "+tmpIterId +" Symbol: "+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" NetChange : "+TIPDataObjects.tipShare[tmpIterId].net_change +" no_of_trades  :"+TIPDataObjects.tipShare[tmpIterId].no_of_trades+" percent_change :"+TIPDataObjects.tipShare[tmpIterId].percent_change  );
															 // logger.info("TMarketCap "+TIPDataObjects.tipShare[tmpIterId].market_cap +" volume_traded: "+TIPDataObjects.tipShare[tmpIterId].volume_traded +" turnover : "+TIPDataObjects.tipShare[tmpIterId].turnover+" TIPDataObjects.totShareUpdated  :"+TIPDataObjects.totShareUpdated  );
															break;
														
														case SMESHARE:
															//Find Index
															 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
											            	 {
											            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
											            	 }
															
															 //Not Special Traded. Normal Trades
															 if(!TIPDataObjects.tipSMEShare[tmpIterId].isSpecialShare)	
															    {
															
															    if(null!=tmpPd)  TIPDataObjects.tipSMEShare[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpANt) TIPDataObjects.tipSMEShare[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
															    if(null!=tmpPLp)  TIPDataObjects.tipSMEShare[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpCAp)  TIPDataObjects.tipSMEShare[tmpIterId].market_cap = Math.round(Double.parseDouble(tmpCAp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpBr)  TIPDataObjects.tipSMEShare[tmpIterId].Pbr = Math.round(Double.parseDouble(tmpBr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpPr)  TIPDataObjects.tipSMEShare[tmpIterId].Per = Math.round(Double.parseDouble(tmpPr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
															    if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
															    if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
															    if(null!=tmpYl)  TIPDataObjects.tipSMEShare[tmpIterId].yield = Math.round(Double.parseDouble(tmpYl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpPh)  TIPDataObjects.tipSMEShare[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpLOp)  TIPDataObjects.tipSMEShare[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpPf)  TIPDataObjects.tipSMEShare[tmpIterId].Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    TIPDataObjects.tipSMEShare[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
															    TIPDataObjects.tipSMEShare[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
															
															    //To update the Historical Page. This issue is to fix the last trade of Negdeals issue
															    TIPDataObjects.tipSMEShare[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
															    TIPDataObjects.tipSMEShare[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
															    if(null!=tmpANt) TIPDataObjects.tipSMEShare[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
														
															    
															    if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
															    	TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
															    	TIPDataObjects.totSMEShareUpdated++;
										                        }
															    
															    logger.info("TradeStatisticsUpdate Trade Type ::"+TIPDataObjects.tipSMEShare[tmpIterId].isSpecialShare );
																 
															    TIP_SMEShareVO smeShareToQ = new TIP_SMEShareVO(TIPDataObjects.tipSMEShare[tmpIterId]);

															    MessageDatabaseQueue.tipSMEShareIntraQ.setFrame(smeShareToQ);
														    }else{
														//If Special Traded
														    	TIP_SMEShareVO smeShareToQ = new TIP_SMEShareVO(TIPDataObjects.tipSMEShare[tmpIterId]);
														        if(null!=tmpPd)  smeShareToQ.net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpANt) smeShareToQ.no_of_trades = Long.parseLong(tmpANt);
															    if(null!=tmpPLp)  smeShareToQ.percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpCAp)  smeShareToQ.market_cap = Math.round(Double.parseDouble(tmpCAp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpBr)  smeShareToQ.Pbr = Math.round(Double.parseDouble(tmpBr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpPr)  smeShareToQ.Per = Math.round(Double.parseDouble(tmpPr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
															    if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
															    if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
															    if(null!=tmpYl)  smeShareToQ.yield = Math.round(Double.parseDouble(tmpYl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpPh)  smeShareToQ.high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpLOp)  smeShareToQ.low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    if(null!=tmpPf)  smeShareToQ.Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															    smeShareToQ.volume_traded = accumlatedVolume + reportedVolume;
															    smeShareToQ.turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
															
															    if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
															    	TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
															    	TIPDataObjects.totSMEShareUpdated++;
										                        }
															    smeShareToQ.sequence_no = sequence_no;
															    
															    logger.info("Special SME TradeStatisticsUpdate Trade Type ::"+smeShareToQ.isSpecialShare );
																 
															    logger.info("MessageDatabaseQueue.tipSMEShareSpecialIntraQ Added ::"+smeShareToQ.fk_te_symbol );
														    	MessageDatabaseQueue.tipSMEShareSpecialIntraQ.setFrame(smeShareToQ);	
														    	logger.info("MessageDatabaseQueue.tipSMEShareSpecialIntraQ Added ::"+MessageDatabaseQueue.tipSMEShareSpecialIntraQ.size() );
														    }
															    TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no;
																//  logger.info("TSt SME  Iter ID "+tmpIterId +" Symbol: "+TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol +" NetChange : "+TIPDataObjects.tipSMEShare[tmpIterId].net_change +" no_of_trades  :"+TIPDataObjects.tipSMEShare[tmpIterId].no_of_trades+" percent_change :"+TIPDataObjects.tipSMEShare[tmpIterId].percent_change  );
																//  logger.info("TMarketCap "+TIPDataObjects.tipSMEShare[tmpIterId].market_cap +" volume_traded: "+TIPDataObjects.tipSMEShare[tmpIterId].volume_traded +" turnover : "+TIPDataObjects.tipSMEShare[tmpIterId].turnover+" TIPDataObjects.totShareUpdated  :"+TIPDataObjects.totSMEShareUpdated  );

															break;
														
														case BOND:
															//Find Index
															 if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
											            	 {
											            		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
											            	 }
												//Not Special Traded. Normal Trades
												 if(!TIPDataObjects.tipBond[tmpIterId].isSpecialShare)	
												    {
													 	if(null!=tmpPd)  TIPDataObjects.tipBond[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														if(null!=tmpANt) TIPDataObjects.tipBond[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
														if(null!=tmpPLp)  TIPDataObjects.tipBond[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
														if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
														if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
														if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
														if(null!=tmpPh)  TIPDataObjects.tipBond[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														if(null!=tmpLOp)  TIPDataObjects.tipBond[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														if(null!=tmpPf)  TIPDataObjects.tipBond[tmpIterId].Todays_open_price = Math.round(Double.parseDouble(tmpPf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														TIPDataObjects.tipBond[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
														TIPDataObjects.tipBond[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
															          
														  //To update the Historical Page. This issue is to fix the last trade of Negdeals issue
														 if(null!=tmpANt) TIPDataObjects.tipBond[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
														TIPDataObjects.tipBond[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
														TIPDataObjects.tipBond[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
													
													
														
														 if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
														   	TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
														   	TIPDataObjects.totBondUpdated++;
										                   }
														 TIP_BondVO bondToQ = new TIP_BondVO(TIPDataObjects.tipBond[tmpIterId]);
														 MessageDatabaseQueue.tipBondIntraQ.setFrame(bondToQ);		
														 TIPDataObjects.tipBond[tmpIterId].sequence_no = sequence_no;
												    }else
												    {
												    	 TIP_BondVO bondToQ = new TIP_BondVO(TIPDataObjects.tipBond[tmpIterId]);
												    	 if(null!=tmpPd)  bondToQ.net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
												    	 if(null!=tmpANt) bondToQ.no_of_trades = Long.parseLong(tmpANt);
														 if(null!=tmpPLp)  bondToQ.percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														 if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
														 if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
														 if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
														 if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
														 if(null!=tmpPh)  bondToQ.high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														 if(null!=tmpLOp)  bondToQ.low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														 if(null!=tmpPf)  bondToQ.Todays_open_price = Math.round(Double.parseDouble(tmpPf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
														 bondToQ.volume_traded = accumlatedVolume + reportedVolume;
														 bondToQ.turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
																          
														 if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
															   	TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
															   	TIPDataObjects.totBondUpdated++;
											                 }
														 bondToQ.sequence_no = sequence_no;														
														 logger.info("MessageDatabaseQueue.tipBondSpecialIntraQ Added ::"+bondToQ.fk_fi_symbol );
													     MessageDatabaseQueue.tipBondSpecialIntraQ.setFrame(bondToQ);	
													     logger.info("MessageDatabaseQueue.tipBondSpecialIntraQ Added ::"+MessageDatabaseQueue.tipBondSpecialIntraQ.size() );
												    }
														 
															  //  logger.info("TSt Bond  Iter ID "+tmpIterId +" Symbol: "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" NetChange : "+TIPDataObjects.tipBond[tmpIterId].net_change +" no_of_trades  :"+TIPDataObjects.tipBond[tmpIterId].no_of_trades+" percent_change :"+TIPDataObjects.tipBond[tmpIterId].percent_change  );
																//.info(" volume_traded: "+TIPDataObjects.tipBond[tmpIterId].volume_traded +" turnover : "+TIPDataObjects.tipBond[tmpIterId].turnover+" TIPDataObjects.totShareUpdated  :"+TIPDataObjects.totBondUpdated  );

												break;
															
											case OTCSHARE:
												//Find Index															
												 if(TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
								            	 {
								            		 tmpIterId = (int)TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
								            	 } else
								            	 {
								            		 logger.info("TRd received for unkown symbol :"+tmpSymbolId);
								            		 break;
								            	 }
												
												 logger.info("OTC TRd received for  symbol in parseTradeStatistics :"+tmpSymbolId);
											
													 	if(null!=tmpPd)  TIPDataObjects.tipOTCShare[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpANt) TIPDataObjects.tipOTCShare[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
													    if(null!=tmpPLp)  TIPDataObjects.tipOTCShare[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpCAp)  TIPDataObjects.tipOTCShare[tmpIterId].market_cap = Math.round(Double.parseDouble(tmpCAp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpBr)  TIPDataObjects.tipOTCShare[tmpIterId].Pbr = Math.round(Double.parseDouble(tmpBr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpPr)  TIPDataObjects.tipOTCShare[tmpIterId].Per = Math.round(Double.parseDouble(tmpPr) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
													    if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
													    if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
													    if(null!=tmpYl)  TIPDataObjects.tipOTCShare[tmpIterId].yield = Math.round(Double.parseDouble(tmpYl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpPh)  TIPDataObjects.tipOTCShare[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpLOp)  TIPDataObjects.tipOTCShare[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    if(null!=tmpPf)  TIPDataObjects.tipOTCShare[tmpIterId].Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
													    TIPDataObjects.tipOTCShare[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
													    TIPDataObjects.tipOTCShare[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
													    
													    if (! TIPDataObjects.tipOTCShare[tmpIterId].isUpdated) {
													    	TIPDataObjects.tipOTCShare[tmpIterId].isUpdated = true;
													    	TIPDataObjects.totOTCShareUpdated++;
								                        }												    
														 
														/*TIP_ShareVO shareToQ = new TIP_ShareVO(TIPDataObjects.tipOTCShare[tmpIterId]);
														MessageDatabaseQueue.tipShareIntraQ.setFrame(shareToQ);
												    */
												    TIPDataObjects.tipOTCShare[tmpIterId].sequence_no = sequence_no;
												 logger.info("TSt Main  Iter ID "+tmpIterId +" Symbol: "+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" NetChange : "+TIPDataObjects.tipShare[tmpIterId].net_change +" no_of_trades  :"+TIPDataObjects.tipShare[tmpIterId].no_of_trades+" percent_change :"+TIPDataObjects.tipShare[tmpIterId].percent_change  );
												  logger.info("TMarketCap "+TIPDataObjects.tipShare[tmpIterId].market_cap +" volume_traded: "+TIPDataObjects.tipShare[tmpIterId].volume_traded +" turnover : "+TIPDataObjects.tipShare[tmpIterId].turnover+" TIPDataObjects.totShareUpdated  :"+TIPDataObjects.totShareUpdated  );
												break;
												
											case OTCBOND:
												//Find Index
												 if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
								            	 {
								            		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
								            	 }
								
										 	if(null!=tmpPd)  TIPDataObjects.tipOTCBond[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
											if(null!=tmpANt) TIPDataObjects.tipOTCBond[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
											if(null!=tmpPLp)  TIPDataObjects.tipOTCBond[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
											if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
											if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
											if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
											if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
											if(null!=tmpPh)  TIPDataObjects.tipOTCBond[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
											if(null!=tmpLOp)  TIPDataObjects.tipOTCBond[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
											if(null!=tmpPf)  TIPDataObjects.tipOTCBond[tmpIterId].Todays_open_price = Math.round(Double.parseDouble(tmpPf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
											TIPDataObjects.tipOTCBond[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
											TIPDataObjects.tipOTCBond[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
												          
											  //To update the Historical Page. This issue is to fix the last trade of Negdeals issue
											 if(null!=tmpANt) TIPDataObjects.tipOTCBond[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
											TIPDataObjects.tipOTCBond[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
											TIPDataObjects.tipOTCBond[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
										
										
											
											 if (! TIPDataObjects.tipOTCBond[tmpIterId].isUpdated) {
											   	TIPDataObjects.tipOTCBond[tmpIterId].isUpdated = true;
											   	TIPDataObjects.totOTCBondUpdated++;
							                   }
											 TIP_BondVO bondToQ = new TIP_BondVO(TIPDataObjects.tipOTCBond[tmpIterId]);
											 MessageDatabaseQueue.tipOTCBondIntraQ.setFrame(bondToQ);		
											 TIPDataObjects.tipOTCBond[tmpIterId].sequence_no = sequence_no;
									   
											 
												  //  logger.info("TSt Bond  Iter ID "+tmpIterId +" Symbol: "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" NetChange : "+TIPDataObjects.tipBond[tmpIterId].net_change +" no_of_trades  :"+TIPDataObjects.tipBond[tmpIterId].no_of_trades+" percent_change :"+TIPDataObjects.tipBond[tmpIterId].percent_change  );
													//.info(" volume_traded: "+TIPDataObjects.tipBond[tmpIterId].volume_traded +" turnover : "+TIPDataObjects.tipBond[tmpIterId].turnover+" TIPDataObjects.totShareUpdated  :"+TIPDataObjects.totBondUpdated  );

									break;

												
												
												default:
													break;
													}								
										  	message = null;
										  	
									  }//End of Try
									  catch(Exception e)
									  {
										  logger.error("Error Occured in TSt Processing", e);
										  e.printStackTrace();
									  }
									  logger.debug("DataParserTrades Statistics Updates   : Total  No of Trade Statistics Messages Parsed : "+totalTradeStatisticsMessages);
			
		}//Function run End
	
	
	@SuppressWarnings("unchecked")
	private void parseTradeData(String[] messageValue)
	{
		totalTradeUpdatesMessages++;
		TIPDataObjects.totalMessagesParsed++;
		  try {
			  
			  	tmpTime=null;tmpTq=null;tmpUl=null;tmpTp=null;tmpTt=null;tmpTy=null;
			  	sequence_no=0;										 								  
			  	finished = false;
			  	for (int i=1;i< messageValue.length && !finished ; i++) {
					value = messageValue[i].split(TipConstants.SPLITVALUE, 2);
					if (value != null && value.length > 1) {
						if (value[0] != null && value[1] != null) {
							messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
						}
						else {
							break;
						}							
										switch(Trades.getMessageValue(messageColumnType)){				
											case 1 : // IDCODE (1,"i")
												 tmpid=Integer.parseInt(value[1]);
												break;
											
											case 2 : // SOURCEID (2,"Si")
												  tmpSymbolId=value[1];
												 break;
											
											case 3 :  //TIMEEXEC (3, "Tx"),	
												tmpTime =value[1];
												break;
											
											case 4 ://  TRADEDQUANTITY (4, "Tq"),
												tmpTq=value[1];
												break;
											
											case 5 : // TRADEUPDATESLASTPAID (5, "Ul"),
												tmpUl = value[1];
												finished = true;
												break;	
												
											case 6 ://   TRADEDPRICE (6, "Tp"),
												tmpTp=value[1];	
												//logger.info( " TRd Message tmpTp :"+tmpTp);
												break;
												
											case 7 ://  TRADETYPE (7, "Tt");
												tmpTt=value[1];
												break;		
												
											case 8 ://  Sequence No (8, "n");
												sequence_no=Long.parseLong(value[1]);
												TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
												break;		
										
												//Added for Yield Change CRQ21157
											case 9 ://  Traded Yield (9, "Ty");
												tmpTy = value[1];
												break;		
												
											default:
												break;
										}	//End of Switch
						}
					}//End of For loop	
					
			  	 logger.info("TRd receivedfor Symbol:"+tmpSymbolId);
					
					 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);
					 
					 logger.info("TRd receivedfor Symbol:"+tmpSymbolId +" OBJ_TYPE :"+OBJ_TYPE.toString());
					 
					 switch(OBJ_TYPE)
						{
							case SHARE:
								//Find Index
								 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 } else
				            	 {
				            		 logger.info("TRd received for unkown symbol :"+tmpSymbolId);
				            		 break;
				            	 }
								
								 TIPDataObjects.tipShare[tmpIterId].isSpecialShare= false;														
									 if(null != tmpTime)
									 {
										 try
										 {
										 TIPDataObjects.tipShare[tmpIterId].transaction_date = Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
										 TIPDataObjects.tipShare[tmpIterId].last_update_time = TIPDataObjects.tipShare[tmpIterId].transaction_date;
										 }catch(Exception e)
							 				 {
												//  logger.error("Error Occured in TRD Processing", e);
							 				 
										 }
									  }
									 if(null != tmpTq)TIPDataObjects.tipShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpTq);									                         
		                                
									 if (tmpUl.equalsIgnoreCase("Y"))
		                                	TIPDataObjects.tipShare[tmpIterId].lastNormalTradeTime = TIPDataObjects.tipShare[tmpIterId].transaction_date;
		                                    //tradeUpdatesLastPaid = true;
		                            	if(null != tmpTp) 
		                            	{
		                            		TIPDataObjects.tipShare[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpTp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
		                            		// Fix for UAT bug - 22-Apr-2009                                
		                            		TIPDataObjects.tipShare[tmpIterId].todays_close_price = TIPDataObjects.tipShare[tmpIterId].last_trade_price;
		                            	}
		                           
		                          
		                               	 /* For Handling put-through message , Trade Type(Tt)26  */ //Tt
		                            	//if(curMsg.values[tmpIter1].equals("26"))
		                            	if((tmpTt!=null) && ( tmpTt.equals("5")||tmpTt.equals("6")||tmpTt.equals("7")||tmpTt.equals("8")||tmpTt.equals("9")||tmpTt.equals("10")))
		                            	{
		                            		logger.debug("Got the Speical Share-->+ComapnyId:"+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol+":   Quantity:"+TIPDataObjects.tipShare[tmpIterId].last_trade_quantity);
		                            		TIPDataObjects.tipShare[tmpIterId].isSpecialShare= true;
		                            		TIPDataObjects.tipShare[tmpIterId].isUpdated = true;									                            		
		                            		TIPDataObjects.tipShare[tmpIterId].specialTradesSequenceNos.add(sequence_no);
		                            		TIPDataObjects.totShareUpdated++;
		                            		
		                            		//This is needed to parse the TST , to have enough time
		                            		//Utility.sleepAnalyser(10);
		                            		
		                            		/*//Todo should be reviewed
		                            		 TIP_ShareVO shareToQ = new TIP_ShareVO(TIPDataObjects.tipShare[tmpIterId]);
											 MessageDatabaseQueue.tipShareIntraQ.setFrame(shareToQ);*/	
		                            		
		                            	}
		                            	
		                            	if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
		                            		TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
		                            		TIPDataObjects.totShareUpdated++;
		                                }
		                            	TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
		                            	//logger.info("*** TRD****last_trade_price for company :"+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" is :"+TIPDataObjects.tipShare[tmpIterId].last_trade_price);
		                            	//logger.info("TLU Main  TIPDataObjects.tipShare[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" TIPDataObjects.tipShare[tmpIterId].transaction_date : "+TIPDataObjects.tipShare[tmpIterId].transaction_date +" TIPDataObjects.tipShare[tmpIterId].last_trade_price  :"+TIPDataObjects.tipShare[tmpIterId].last_trade_price+" TIPDataObjects.tipShare[tmpIterId].last_trade_quantity :"+TIPDataObjects.tipShare[tmpIterId].last_trade_quantity   );
								break;
							
							case SMESHARE:
								//Find Index
								 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 } else
				            	 {
				            		 logger.info("TRd received for unkown symbol :"+tmpSymbolId);
				            		 break;
				            	 }
								 	
								 TIPDataObjects.tipSMEShare[tmpIterId].isSpecialShare= false;
								 
								 if(null != tmpTime)
								 {
									 try
									 {
									 TIPDataObjects.tipSMEShare[tmpIterId].transaction_date = Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));//Tx
									 TIPDataObjects.tipSMEShare[tmpIterId].last_update_time = TIPDataObjects.tipSMEShare[tmpIterId].transaction_date; 
										 }catch(Exception e)
						 				 {
											//  logger.error("Error Occured in TRD Processing", e);
						 				 
									 }
								 }
								 if(null != tmpTq)	 TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpTq);									                         
		                                if (tmpUl.equalsIgnoreCase("Y"))
		                                	TIPDataObjects.tipSMEShare[tmpIterId].lastNormalTradeTime = TIPDataObjects.tipSMEShare[tmpIterId].transaction_date;
		                                    //tradeUpdatesLastPaid = true;
		                           if(null != tmpTp) 
		                           	{
		                                TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpTp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
		                                // Fix for UAT bug - 22-Apr-2009                                
		                            	TIPDataObjects.tipSMEShare[tmpIterId].todays_close_price = TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price;
		                           	}
		                          
		                               	 /* For Handling put-through message , Trade Type(Tt)26  */ //Tt
		                            	//if(curMsg.values[tmpIter1].equals("26"))
		                            	if((tmpTt!=null) && (tmpTt.equals("5")||tmpTt.equals("6")||tmpTt.equals("7")||tmpTt.equals("8")||tmpTt.equals("9")||tmpTt.equals("10")))
		                            	{
		                            		logger.debug("Got the Speical Share-->+ComapnyId:"+TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol+":   Quantity:"+TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity);
		                            		TIPDataObjects.tipSMEShare[tmpIterId].isSpecialShare= true;
		                            		TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
		                            		//ToDo
		                                    //totShareUpdated++;
		                            	}
		                            	if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
		                            		TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
		                            		TIPDataObjects.totSMEShareUpdated++;
		                                }
		                            	
		                            	TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no;
		                            //	logger.info("TLU Main  TIPDataObjects.tipSMEShare[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol +" TIPDataObjects.tipSMEShare[tmpIterId].transaction_date : "+TIPDataObjects.tipSMEShare[tmpIterId].transaction_date +" TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price  :"+TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price+" TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity :"+TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity   );
								break;
							
							case BOND:
								//Find Index
								 if(TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 } else
				            	 {
				            		 logger.info("TRd received for unkown symbol :"+tmpSymbolId);
				            		 break;
				            	 }
								
								 TIPDataObjects.tipBond[tmpIterId].isSpecialShare= false;
								 
								 if(null != tmpTime)
								 {
									 try
									 {
									 TIPDataObjects.tipBond[tmpIterId].transaction_date = Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));//Tx
									 TIPDataObjects.tipBond[tmpIterId].last_update_time = TIPDataObjects.tipBond[tmpIterId].transaction_date; 
										 }catch(Exception e)
						 				 {
											//  logger.error("Error Occured in TRD Processing", e);
									 }
								 }
								 if(null != tmpTq)	 TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpTq);	
								 {
		                             TIPDataObjects.tipBond[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpTp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
		                                // Fix for UAT bug - 22-Apr-2009                                
		                           TIPDataObjects.tipBond[tmpIterId].todays_close_price = TIPDataObjects.tipBond[tmpIterId].last_trade_price;
								 }
		                         
								//Added for Bonds Yield Change CRQ21157
								 if(null != tmpTy)	 
								 {
									 
		                             TIPDataObjects.tipBond[tmpIterId].traded_yield = Math.round(Double.parseDouble(tmpTy) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff;
		                             logger.info("Traded Yield Received for symbol : "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" Vaue " +TIPDataObjects.tipBond[tmpIterId].traded_yield);
								 }
								 
								 /* For Handling put-through message , Trade Type(Tt)26  */ //Tt
	                            	//if(curMsg.values[tmpIter1].equals("26"))
	                            	if((tmpTt!=null) && (tmpTt.equals("5")||tmpTt.equals("6")||tmpTt.equals("7")||tmpTt.equals("8")||tmpTt.equals("9")||tmpTt.equals("10")))
	                            	{
	                            		logger.debug("Got the Speical Trades for Sukuk & Bonds -->+Sukuk ID:"+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol+":   Quantity:"+TIPDataObjects.tipBond[tmpIterId].last_trade_quantity);
	                            		TIPDataObjects.tipBond[tmpIterId].isSpecialShare= true;
	                            		TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
	                            		//ToDo
	                                    //totShareUpdated++;
	                            		//This is needed to parse the TST , to have enough time
	                            		Utility.sleepAnalyser(5);
	                            	}
								 
		                            	 // Fix for UAT bug - 22-Apr-2009
		                             TIPDataObjects.tipBond[tmpIterId].todays_close_price = TIPDataObjects.tipBond[tmpIterId].last_trade_price;
	                                    if(TIPDataObjects.tipBond[tmpIterId].high_52_weeks_price == 0 || TIPDataObjects.tipBond[tmpIterId].last_trade_price >= TIPDataObjects.tipBond[tmpIterId].high_52_weeks_price){
	                                    	TIPDataObjects.tipBond[tmpIterId].high_52_weeks_price = TIPDataObjects.tipBond[tmpIterId].last_trade_price;
	                                    	TIPDataObjects.tipBond[tmpIterId].high_52_weeks_date = new Date(System.currentTimeMillis());;
	                                    }
	                                    if(TIPDataObjects.tipBond[tmpIterId].low_52_weeks_price == 0 || TIPDataObjects.tipBond[tmpIterId].last_trade_price <= TIPDataObjects.tipBond[tmpIterId].low_52_weeks_price){
	                                    	TIPDataObjects.tipBond[tmpIterId].low_52_weeks_price = TIPDataObjects.tipBond[tmpIterId].last_trade_price;
	                                    	TIPDataObjects.tipBond[tmpIterId].low_52_weeks_date = new Date(System.currentTimeMillis());;
	                                    }
		                            	
	                                    if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
		                            		TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
		                            		TIPDataObjects.totBondUpdated++;
		                                }
	                                    
	                                    TIPDataObjects.tipBond[tmpIterId].sequence_no = sequence_no;
	                                	//logger.info("TLU tipBond  TIPDataObjects.tipBond[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" TIPDataObjects.tipBond[tmpIterId].transaction_date : "+TIPDataObjects.tipBond[tmpIterId].transaction_date +" TIPDataObjects.tipBond[tmpIterId].last_trade_price  :"+TIPDataObjects.tipBond[tmpIterId].last_trade_price+" TIPDataObjects.tipBond[tmpIterId].last_trade_quantity :"+TIPDataObjects.tipBond[tmpIterId].last_trade_quantity   );
								break;
								
							case OTCSHARE:
								//Find Index
								 if(TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipOTCShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 } else
				            	 {
				            		 logger.info("TRd received for unkown symbol :"+tmpSymbolId);
				            		 break;
				            	 }
								
								
								 logger.info("OTC TRd received for  symbol :"+tmpSymbolId);
								 
									 if(null != tmpTime)
									 {
										 try
										 {
										 TIPDataObjects.tipOTCShare[tmpIterId].transaction_date = Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
										 TIPDataObjects.tipOTCShare[tmpIterId].last_update_time = TIPDataObjects.tipOTCShare[tmpIterId].transaction_date;
										 }catch(Exception e)
							 				 {
												
							 				 
										 }
									  }
									 if(null != tmpTq)TIPDataObjects.tipOTCShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpTq);									                         
		                                
									 if (tmpUl.equalsIgnoreCase("Y"))
		                                	TIPDataObjects.tipOTCShare[tmpIterId].lastNormalTradeTime = TIPDataObjects.tipOTCShare[tmpIterId].transaction_date;
		                                    //tradeUpdatesLastPaid = true;
		                            	if(null != tmpTp) 
		                            	{
		                            		TIPDataObjects.tipOTCShare[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpTp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
		                            		// Fix for UAT bug - 22-Apr-2009                                
		                            		TIPDataObjects.tipOTCShare[tmpIterId].todays_close_price = TIPDataObjects.tipOTCShare[tmpIterId].last_trade_price;
		                            	}
		                            	
		                            	/*if (! TIPDataObjects.tipOTCShare[tmpIterId].isUpdated) {
		                            		TIPDataObjects.tipOTCShare[tmpIterId].isUpdated = true;
		                            		TIPDataObjects.totOTCShareUpdated++;
		                                }*/
		                            	TIPDataObjects.tipOTCShare[tmpIterId].sequence_no = sequence_no;
		                            	//logger.info("*** TRD****last_trade_price for company :"+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" is :"+TIPDataObjects.tipShare[tmpIterId].last_trade_price);
		                            	//logger.info("TLU Main  TIPDataObjects.tipShare[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" TIPDataObjects.tipShare[tmpIterId].transaction_date : "+TIPDataObjects.tipShare[tmpIterId].transaction_date +" TIPDataObjects.tipShare[tmpIterId].last_trade_price  :"+TIPDataObjects.tipShare[tmpIterId].last_trade_price+" TIPDataObjects.tipShare[tmpIterId].last_trade_quantity :"+TIPDataObjects.tipShare[tmpIterId].last_trade_quantity   );
								break;
								
							case OTCBOND:
								//Find Index
								 if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
				            	 {
				            		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
				            	 } else
				            	 {
				            		 logger.info("TRd received for unkown symbol :"+tmpSymbolId);
				            		 break;
				            	 }
								
								 logger.info("OTCBond Trade Received for Bond ID :"+tmpSymbolId);
								 
								 TIPDataObjects.tipOTCBond[tmpIterId].isSpecialShare= false;
								 
								 if(null != tmpTime)
								 {
									 try
									 {
									 TIPDataObjects.tipOTCBond[tmpIterId].transaction_date = Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));//Tx
									 TIPDataObjects.tipOTCBond[tmpIterId].last_update_time = TIPDataObjects.tipOTCBond[tmpIterId].transaction_date; 
										 }catch(Exception e)
						 				 {
											//  logger.error("Error Occured in TRD Processing", e);
									 }
								 }
								 if(null != tmpTq)	 TIPDataObjects.tipOTCBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpTq);	
								 {
		                             TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpTp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
		                                // Fix for UAT bug - 22-Apr-2009                                
		                           TIPDataObjects.tipOTCBond[tmpIterId].todays_close_price = TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price;
								 }
		                         
								//Added for Bonds Yield Change CRQ21157
								 if(null != tmpTy)	 
								 {
									 
		                             TIPDataObjects.tipOTCBond[tmpIterId].traded_yield = Math.round(Double.parseDouble(tmpTy) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff;
		                             logger.info("Traded Yield Received for symbol : "+TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol +" Vaue " +TIPDataObjects.tipBond[tmpIterId].traded_yield);
								 } 
		                            	 // Fix for UAT bug - 22-Apr-2009
		                             TIPDataObjects.tipOTCBond[tmpIterId].todays_close_price = TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price;
	                                    if(TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_price == 0 || TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price >= TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_price){
	                                    	TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_price = TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price;
	                                    	TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_date = new Date(System.currentTimeMillis());;
	                                    }
	                                    if(TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_price == 0 || TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price <= TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_price){
	                                    	TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_price = TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price;
	                                    	TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_date = new Date(System.currentTimeMillis());;
	                                    }
		                            	
	                                    if (! TIPDataObjects.tipOTCBond[tmpIterId].isUpdated) {
		                            		TIPDataObjects.tipOTCBond[tmpIterId].isUpdated = true;
		                            		TIPDataObjects.totOTCBondUpdated++;
		                                }
	                                    
	                                    TIPDataObjects.tipOTCBond[tmpIterId].sequence_no = sequence_no;
	                                	logger.info("TRd tipOTCBond  TIPDataObjects.tipOTCBond[tmpIterId] : Shares "+tmpIterId +" Symbol: "+TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol +" TIPDataObjects.tipOTCBond[tmpIterId].transaction_date : "+TIPDataObjects.tipOTCBond[tmpIterId].transaction_date +" TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price  :"+TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price+" TIPDataObjects.tipOTCBond[tmpIterId].last_trade_quantity :"+TIPDataObjects.tipOTCBond[tmpIterId].last_trade_quantity   );
								break;
							
								
							default:
								break;
						}
	
		  }//End of Try
		  catch(Exception e)
		  {
			  logger.error("Error Occured in TRd Processing", e);
		  }
		  
		logger.debug("DataParserTradesUpdates   : Total  No of Trade Updates Messages Parsed : "+totalTradeUpdatesMessages);
	
	}
	
	
	
	//Parse for OBs messages
	private void parseOrderBookSummary(String[] messageValue){
		
		totalOrderBookSummaryMessages++;
		TIPDataObjects.totalMessagesParsed++;
		
		finished = false;
		tmpid=0;									
		String tmpSymbolId="",tmpLPd=null,tmpTime=null;
		tmpOSt=null;
		tmpIterId=0;
		long tmpDateLong=0;
		tmpLTv=null;
		tmpACp=null; tmpPl=null; tmpPf=null; tmpMCa=null; tmpPOf=null;tmpPh=null;tmpLOp=null;tmpHPw=null;tmpLPw=null; tmpPd=null;tmpANt=null;tmpPLp=null;tmpAq=null;tmpRq=null; tmpAt=null; tmpRt=null;
		//Added for Bonds Yield Change CRQ21157
		tmpBYl=null;tmpAYa=null;
		//Added for Bid & Ask Price defects CRQ22455
		tmpb=null;tmpVb=null;tmpa=null;tmpVa=null;
		setPrevious_close_price = false;
		sequence_no = 0;		
				
		for (int i=1;i< messageValue.length ; i++) {
			value = messageValue[i].split(TipConstants.SPLITVALUE, 2);
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
								
								//Added for Bonds Yield Change CRQ21157
								case 18 ://BIDYIELDATLEVEL1(18,"BYl"),
									tmpBYl=value[1];													 
									break;
									
								case 19 ://ASKYIELDATLEVEL1(19,"AYa")
									tmpAYa=value[1];													 
									break;
									
								//Added for Bid & Ask Price defects CRQ22455
								case 20 ://   BIDPRICEATLEVEL1(20,"BPr"),
									tmpb=value[1];
									break;	
									
								case 21 ://    ASKPRICEATLEVEL1(21,"APl"), 
									tmpa=value[1];									
									break;
									
								case 22 ://   BIDVOLUMEATLEVEL1(22,"VBl"),
									tmpVb=value[1];
									break;
									
								case 23 ://    ASKVOLUMEATLEVEL1(23,"VAl");
									tmpVa=value[1];
									break;
									
								case 24 :  //  DIFFLASTPRICE (24, "Pd"),	
									tmpPd =value[1];											 
									break;
								
								case 25://  NUMBEROFTRADES (25 "ANt"),
									tmpANt=value[1];		
									break;
									
								case 26 : // DIFFLASTPRICEPERCENT (26, "PLp"),
									tmpPLp = value[1];																				
									break;	
									
								case 27 ://   ACCUMULATEDVOLUME (9, "Aq"),
									tmpAq=value[1];		
									break;
									
								case 28 ://     REPORTEDVOLUME (10, "Rq"),
									tmpRq=value[1];		
									break;
									
								case 29 ://     ACCUMULATEDTURNOVER (11, "At"),
									tmpAt=value[1];		
									break;
									
								case 30 ://    REPORTEDTURNOVER (12, "Rt"),
									tmpRt=value[1];		
									break;
								default:
									break;
							}	//End of Switch
			}
		}//End of For loop			
				
		 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);
		  accumlatedVolume=0;
		  reportedVolume=0;
		  accumlatedTurnover=0;
		  reportedTurnover=0;
		 
		 logger.info("OBS Message OBJ_TYPE :"+OBJ_TYPE.toString());
		 
		 if (TipConstants.TARGIN_FEED_REQ_DATE != tmpDateLong)
		 {
             setPrevious_close_price = true;
		 }else
		 {
			 
			/* if(isFirstOBsInEvening)
             {//To fix the OBS wait for TRD and TRD to complete
				 logger.info("Sleep wait for OBS Thread :"+TipConstants.SLEEPWAIT_FOR_OBS_BEFORE_PROCESSING);
            	 try {                        		 
					Thread.sleep(TipConstants.SLEEPWAIT_FOR_OBS_BEFORE_PROCESSING);
				} catch (InterruptedException e) {
					logger.error("Error occured in OBS processing :"+e.getMessage());
				}
             }*/			
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
					  	
					 	try
					 	{
					 	if(null!=tmpTime)
					 	 TIPDataObjects.tipShare[tmpIterId].last_update_time=Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
					 	}catch(Exception e)
					 	{
					 		logger.error("OBS Error", e.fillInStackTrace());
					 	}
						 
					 	 if(null !=tmpPOf) TIPDataObjects.tipShare[tmpIterId].todays_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
					 	
					 	 //Added for Bid & Ask Price defects CRQ22455
						 if(null!=tmpb)	TIPDataObjects.tipShare[tmpIterId].bid_price = Math.round(Double.parseDouble(tmpb) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
						 if(null!=tmpa)	TIPDataObjects.tipShare[tmpIterId].ask_price = Math.round(Double.parseDouble(tmpa) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
						 if(null!=tmpVa)	TIPDataObjects.tipShare[tmpIterId].ask_quantity = (long)Double.parseDouble(tmpVa);
						 if(null!=tmpVb)	TIPDataObjects.tipShare[tmpIterId].bid_quantity = (long)Double.parseDouble(tmpVb);
					 	 
					 	 
					 	if (setPrevious_close_price)
					 	 {					
					 		if(null != tmpMCa)TIPDataObjects.tipShare[tmpIterId].market_cap=Math.round(Double.parseDouble(tmpMCa) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
					 		if(tmpLPd!=null)
					 			TIPDataObjects.tipShare[tmpIterId].transaction_date=Timestamp.valueOf(Utility.formatDate(tmpLPd)+" 00:00:00");
					 	 }								 		 
					 	 else
					 	 {
					 		
					 		if(null != tmpPl)
					 			TIPDataObjects.tipShare[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		else
					 			TIPDataObjects.tipShare[tmpIterId].last_trade_price=0d;
					 		
					 		if(null != tmpLTv)
					 				TIPDataObjects.tipShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
					 		else
					 			   TIPDataObjects.tipShare[tmpIterId].last_trade_quantity = 0;
					 		
					 		if(null != tmpPf)TIPDataObjects.tipShare[tmpIterId].Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		//if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].last_trade_price = tmpPl;
					 		
					 		if(null != tmpPh)TIPDataObjects.tipShare[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		if(null != tmpLOp)TIPDataObjects.tipShare[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
					 		
					 		if(null!=tmpPd)  TIPDataObjects.tipShare[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
							if(null!=tmpANt) TIPDataObjects.tipShare[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
							if(null!=tmpPLp)  TIPDataObjects.tipShare[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
							if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
							if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
							if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
							if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
							TIPDataObjects.tipShare[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
							TIPDataObjects.tipShare[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
							
					 		
					 		if(TIPDataObjects.tipShare[tmpIterId].no_of_trades >0)
					 		{
					 			 logger.debug("Before setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipShare[tmpIterId].fk_te_symbol+ " last_trade_price: " + TIPDataObjects.tipShare[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipShare[tmpIterId].last_trade_quantity + " transaction_date: " + TIPDataObjects.tipShare[tmpIterId].transaction_date);
					 			if(null != tmpACp)
					 				TIPDataObjects.tipShare[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 			else
						 			TIPDataObjects.tipShare[tmpIterId].last_trade_price=0d;
					 			
					 			if(null != tmpLTv) 
					 				TIPDataObjects.tipShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
					 			else
					 				TIPDataObjects.tipShare[tmpIterId].last_trade_quantity = 0;
					 			
	                               TIPDataObjects.tipShare[tmpIterId].transaction_date = TIPDataObjects.tipShare[tmpIterId].lastNormalTradeTime;
	                                logger.debug("After setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipShare[tmpIterId].fk_te_symbol + " last_trade_price: " + TIPDataObjects.tipShare[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipShare[tmpIterId].last_trade_quantity  + " transaction_date: " + TIPDataObjects.tipShare[tmpIterId].transaction_date);
					 		}
					 		
					 	//	logger.info("*** OBS****last_trade_price for company :"+TIPDataObjects.tipShare[tmpIterId].fk_te_symbol +" is :"+TIPDataObjects.tipShare[tmpIterId].last_trade_price);
					 		
					 		/*if (! TIPDataObjects.tipShare[tmpIterId].isUpdated) {
					 			TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
					 			TIPDataObjects.totShareUpdated++;
                            }*/
					 		
					 		TIPDataObjects.tipShare[tmpIterId].isUpdated = true;
				 			TIPDataObjects.totShareUpdated++;
					 		
					 		//Commented to take from STa
					 		/* if (TIPDataObjects.tipShare[tmpIterId].fk_te_symbol.equals(TipConstants.SHARE_MARKET_SYMBOL_REFERENCE)) {
	                            	
					 			TIPDataObjects.isShareEOD = true;
	                            }			*/					 		
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
				 	
				 	 //Added for Bid & Ask Price defects CRQ22455
					 if(null!=tmpb)	TIPDataObjects.tipSMEShare[tmpIterId].bid_price = Math.round(Double.parseDouble(tmpb) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
					 if(null!=tmpa)	TIPDataObjects.tipSMEShare[tmpIterId].ask_price = Math.round(Double.parseDouble(tmpa) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
					 if(null!=tmpVa)	TIPDataObjects.tipSMEShare[tmpIterId].ask_quantity = (long)Double.parseDouble(tmpVa);
					 if(null!=tmpVb)	TIPDataObjects.tipSMEShare[tmpIterId].bid_quantity = (long)Double.parseDouble(tmpVb);
				 	 
				 	 
					 	if (setPrevious_close_price)
					 	 {
					 		if(null != tmpMCa)TIPDataObjects.tipSMEShare[tmpIterId].market_cap=Math.round(Double.parseDouble(tmpMCa) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		if(null != tmpPOf)TIPDataObjects.tipSMEShare[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
					if(tmpLPd!=null)
					 			TIPDataObjects.tipSMEShare[tmpIterId].transaction_date=Timestamp.valueOf(Utility.formatDate(tmpLPd)+" 00:00:00");;
					 	 }								 		 
					 	 else
					 	 {
					 		if(null != tmpPl)
					 			TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		else
					 			TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price=0d;
					 		
					 		if(null != tmpPf)
					 			TIPDataObjects.tipSMEShare[tmpIterId].Todays_open = Math.round(Double.parseDouble(tmpPf) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		//if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].last_trade_price = tmpPl;
					 		if(null != tmpLTv)
					 			TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
					 		else
					 			TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity =0;
					 		
					 		if(null != tmpPh)TIPDataObjects.tipSMEShare[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 		if(null != tmpLOp)TIPDataObjects.tipSMEShare[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
					 		
					 		
					 		if(null!=tmpPd)  TIPDataObjects.tipSMEShare[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
							if(null!=tmpANt) TIPDataObjects.tipSMEShare[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
							if(null!=tmpPLp)  TIPDataObjects.tipSMEShare[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
							if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
							if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
							if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
							if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
							TIPDataObjects.tipSMEShare[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
							TIPDataObjects.tipSMEShare[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
							

					 		
					 		
				if(TIPDataObjects.tipSMEShare[tmpIterId].no_of_trades >0)
					 		{
					 			 logger.debug("Before setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol+ " last_trade_price: " + TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity + " transaction_date: " + TIPDataObjects.tipSMEShare[tmpIterId].transaction_date);
					 			if(null != tmpACp)
					 				TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;
					 			else
					 				TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price=0d;
					 			
					 			if(null != tmpLTv) 
					 				TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
					 			else
					 				TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity =0;
					 			
	                            TIPDataObjects.tipSMEShare[tmpIterId].transaction_date = TIPDataObjects.tipSMEShare[tmpIterId].lastNormalTradeTime;
	                                logger.debug("After setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol + " last_trade_price: " + TIPDataObjects.tipSMEShare[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipSMEShare[tmpIterId].last_trade_quantity  + " transaction_date: " + TIPDataObjects.tipSMEShare[tmpIterId].transaction_date);
					 		}		
				
								/*if (! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated) {
						 			TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
						 			TIPDataObjects.totSMEShareUpdated++;
	                            }*/
				
								TIPDataObjects.tipSMEShare[tmpIterId].isUpdated = true;
					 			TIPDataObjects.totSMEShareUpdated++;
					 			
								//Commented to take from STa
					 		/* if (TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol.equals(TipConstants.SME_MARKET_SYMBOL_REFERENCE)) {
	                            	
					 			TIPDataObjects.isSMEShareEOD = true;
	                            }			*/				 		
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
					if(null !=tmpPOf) TIPDataObjects.tipBond[tmpIterId].todays_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;	
					
					//Added for Bonds Yield Change CRQ21157
					if(null !=tmpBYl) TIPDataObjects.tipBond[tmpIterId].bestBid_yield=Math.round(Double.parseDouble(tmpBYl) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff ;
					if(null !=tmpAYa) TIPDataObjects.tipBond[tmpIterId].bestOffer_yield=Math.round(Double.parseDouble(tmpAYa) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff ;
					
					//Added for Bid & Ask Price defects CRQ22455
					 if(null!=tmpb)	TIPDataObjects.tipBond[tmpIterId].bid_price = Math.round(Double.parseDouble(tmpb) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
					 if(null!=tmpa)	TIPDataObjects.tipBond[tmpIterId].ask_price = Math.round(Double.parseDouble(tmpa) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
					 if(null!=tmpVa)	TIPDataObjects.tipBond[tmpIterId].ask_quantity = (long)Double.parseDouble(tmpVa);
					 if(null!=tmpVb)	TIPDataObjects.tipBond[tmpIterId].bid_quantity = (long)Double.parseDouble(tmpVb);
					 
					//  logger.info("Best Bid Yield Received for symbol : "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" Vaue " +TIPDataObjects.tipBond[tmpIterId].bestBid_yield);
					//  logger.info("Best Offer Yield Received for symbol : "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" Vaue " +TIPDataObjects.tipBond[tmpIterId].bestOffer_yield);
					
					if(null !=tmpHPw)TIPDataObjects.tipBond[tmpIterId].high_52_weeks_price=Math.round(Double.parseDouble(tmpHPw) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
					if(null !=tmpLPw)TIPDataObjects.tipBond[tmpIterId].low_52_weeks_price=Math.round(Double.parseDouble(tmpLPw) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
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
				 		 if(null !=tmpPOf) TIPDataObjects.tipBond[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
				 		
				 		if(tmpLPd!=null)
				 			TIPDataObjects.tipBond[tmpIterId].transaction_date=Timestamp.valueOf(Utility.formatDate(tmpLPd)+" 00:00:00");;
				 	 }								 		 
				 	 else
				 	 {
				 		/*if(TIPDataObjects.tipBond[tmpIterId].no_of_trades >0)
				 		{
				 		if(null != tmpPl)TIPDataObjects.tipBond[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
				 		if(null != tmpLTv)TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
				 		}*/
				 		
				 		if(null != tmpPl)
				 			TIPDataObjects.tipBond[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
				 		else
				 			TIPDataObjects.tipBond[tmpIterId].last_trade_price=0d;
				 		
				 		if(null != tmpPf)TIPDataObjects.tipBond[tmpIterId].Todays_open_price = Math.round(Double.parseDouble(tmpPf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
				 		//if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].last_trade_price = tmpPl;
				 		if(null != tmpLTv)
				 			TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
				 		else
				 			TIPDataObjects.tipBond[tmpIterId].last_trade_quantity =0;
				 		
				 		if(null != tmpPh)TIPDataObjects.tipBond[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;;
				 		if(null != tmpLOp)TIPDataObjects.tipBond[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;	;
				 		
				 		if(null!=tmpPd)  TIPDataObjects.tipBond[tmpIterId].net_change = Math.round(Double.parseDouble(tmpPd) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
						if(null!=tmpANt) TIPDataObjects.tipBond[tmpIterId].no_of_trades = Long.parseLong(tmpANt);
						if(null!=tmpPLp)  TIPDataObjects.tipBond[tmpIterId].percent_change = Math.round(Double.parseDouble(tmpPLp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
						if(null!=tmpAq) accumlatedVolume = Long.parseLong(tmpAq);
						if(null!=tmpRq) reportedVolume = Long.parseLong(tmpRq);															   
						if(null!=tmpAt)  accumlatedTurnover = Math.round(Double.parseDouble(tmpAt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
						if(null!=tmpRt)  reportedTurnover= Math.round(Double.parseDouble(tmpRt) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;															    
						TIPDataObjects.tipBond[tmpIterId].volume_traded = accumlatedVolume + reportedVolume;
						TIPDataObjects.tipBond[tmpIterId].turnover =  Math.round((accumlatedTurnover + reportedTurnover)*TipConstants.twoDecimalRoundOff)/TipConstants.twoDecimalRoundOff;
						

				 		if(TIPDataObjects.tipBond[tmpIterId].no_of_trades >0)
				 		{
				 			 logger.debug("Before setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol+ " last_trade_price: " + TIPDataObjects.tipBond[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipBond[tmpIterId].last_trade_quantity + " transaction_date: " + TIPDataObjects.tipBond[tmpIterId].transaction_date);
					 			if(null != tmpACp)
					 				TIPDataObjects.tipBond[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
					 			else
						 			TIPDataObjects.tipBond[tmpIterId].last_trade_price=0d;
					 			
					 			if(null != tmpLTv) 
					 				TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
					 			else
						 			TIPDataObjects.tipBond[tmpIterId].last_trade_quantity =0;
					 			
					 			if(null != tmpACp)TIPDataObjects.tipBond[tmpIterId].todays_close_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
							                               
                                logger.debug("After setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol + " last_trade_price: " + TIPDataObjects.tipBond[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipBond[tmpIterId].last_trade_quantity  + " transaction_date: " + TIPDataObjects.tipBond[tmpIterId].transaction_date);
				 	}			
				 		
				 		/*if (! TIPDataObjects.tipBond[tmpIterId].isUpdated) {
				 			TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
				 			TIPDataObjects.totBondUpdated++;
                        }*/
				 		
				 		TIPDataObjects.tipBond[tmpIterId].isUpdated = true;
			 			TIPDataObjects.totBondUpdated++;
				 		//ToDo
				 		/* if (TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol.equals(TipConstants.SME_MARKET_SYMBOL_REFERENCE)) {
                            	
                                isSMEShareEOD = true;
                            }			*/				 		
				 	 }		
				 	
				 	TIPDataObjects.tipBond[tmpIterId].sequence_no = sequence_no;
				break;
				
				case OTCBOND:
					//Find Index
					 if(TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
	            	 {
	            		 tmpIterId = (int)TIPDataObjects.tipOTCBond_Symbol_ArrayIndexMap.get(tmpSymbolId);
	            	 }		
					 
					 logger.info("OBS Message for OTC Bond :"+tmpSymbolId);
					 
					 if( null != tmpOSt) TIPDataObjects.tipOTCBond[tmpIterId].market_status=TIPCommonFunctions.setState(TIPDataObjects.tipOTCBond[tmpIterId].market_status, Integer.parseInt(tmpOSt));
						 if(null!=tmpTime)
				 	 TIPDataObjects.tipOTCBond[tmpIterId].last_update_time=Timestamp.valueOf(Utility.appendTxWithToday(tmpTime));
					if(null !=tmpPOf) TIPDataObjects.tipOTCBond[tmpIterId].todays_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;	
					
					//Added for Bonds Yield Change CRQ21157
					if(null !=tmpBYl) TIPDataObjects.tipOTCBond[tmpIterId].bestBid_yield=Math.round(Double.parseDouble(tmpBYl) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff ;
					if(null !=tmpAYa) TIPDataObjects.tipOTCBond[tmpIterId].bestOffer_yield=Math.round(Double.parseDouble(tmpAYa) * TipConstants.fourDecimalRoundOff) / TipConstants.fourDecimalRoundOff ;
					
					//Added for Bid & Ask Price defects CRQ22455
					 if(null!=tmpb)	TIPDataObjects.tipOTCBond[tmpIterId].bid_price = Math.round(Double.parseDouble(tmpb) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
					 if(null!=tmpa)	TIPDataObjects.tipOTCBond[tmpIterId].ask_price = Math.round(Double.parseDouble(tmpa) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff;
					 if(null!=tmpVa)	TIPDataObjects.tipOTCBond[tmpIterId].ask_quantity = (long)Double.parseDouble(tmpVa);
					 if(null!=tmpVb)	TIPDataObjects.tipOTCBond[tmpIterId].bid_quantity = (long)Double.parseDouble(tmpVb);
					 
					//  logger.info("Best Bid Yield Received for symbol : "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" Vaue " +TIPDataObjects.tipBond[tmpIterId].bestBid_yield);
					//  logger.info("Best Offer Yield Received for symbol : "+TIPDataObjects.tipBond[tmpIterId].fk_fi_symbol +" Vaue " +TIPDataObjects.tipBond[tmpIterId].bestOffer_yield);
					
					if(null !=tmpHPw)TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_price=Math.round(Double.parseDouble(tmpHPw) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
					if(null !=tmpLPw)TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_price=Math.round(Double.parseDouble(tmpLPw) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
					 	if(LoadInitialData.hmBondHighLowPriceDtMap != null){
                            bondHighLowPriceDtVO = (TIP_GetHighLowPriceVO)LoadInitialData.hmBondHighLowPriceDtMap.get(TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol);
                            if(bondHighLowPriceDtVO != null){
                                if(TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_price != bondHighLowPriceDtVO.high_52_weeks_price){
                                    logger.error("High52WeeksPrice is not matching for Bond Symbol "+ TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol
                                            + " Current: " + bondHighLowPriceDtVO.high_52_weeks_price + " Incoming: " + TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_price);
                                }
                                TIPDataObjects.tipOTCBond[tmpIterId].high_52_weeks_date = bondHighLowPriceDtVO.high_52_weeks_date;
                               
                                if(TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_price != bondHighLowPriceDtVO.low_52_weeks_price){
                                    logger.error("Low52WeeksPrice is not matching for Bond Symbol "+ TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol
                                            + " Current: " + bondHighLowPriceDtVO.low_52_weeks_price + " Incoming: " + TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_price);
                                }
                                TIPDataObjects.tipOTCBond[tmpIterId].low_52_weeks_date = bondHighLowPriceDtVO.low_52_weeks_date;
                            }
                        }
				 	 
				 	if (setPrevious_close_price)
				 	 {
				 		//TIPDataObjects.tipBond[tmpIterId].market_cap=tmpMCa;
				 		 if(null !=tmpPOf) TIPDataObjects.tipOTCBond[tmpIterId].Previous_close_price=Math.round(Double.parseDouble(tmpPOf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
				 		
				 		if(tmpLPd!=null)
				 			TIPDataObjects.tipOTCBond[tmpIterId].transaction_date=Timestamp.valueOf(Utility.formatDate(tmpLPd)+" 00:00:00");;
				 	 }								 		 
				 	 else
				 	 {
				 		/*if(TIPDataObjects.tipBond[tmpIterId].no_of_trades >0)
				 		{
				 		if(null != tmpPl)TIPDataObjects.tipBond[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff ;	
				 		if(null != tmpLTv)TIPDataObjects.tipBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
				 		}*/
				 		
				 		if(null != tmpPl)
				 			TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price=Math.round(Double.parseDouble(tmpPl) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
				 		else
				 			TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price=0d;
				 		
				 		if(null != tmpPf)TIPDataObjects.tipOTCBond[tmpIterId].Todays_open_price = Math.round(Double.parseDouble(tmpPf) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
				 		//if(null != tmpPOf)TIPDataObjects.tipShare[tmpIterId].last_trade_price = tmpPl;
				 		if(null != tmpLTv)
				 			TIPDataObjects.tipOTCBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
				 		else
				 			TIPDataObjects.tipOTCBond[tmpIterId].last_trade_quantity =0;
				 		
				 		if(null != tmpPh)TIPDataObjects.tipOTCBond[tmpIterId].high_price = Math.round(Double.parseDouble(tmpPh) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;;
				 		if(null != tmpLOp)TIPDataObjects.tipOTCBond[tmpIterId].low_price = Math.round(Double.parseDouble(tmpLOp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;	;

				 		if(TIPDataObjects.tipOTCBond[tmpIterId].no_of_trades >0)
				 		{
				 			 logger.debug("Before setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol+ " last_trade_price: " + TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipBond[tmpIterId].last_trade_quantity + " transaction_date: " + TIPDataObjects.tipBond[tmpIterId].transaction_date);
					 			if(null != tmpACp)
					 				TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
					 			else
						 			TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price=0d;
					 			
					 			if(null != tmpLTv) 
					 				TIPDataObjects.tipOTCBond[tmpIterId].last_trade_quantity = Integer.parseInt(tmpLTv);
					 			else
						 			TIPDataObjects.tipOTCBond[tmpIterId].last_trade_quantity =0;
					 			
					 			if(null != tmpACp)TIPDataObjects.tipOTCBond[tmpIterId].todays_close_price = Math.round(Double.parseDouble(tmpACp) * TipConstants.threeDecimalRoundOff) / TipConstants.threeDecimalRoundOff ;
							                               
                                logger.debug("After setting Normal Trade as Last Trade - Symbol: " + TIPDataObjects.tipOTCBond[tmpIterId].fk_fi_symbol + " last_trade_price: " + TIPDataObjects.tipOTCBond[tmpIterId].last_trade_price + " last_trade_quantity: " + TIPDataObjects.tipBond[tmpIterId].last_trade_quantity  + " transaction_date: " + TIPDataObjects.tipBond[tmpIterId].transaction_date);
				 		}			
				 		
				 		
				 		
				 		TIPDataObjects.tipOTCBond[tmpIterId].isUpdated = true;
			 			TIPDataObjects.totOTCBondUpdated++;
				 					 		
				 	 }		
				 	
				 	TIPDataObjects.tipOTCBond[tmpIterId].sequence_no = sequence_no;
				break;

		
				default:
					break;
			}	
		 logger.debug("DataParserTradesUpdates   : Total  No of OBS Messages Parsed : "+totalOrderBookSummaryMessages);
	}
	
}
