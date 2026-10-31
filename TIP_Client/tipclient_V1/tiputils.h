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

#ifndef tiputils_h
#define tiputils_h

/*****************************************************************
 * Definitions
 */
#define TRUE                     1
#define FALSE                    0

/**
 * Definition of byte.
 */
typedef unsigned char Byte;

/**
 * Definition of boolean
 */
typedef unsigned char Boolean;

#define TIP_INDEX_LOGFILE		"TIP_Index.txt"
#define TIP_SHARE_LOGFILE		"TIP_Share.txt"

#define TIP_MAX_TAGS             256 /* dangerous */

#define MAX_INDICES				 20
#define MAX_SYMBOLS				 150
#define TIP_MAX_MSGTYPE_LEN      32 

#define TIP_MAXMESSAGELENGTH     2048
#define TIP_MAXSYMBOL_LEN		 32+1
#define TIP_MAXNAME_LEN			 80+1
#define TIP_DATETIME_LEN		 10+1

#define TIP_DELIMITER            ';'
#define TIP_DELIMITER_STRING     ";"
#define TIP_ENDOFMSG             '\n'
#define TIP_ENDOFMSG_STRING      "\n"
#define TIP_ESCAPE               '\\'
#define TIP_ESCAPE_STRING        "\\"

#define TIP_MSGTYPE_LOGON        "LGn"
#define TIP_MSGTYPE_LOGOFF       "LGf"
#define TIP_MSGTYPE_ENDOFSTREAM  "EOs"
#define TIP_MSGTYPE_GENERALREPLY "GEn"
#define TIP_MSGTYPE_HEARTBEAT    "HBt"
#define TIP_MSGTYPE_REALTIME     "RTr"
#define TIP_MSGTYPE_BASICDATA    "BDa"

#define TIP_FTAG_USERID          "UId"
#define TIP_FTAG_PASSWORD        "PWd"
#define TIP_FTAG_PROTOCOLVERSION "VEr"
#define TIP_FTAG_REPLYCODE       "Rc"
#define TIP_FTAG_REPLYTEXT       "Rx"
#define TIP_FTAG_SEQUENCENUMBER  "n"
#define TIP_FTAG_REQUESTDATE     "QDAy"
#define TIP_FTAG_SCOPE           "QSCo"
#define TIP_FTAG_STARTSEQNO      "Sn"
#define TIP_FTAG_ENDSEQNO        "En"
#define TIP_FTAG_REQUESTTYPE	 "Qt"

#define TIP_SCOPE_NONE           1
#define TIP_SCOPE_UPDATES        2
#define TIP_SCOPE_ALL            3

/*****************************************************************
 * Type definitions
 */

/**
 * Parsed TIP message 
 */

typedef struct
{
  char   inputmsg[TIP_MAXMESSAGELENGTH];
  char   buf[TIP_MAXMESSAGELENGTH*3/2];
  char   msgType[TIP_MAX_MSGTYPE_LEN];
  char*  tags[TIP_MAX_TAGS];
  char*  values[TIP_MAX_TAGS];
  int    numtags;
  int    length;
}
TIP_Msg;

void       AppendIntTag(char* msg, const char* tag, int num);
Boolean    AppendTag(char* msg, const char* tag, const char* tagparam);
Boolean    ParseMsg(TIP_Msg* msg, const char* buffer, int len); //reqd only for logoff response parsing
char*      FindTag(TIP_Msg* msg, const char* tag); //reqd only for logoff response parsing

#endif

