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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.messages.IndicativeOpening;
import sa.com.tadawul.website.tip.messages.TadawulObjectCategory;
import sa.com.tadawul.website.tip.queue.MessageQueue;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import sa.com.tadawul.website.tip.shared.Utility;

public class DataParserIndicativeOpenings  implements Runnable{
	
	private static final Logger logger = LogManager.getLogger(DataParserIndicativeOpenings.class);
	
	private String message = "";
	String messageValue[];
	private String value [] = new String[2];
	private String messageColumnType = "";
		
	int totalINoMessages;		
	int tmpid=0;	
	String tmpTOp=null,tmpTOv=null;
	String tmpSymbolId="";		
	int tmpIterId=0;
	long sequence_no=0;
	
	public DataParserIndicativeOpenings(){	
	}

	@Override
	public void run() {
		
		logger.info("DataParserIndicativeOpenings Thread started. " );
		
		
		while(true)
				{
				while (MessageQueue.indicativeOpeningQueue != null && MessageQueue.indicativeOpeningQueue.size() > 0) {
								//	logger.info("DataParserIndicativeOpenings Queue not null. " );	
									if(totalINoMessages==0)
									{
										//First message processing should be delayed until other Low volume thread are completed BDIn, BDLi
										Utility.sleepAnalyser(TipConstants.SLEEPINTERVALFOROBK_OKS_INOFORFIRSTMESSAGE);
									}
									totalINoMessages++;
									TIPDataObjects.totalMessagesParsed.incrementAndGet();
									  try {
										  	message =MessageQueue.indicativeOpeningQueue.pop();
										  	tmpTOp=null;tmpTOv=null;sequence_no=0;

										  	if(message != null && !message.equals("")) {
										  		messageValue = message.split(TipConstants.SPLITMESSAGE);
												for (int i=0;i< messageValue.length ; i++) {
													value = Utility.splitTagValue(messageValue[i]);
													if (value != null && value.length > 1) {
														if (value[0] != null && value[1] != null) {
															messageColumnType = value[0] + messageValue[i].charAt(value[0].length());
														}
														else {
															break;
														}
														switch(IndicativeOpening.getMessageValue(messageColumnType)){				
																		case 1 : // IDCODE (1,"i")
																			 tmpid=Integer.parseInt(value[1]);	
																			break;
																		
																		case 2 : // SOURCEID (2,"Si")
																			  tmpSymbolId=value[1];	
																			 break;
																		
																		case 3 : //INDICATIVEPRICE (3, "TOp"),
																			tmpTOp =value[1];											 
																			break;
																		
																		case 4 ://  INDICATIVEVOLUME (4, "TOv");
																			tmpTOv=value[1];		
																			break;
																			
																		case 5:// SEQUENCENO(5,"n");
																			sequence_no=Long.parseLong(value[1]);
																			TIPDataObjects.last_Sequencer_Analyzer=sequence_no;
																			//logger.info("sequence_no in INO :{}", sequence_no);
																			break;
																		default:
																			break;
																	}	//End of Switch
													}
												}//End of For loop	
												
												 TadawulObjectCategory OBJ_TYPE = TadawulObjectCategory.getShareMessageCategoty(tmpSymbolId);											
												
												/* logger.info("INo symbold:{}", tmpSymbolId);
												 logger.info("INo OBJ_TYPE:{}", OBJ_TYPE.name());
										*/		 
												 switch(OBJ_TYPE)
													{												 
												
													case SHARE:									            	 
														//Find Index
														 if(TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
										            	 {
										            		 tmpIterId = (int)TIPDataObjects.tipShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
										            	 }
														
															 if(null != tmpTOp) TIPDataObjects.tipShare[tmpIterId].indicative_Open_price = Math.round(Double.parseDouble(tmpTOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															 if(null != tmpTOv) TIPDataObjects.tipShare[tmpIterId].indicative_Open_volume = Long.parseLong(tmpTOv);
															 
															 if(! TIPDataObjects.tipShare[tmpIterId].isUpdated)
															 {
																 TIPDataObjects.tipShare[tmpIterId].isUpdated =  true;
																 TIPDataObjects.totShareUpdated.incrementAndGet();
															 }
															 
															 TIPDataObjects.tipShare[tmpIterId].sequence_no = sequence_no;
										 				//logger.info("TIPDataObjects.tipShare[tipIndexIter] : Index {} Symbol: {} indicative_Open_price: {}  indicative_Open_volume {}", tmpIterId, TIPDataObjects.tipShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipShare[tmpIterId].indicative_Open_price, TIPDataObjects.tipShare[tmpIterId].indicative_Open_volume);
    		
														break;
														
													case SMESHARE:	
														//Find Index
														 if(TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId) !=null)
										            	 {
										            		 tmpIterId = (int)TIPDataObjects.tipSMEShare_Symbol_ArrayIndexMap.get(tmpSymbolId);
										            	 }
														
														     if(null != tmpTOp)TIPDataObjects.tipSMEShare[tmpIterId].indicative_Open_price = Math.round(Double.parseDouble(tmpTOp) * TipConstants.twoDecimalRoundOff) / TipConstants.twoDecimalRoundOff;
															 if(null != tmpTOv) TIPDataObjects.tipSMEShare[tmpIterId].indicative_Open_volume = Long.parseLong(tmpTOv);		
															 
															 if(! TIPDataObjects.tipSMEShare[tmpIterId].isUpdated)
															 {
																 TIPDataObjects.tipSMEShare[tmpIterId].isUpdated =  true;																 
																 TIPDataObjects.totSMEShareUpdated.incrementAndGet();
															 }
															 
															 TIPDataObjects.tipSMEShare[tmpIterId].sequence_no = sequence_no;
															// logger.info("TIPDataObjects.tipSMEShare[tipIndexIter] : Index {} Symbol: {} indicative_Open_price: {}  indicative_Open_volume {}", tmpIterId, TIPDataObjects.tipSMEShare[tmpIterId].fk_te_symbol, TIPDataObjects.tipSMEShare[tmpIterId].indicative_Open_price, TIPDataObjects.tipSMEShare[tmpIterId].indicative_Open_volume);
														break;
													
													default:
														break;														
													
									             }	
										  	}
								
									  }//End of Try
									  catch(Exception e)
									  {
										  logger.error("Error Occured in INo Processing", e);
									  }
									  
									 
									//logger.info("DataParserIndicativeOpenings  Statistics : Total  No of INo Messages Parsed : {}", totalINoMessages);
									  
									
								}//End of InnerWhile
						
					MessageQueue.indicativeOpeningQueue.awaitData(TipConstants.PARSER_QUEUE_IDLE_WAIT_MS); // was: sleep(DATA_ANALYZER_SLEEP_INTERVAL_INO) polling
				}//Outer while
		}//Function run End
		
		
	
	
	
}
