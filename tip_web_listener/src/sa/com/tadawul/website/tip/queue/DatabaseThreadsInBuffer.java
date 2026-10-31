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
 * Name          :     FileName : AnalyserInBuffer.java
 *
 * Version             Date                     Author                  Change Description
 *----------------------------------------------------------------------------------------------
 * 1.0                 10/April/2009            Oracle                   Initial Version
 * 1.1				   14/November/2009         Mohanram,Oracle          Added functionalities to process the Equity Rights Share
 * 1.2				   02/February/2010         Mohanram,Oracle          Added functionalities to process the Exchange Traded Funds.	

 *----------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.queue;

import com.objectspace.jgl.Queue;

public class DatabaseThreadsInBuffer {
    private Queue qBuffer;
    public DatabaseThreadsInBuffer() {
        qBuffer = new Queue();
    }
    
    /**
     * This method is called to write the message to queue
     * @param oFrame
     */
    public void setFrame(FrameHolder oFrame) {
        qBuffer.push(oFrame);
    }
    
    /**
     * This method is called to read the message from queue
     */
    public FrameHolder getFrame() {
        Object obj = qBuffer.pop();
        return (FrameHolder)obj;
    }
    
    /**
     * This method is called to get the queue size
     * @param oFrame
     */
    public int size() {
        return qBuffer.size();
    }
    
    
}