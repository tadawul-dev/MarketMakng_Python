/**
 * ---------------------------------------------------------------------------------------------
 * (c) Copyright 2016 Tadawul - All Rights Reserved
 *
 *		http://www.tadawul.com.sa
 *
 * This source is property of Tadawul, Saudi Stock Exchange.
 * No part of this source or any copyrights subsisting or arising out of these materials may be
 * reproduced, distributed, amended, modified, adapted, stored in a retrieval system, adopted
 * or transmitted in any form or by any means, electronic, mechanical, photographic, graphic,
 * optic recording or otherwise, translated in any language or computer language, without the
 * prior written permission from Tadawul, Saudi Stock Exchange
 *
 * ---------------------------------------------------------------------------------------------
 *
 * File Name	: Log.java
 *
 * 
 * Version		Date				Author					Change Description
 * ---------------------------------------------------------------------------------------------
 * 1.0			Nov 20, 2016		Mohanram		Initial version
 * ---------------------------------------------------------------------------------------------
 */
package sa.com.tadawul.website.tip.log;

/**
 * @author Mohanram
 * 
 */
public interface Log {

	void serviceEntry(String serviceName);

	void serviceEntry(String serviceName, Object input);

	void serviceExit(String serviceName);

	void serviceExit(String serviceName, Object output);

	void methodEntry(String methodName);

	void methodEntry(String methodName, Object input);

	void methodExit(String methodName);

	void methodExit(String methodName, Object output);

	void fatal(String message);

	void fatal(String message, Throwable error);

	void error(String message);

	void error(String message, Throwable error);

	void info(String message);

	void info(Object message);

	void debug(String message);

	void debug(Object message);
}
