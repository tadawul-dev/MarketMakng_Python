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
 * Name          :     FileName : Buffer.java
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

//import com.objectspace.jgl.Queue;

public class Buffer {
    private Queue g_oBuffer;
    
    private Object g_sNextFrame = null;
    
    /**
     * Constructor
     */
    public Buffer() {
        g_oBuffer = new Queue();
    }
    
    /**
     * Add a frame to the buffer
     */
    public void setFrame(Object sFrame) {
        g_oBuffer.push(sFrame);
    }
    
    /**
     * Returns the topmost frame from the buffer. Current poped out frame is
     * kept in a temperory location untill it is fully consumed.
     */
    public Object getFrame() {
        if (g_sNextFrame == null) {
            g_sNextFrame = g_oBuffer.pop();
        }
        return g_sNextFrame;
    }
    
    /**
     * Indicates that the poped out frame is consumed.
     */
    public void releaseFrame() {
        g_sNextFrame = null;
    }
    
    /**
     * Returns the number of frames in the buffer.
     */
    public int size() {
        if (g_sNextFrame == null)
            return g_oBuffer.size();
        else
            return g_oBuffer.size() + 1;
    }
}
