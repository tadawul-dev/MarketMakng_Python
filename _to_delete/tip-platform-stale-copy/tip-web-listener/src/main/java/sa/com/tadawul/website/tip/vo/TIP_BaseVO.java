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

import java.util.Arrays;

import sa.com.tadawul.website.tip.shared.DBManagerMarket;

/**
 * @description This class contains attributes to get and set start,end time,sequenceno and frameno
 * from each thread
 * @ version 1.0
 */
public class TIP_BaseVO {
    
    /**No of Frames from each thread*/
    public int frameNo;
    
    /**Start time of DataAnalyser thread*/
    public long analyserStartTime;
    
    /**End time of Dataanalyser thread*/
    public long analyserEndTime;
    
    /**Start time of Reader thread*/
    public long readerStartTime;
    
    /**End time of Reader thread*/
    public long readerEndTime;
    
    /**Sequence Number from each thread*/
    public long sequence_no;
    
    /**Begining of the Day flag*/
    public boolean isBOD;
    
    /**EOD of the Day flag*/
    public boolean isEOD;
    
    /**flag to indicate where this VO has been updated since last database update.
     * The ONLY volatile field of the value objects: a parser writes the data fields and then sets this flag;
     * DBQueueUpdater reads the flag before copying, so the volatile write/read publishes the data fields
     * (happens-before). Copies travel through MessageChannel queues, which publish them as well. Making every
     * field volatile (as before 2.1) put a full memory fence on each of the ~140 writes of a share copy. */
    public volatile boolean isUpdated;
    
    /**flag to indicate this VO is the last record in the current set*/
    public boolean isEndOfBatch;
    
    public boolean isBODPreOpen;
    
    public boolean isBODOpen;
    
    /** Empty order book: levels 1..ORDER_BOOK_DEPTH with zero price and quantity. */
    public static BestBidOffVO[] getInitailBestBidsOffers() {
        BestBidOffVO[] levels = new BestBidOffVO[DBManagerMarket.ORDER_BOOK_DEPTH];
        Arrays.setAll(levels, i -> new BestBidOffVO(i + 1, 0.0, 0, 0));   // as fast as a loop; a Stream was ~70% slower
        return levels;
    }
}
