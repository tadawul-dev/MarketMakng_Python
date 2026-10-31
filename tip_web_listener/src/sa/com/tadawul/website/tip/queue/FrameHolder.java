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
 * Name          :     FileName : FrameHolder.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	
 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.queue;


public class FrameHolder {
    private long g_lTime;
    private String g_buffer;
    private int g_frameNo;
    private long g_readerStartTime;
    private long g_readerEndTime;
    
    public FrameHolder(long lTime, String buffer,int inFrameNo, long inReaderStartTime, long inReaderEndTime) {
        g_lTime = lTime;
        g_buffer = buffer;
        g_frameNo = inFrameNo;
        g_readerStartTime = inReaderStartTime;
        g_readerEndTime = inReaderEndTime;
    }
    
    /**
     * getter method
     */
    public String getBuffer() {
        return g_buffer;
    }
    
    /**
     * setter method
     * @param g_buffer
     */
    public void setBuffer(String g_buffer) {
        this.g_buffer = g_buffer;
    }
    
    /**
     * getter method
     */
    public long getTime() {
        return g_lTime;
    }
    
    /**
     * setter method
     * @param lTime
     */
    public void setTime(long lTime) {
        g_lTime = lTime;
    }
    
    /**
     * getter method
     */
    public int getG_frameNo() {
        return g_frameNo;
    }
    
    /**
     * setter method
     * @param no
     */
    public void setG_frameNo(int no) {
        g_frameNo = no;
    }
    
    /**
     * getter method
     */
    public long getG_readerStartTime() {
        return g_readerStartTime;
    }
    
    /**
     * setter method
     * @param startTime
     */
    public void setG_readerStartTime(long startTime) {
        g_readerStartTime = startTime;
    }
    
    /**
     * getter method
     */
    public long getG_readerEndTime() {
        return g_readerEndTime;
    }
    
    /**
     * setter method
     * @param endTime
     */
    public void setG_readerEndTime(long endTime) {
        g_readerEndTime = endTime;
    }
}