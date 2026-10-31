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
 * Name          :     FileName : TIP_BaseVO.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.vo;

import sa.com.tadawul.website.tip.shared.DBManagerMarket;

/**
 * @description This class contains attributes to get and set start,end time,sequenceno and frameno
 * from each thread
 * @ version 1.0
 */
public class TIP_BaseVO {
    
    /**No of Frames from each thread*/
    public volatile int frameNo;
    
    /**Start time of DataAnalyser thread*/
    public volatile long analyserStartTime;
    
    /**End time of Dataanalyser thread*/
    public volatile long analyserEndTime;
    
    /**Start time of Reader thread*/
    public volatile long readerStartTime;
    
    /**End time of Reader thread*/
    public volatile long readerEndTime;
    
    /**Sequence Number from each thread*/
    public volatile long sequence_no;
    
    /**Begining of the Day flag*/
    public volatile boolean isBOD;
    
    /**EOD of the Day flag*/
    public volatile boolean isEOD;
    
    /**flag to indicate where this VO has been updated since last database update*/
    public volatile boolean isUpdated;
    
    /**flag to indicate this VO is the last record in the current set*/
    public volatile boolean isEndOfBatch;
    
    public volatile boolean isBODPreOpen;
    
    public volatile boolean isBODOpen;
    
    public static BestBidOffVO[] getInitailBestBidsOffers() {
    	BestBidOffVO[] _bestBids = new BestBidOffVO[DBManagerMarket.ORDER_BOOK_DEPTH];
    	 for (int i = 0; i < DBManagerMarket.ORDER_BOOK_DEPTH; i++) {
    		 _bestBids[i] = new BestBidOffVO(i+1, 0.0,0,0);
         }
    	 return _bestBids;
    }
}
