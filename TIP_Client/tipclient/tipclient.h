/**
 * This is a sample TIP client.
 *
 * Copyright (c) 2008 OMX AB.
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OMX AB ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL OMX AB BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

#ifndef tipspy_h
#define tipspy_h

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
//#include <assert.h>
#include <limits.h>
#include <errno.h>
#include <netinet/in.h>

#include <unistd.h>
#include <signal.h>
#include <time.h>

#include "xmapi.h"

#include "tiputils.h"

/*****************************************************************
 * Definitions
 */

#define USAGE_STRING \
"usage is: tipclient -a address -u uid -p pwd {-R|-B} [-s scope] [-v] [-d YYYYMMDD] [-x protversion] [-b n] [-e n] [-z][-q queuealert]\n\n"\
"\t-a address       DDS address hostname:port. Specify multiple addresses to enable fail-over on error.\n"\
"\t-u uid           Account name to use when connecting.\n"\
"\t-p pwd           Password to use when connecting.\n"\
"\t-R               Request realtime (RTr) data\n"\
"\t-B               Request basic (BDa) data}\n"\
"\t-s scope         Basic data scope (numeric value).\n"\
"\t-v               Increase verbosity level; each occurence of this argument\n"\
"\t                 increases the amount of messages displayed.\n"\
"\t-d YYYYMMDD      Use this date when requesting data.\n"\
"\t-x protversion   Specify protocol version. Default is no protocol version.\n"\
"\t-b n             Recover from this sequence number.\n"\
"\t-e n             Recover to this sequence number.\n"\
"\t-z               Enable compression.\n"\
"\t-q queuealert    Queue alert treshold, ms.\n%s\n"

/*****************************************************************
 * Type definitions
 */

/**
 * Request type
 */
typedef enum
  {
    Req_Realtime = 1,
    Req_BasicData = 2
  }
RequestType;

/**
 * Global data
 */
typedef struct
{
  const char*     userid;
  const char*     password;
  char*           primaryAddress;
  const char*     protocolVersion;
  FILE*           fp;
  long long       quantaStart;
  unsigned long   seqno;
  unsigned long   recoverFrom;
  unsigned long   recoverTo;
  int             fromDate;
  int             scope;
  int             verbosity;
  int             queueAlert;
  int             connectTimeout;
  int             connectInterval;
  int             recvTimeout;
  Boolean         connected;
  Boolean         compression;
  Boolean         pendingLogoff;
  XMP_SS          sessionState;
  Int32           extHdrLen;
  RequestType     request;
  XM_Service      svc;
  XM_Session      session;
  Byte            extHdr[XMP_MaxExtHdrSize];
  TIP_Msg         currMsg;
  char            outputfile[PATH_MAX+1];
}
GlobalData;

/*****************************************************************
 * Function prototypes
 */
GlobalData* g;

#define FUNCTION_INITIALIZE Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeInitialize
#define FUNCTION_GET_ERROR_MSG Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeGetErrorMessage
#define FUNCTION_CONNECT Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeConnect
#define FUNCTION_LOGON Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeLogon
#define FUNCTION_STREAMING_REQUEST Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeStreamingRequest
#define FUNCTION_GET_NEXT_MSG Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeGetNextMessage
#define FUNCTION_LOGOFF Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeLogoff
#define FUNCTION_GET_SIGNAL_RECEIVED Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeGetSignalReceived
#define FUNCTION_DISCONNECT Java_sa_com_tadawul_applications_tip_isi_efp_analyser_AnalyserReader_CNativeDisconnect

int        GetCurrentDate();

#endif
