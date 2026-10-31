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

/*****************************************************************
 * Definitions
 */

/*****************************************************************
 * Include files
 */

#include "tipclient.h"
#include <jni.h>

/*****************************************************************
 * Type definitions
 */


/*****************************************************************
 * Module variables
 */

static const char* 
stateNames[] =
  {
    "None",
    "Offline",
    "Catchup",
    "Online"
  };



int signalReceived = 0;


/*****************************************************************
 * Module function prototypes
 */

void        SetDefaults(void);
Boolean     Cleanup(void);
Boolean     ParseExtHdr(const Byte* xmp);

Boolean     ConnectSession(void);
Boolean     DisconnectSession(void);
Boolean     Send(const char* buffer);
const char* Recv(Int32* bodyLen);

const char* Logon(void);
Boolean     Logoff(void);
Boolean     StreamingRequest(void);

void SignalStop(int sig);

/*****************************************************************
 * Functions start here
 */

/**
 * First Function to be called from Java to initialize the settings
 */

JNIEXPORT jint JNICALL FUNCTION_INITIALIZE
  (JNIEnv *je, jclass jc)
{
  XM_ErrorCode Code;

  //fprintf(stdout,"C Library: Init Function Called\n");fflush(stdout);
  memset(ErrMsg,0,sizeof(ErrMsg));
  
  signal(SIGINT,SignalStop);
  signal(SIGHUP,SignalStop);
  signal(SIGTSTP,SignalStop);

  g = (GlobalData*) malloc(sizeof(GlobalData));
  assert(g != NULL);
  
  Code = XM_Init (fileno (stderr));
  assert (Code == XME_NoError);

  SetDefaults();

  //fprintf(stdout,"C Library: After SetDefaults\n");fflush(stdout);

  return TRUE;
}


JNIEXPORT jint JNICALL FUNCTION_GET_SIGNAL_RECEIVED
  (JNIEnv *je, jclass jc)
{
	return signalReceived;
}
/**
 * Set default values
 */

void  
SetDefaults(void)
{
  memset(g, 0, sizeof(GlobalData));

  g->queueAlert      = 500;
  g->seqno           = 1;
  g->recoverFrom     = 1;
  g->scope           = TIP_SCOPE_ALL;
  g->connectTimeout  = 30;
  g->connectInterval = 5;
  g->recvTimeout     = 35;
  g->fromDate        = GetCurrentDate();
}

/**
 * Return the current date
 */

int
GetCurrentDate()
{
  time_t     t   = 0;
  struct tm  tm;

  t = time(NULL);

  localtime_r(&t, &tm);

  return (tm.tm_year+1900) * 10000 + (tm.tm_mon+1) * 100 + tm.tm_mday;
}

JNIEXPORT jstring JNICALL FUNCTION_GET_ERROR_MSG
  (JNIEnv *je, jclass jc)
{
	/*static jstring jstr = NULL;
	if (jstr != NULL)
	{
		(*je)->ReleaseStringUTFChars(je,jstr,ErrMsg);
	}
	jstr =(*je)->NewStringUTF(je,ErrMsg);
	return jstr;*/
	return (*je)->NewStringUTF(je,ErrMsg);
}

JNIEXPORT jint JNICALL FUNCTION_CONNECT
  (JNIEnv *je, jclass jc, jstring jHost, jint jPort, jint jCompression)
{
  int	i=0, j=0, k=0;
  int  port = 0;
  const char *hostname;

  jboolean iscopy;
  
  memset(ErrMsg,0,sizeof(ErrMsg));
  
  //fprintf(stdout,"C Library: Connect Function Called\n");fflush(stdout);

 	if ( g->connected )
  {
		strcpy(ErrMsg,"Already connected");
		return FALSE;
	}
  
  hostname = (*je)->GetStringUTFChars(je, jHost, &iscopy);
	if (hostname == NULL)
	{
		strcpy(ErrMsg,"IP Address passed is null");
		return FALSE;
	}
  
	if (jPort <= 0)
	{
		strcpy(ErrMsg,"Port passed is zero");
		return FALSE;
	}
  port = jPort;
  
  if (jCompression != 0 && jCompression != 1)
	{
		strcpy(ErrMsg,"Compression flag cane be zero or one only");
		return FALSE;
	}
	g->compression = jCompression;

  //fprintf(stdout,"From C: Host %s Port %d Compression Flag \n",hostname,port,jCompression);fflush(stdout);

  assert( XM_Service_Init(& g->svc) == XME_NoError );
  assert( XM_Service_AddEntry(g->svc, hostname, port) == XME_NoError );

	if (ConnectSession()) 
		return TRUE;

  assert( XM_Service_Destroy(g->svc) == XME_NoError );
  return FALSE;
}

/**
 * Connect, logon and send a streaming request. 
 *
 * First, the session is connected. If the primary 
 * address is not available, XMAPI will attempt to
 * connect to the secondary address, then back to
 * the primary and so on until a connection is
 * established.
 *
 * When a connection has been established, the session
 * is initialized with the specified queue alert and 
 * compression values. These values must be send *prior*
 * to the logon message.
 *
 * The server will respond to the logon message with a 
 * general reply message and zero or more extension headers.
 *
 * The general reply message is parsed and the error code 
 * checked.
 */

Boolean
ConnectSession(void)
{
  XM_ErrorCode error       = XME_NoError;
  int          queueAlert  = htonl(g->queueAlert);
  char         compression = XMP_CA_None;

  error = XM_Session_ConnectService(& g->session,
				    g->svc,
				    XM_SessionMode_Cooked,
				    NULL,
				    XM_AnySourcePort,
				    g->connectTimeout);
  if (error != XME_NoError)
  {
    sprintf(ErrMsg, "Connection failed: %s\n", XM_GetErrorMessage(error));

    return FALSE;
  }

  if (g->compression)
    compression = XMP_CA_LZ1;

  assert( XMP_AddExtHdrEntry (g->extHdr, 
			      & g->extHdrLen,
			      XMP_Tag_CompressionAlgorithm,
			      & compression, 
			      sizeof(compression)) == XME_NoError );
    
  assert( XMP_AddExtHdrEntry (g->extHdr, 
			      & g->extHdrLen,
			      XMP_Tag_QueueAlert,
			      & queueAlert, 
			      sizeof(queueAlert)) == XME_NoError );

	g->connected = TRUE;
  return TRUE;
}

JNIEXPORT jstring JNICALL FUNCTION_LOGON
  (JNIEnv *je, jclass jc, jstring jUserName, jstring jPassword, jstring jProtocolVersion)
{
  int	i=0, j=0, k=0;

  jboolean iscopy;
  const char* logonreply;

 	if (jUserName != NULL) 
 	{
		g->userid = (*je)->GetStringUTFChars(je, jUserName, &iscopy);
	}
	else
	{
			strcpy(ErrMsg,"UserName is NULL");
			return NULL;
	}
	
	if (jPassword != NULL)
	{
		g->password = (*je)->GetStringUTFChars(je, jPassword, &iscopy);
	}
	else
	{
		strcpy(ErrMsg,"Password is NULL");
		return NULL;
	}
		
	if (jProtocolVersion != NULL)
	{
		g->protocolVersion = (*je)->GetStringUTFChars(je, jProtocolVersion, &iscopy);
	}
	else
	{
		//fprintf(stdout,"Protocol Version is NULL\n");
		//return NULL;
	}

	
	logonreply = Logon();
	if (logonreply == NULL)
		return NULL;

	return (*je)->NewStringUTF(je,logonreply);
}

/**
 * Logon, read and parse the reply.
 */

const char*
Logon(void)
{
  Int32       replyLen = 0;
  char*       fval     = NULL;
  char*       text     = NULL;
  const char* reply    = NULL;
  static char        buffer[TIP_MAXMESSAGELENGTH];
  TIP_Msg     msg;
  
	memset(buffer,0,TIP_MAXMESSAGELENGTH);
  strcpy(buffer, "");
  AppendTag(buffer, TIP_MSGTYPE_LOGON, NULL);
  AppendTag(buffer, TIP_FTAG_USERID, g->userid);
  AppendTag(buffer, TIP_FTAG_PASSWORD, g->password);

  if (g->protocolVersion != NULL) 
    AppendTag(buffer, TIP_FTAG_PROTOCOLVERSION, g->protocolVersion);

  //fprintf(stderr, "Logon request: '%s'\n", buffer);

  strcat(buffer, TIP_ENDOFMSG_STRING);

  if (! Send(buffer))
  {
  	strcpy(ErrMsg,"Sending Logon request failed..");
    return NULL;
  }

  reply = Recv(& replyLen);
  if (reply == NULL)
  {
  	strcpy(ErrMsg,"Logon response received is NULL");
    return NULL;
  }

	memset(buffer,0,TIP_MAXMESSAGELENGTH);
  strncpy(buffer,reply,replyLen);
	return buffer;
}

JNIEXPORT jint JNICALL FUNCTION_STREAMING_REQUEST
  (JNIEnv *je, jclass jc, jint jReqType, jint jScope, jint jDate,jlong jStartSeq, jlong jEndSeq)
{
  	if (jReqType == 0)
  	{
  		strcpy(ErrMsg,"Request Type is not specified");
			return FALSE;
		}
		if (jStartSeq <= 0)
  	{
  		sprintf(ErrMsg,"StartSeq is not specified %d",jStartSeq);
			return FALSE;
		}
				
  	if (jEndSeq != 0 && jEndSeq < jStartSeq)
  	{
  		strcpy(ErrMsg,"End sequence less than Start sequence");
			return FALSE;
		}
		
		g->seqno = jStartSeq;
		g->recoverFrom = jStartSeq;
		g->recoverTo = jEndSeq;
		g->request = jReqType;
		g->fromDate = jDate;
		g->scope = jScope;
		
		return StreamingRequest();
}

/**
 * Send a streaming request.
 */

Boolean
StreamingRequest(void)
{
  char buffer[TIP_MAXMESSAGELENGTH];

  strcpy(buffer, "");

  if (g->request == Req_Realtime)
  {
    AppendTag(buffer, TIP_MSGTYPE_REALTIME, NULL);

    AppendIntTag(buffer, TIP_FTAG_STARTSEQNO, g->seqno);

    if (g->recoverTo != 0)
      AppendIntTag(buffer, TIP_FTAG_ENDSEQNO, g->recoverTo);

    if (g->fromDate != 0)
      AppendIntTag(buffer, TIP_FTAG_REQUESTDATE, g->fromDate);

    AppendIntTag(buffer, TIP_FTAG_SCOPE, g->scope);
  }
  else
  {
    AppendTag(buffer, TIP_MSGTYPE_BASICDATA, NULL);

    if (g->fromDate != 0)
      AppendIntTag(buffer, TIP_FTAG_REQUESTDATE, g->fromDate);

    AppendIntTag(buffer, TIP_FTAG_SCOPE, g->scope);
  }

  
  strcat(buffer, TIP_ENDOFMSG_STRING);

  if (! Send(buffer))
    return FALSE;

  return TRUE;
}

/**
 * Return the next message from the session. If the session is not
 * connected, attempt to connect.
 *
 * Note that the message is copied to the local buffer here; we do
 * not want to access or (risk to) modify the pointer to the body,
 * since it's owned by XMAPI.
 */

JNIEXPORT jstring JNICALL FUNCTION_GET_NEXT_MSG
  (JNIEnv *je, jclass jc)
{
  const char* body = NULL;
  Int32 bodyLen = 0;
  static char mybuf[TIP_MAXMESSAGELENGTH];

  memset(ErrMsg,0,sizeof(ErrMsg));
  /*static jstring tipMessage = NULL;

  if (tipMessage != NULL)
  {
		(*je)->ReleaseStringUTFChars(je,tipMessage,mybuf);
  }
  */

  if (signalReceived)
  {
	sprintf(ErrMsg,"Signal recived...quiting GetNextMessage");
	return NULL;
  }

	
  if  (!g->connected)
  {
    sprintf(ErrMsg,"No connection available to read next message");
    return NULL;
  }

	
  body = Recv(& bodyLen);


  if (body == NULL)
  {
	sprintf(ErrMsg,"Recv failed.....");
  	return NULL;
  }
  
	memset(mybuf,0,TIP_MAXMESSAGELENGTH);
    strncpy(mybuf,body,bodyLen);
  /*tipMessage =(*je)->NewStringUTF(je,mybuf);
	return tipMessage;*/
	return (*je)->NewStringUTF(je,mybuf);

}

/**
 * Logoff, read and parse the reply.
 */
 
JNIEXPORT jint JNICALL FUNCTION_LOGOFF
  (JNIEnv *je, jclass jc)
{
	return Logoff();
}


Boolean
Logoff(void)
{
  Int32       replyLen = 0;
  char*       fval     = NULL;
  const char* reply    = NULL;
  char*       text     = NULL;
  char        buffer[TIP_MAXMESSAGELENGTH];
  TIP_Msg     msg;
  int MsgCnt = 0 ;
  int TotalBufferLen = 0;
  int loggedOffFlag = FALSE;

  strcpy(buffer, "");
  AppendTag(buffer, TIP_MSGTYPE_LOGOFF, NULL);
  strcat(buffer, TIP_ENDOFMSG_STRING);

  //fprintf(stderr, "Logoff request: %s", buffer);

  signalReceived = 0;

  if (! Send(buffer))
  {
  	strcpy(ErrMsg,"Unable to send Logoff Message");
    return FALSE;
  }

  return TRUE;
}

/**
 * Send a message. Include extension headers.
 */

Boolean
Send(const char* tipMsg)
{
  XM_ErrorCode error = XME_NoError;
  size_t       len   = 0;
  Byte*        ptr   = NULL;
  Byte         xmp[XMP_MaxMessageLength];

  if (g->extHdrLen > 0)
    ptr = g->extHdr;

  len = strlen(tipMsg);

  assert( XMP_CreateMessage(xmp, 
			    XMP_HeaderSize + XMP_MaxExtHdrSize,
			    XMP_Type_BinaryRecord, 
			    ptr,
			    g->extHdrLen, 
			    tipMsg, 
			    len) == XME_NoError );

  /*if (g->verbosity >= 1)
    fprintf(stderr, "Send: extension header%d bytes: body length %d bytes: frame length %d bytes\n",
	    g->extHdrLen,
	    len,
	    XMP_HeaderSize + g->extHdrLen + len);
  */
  g->extHdrLen = 0;

  error = XM_Session_Send(g->session, xmp);
  if (error != XME_NoError)
  {
    sprintf(ErrMsg, "Failed to send: %s\n", XM_GetErrorMessage(error));

    DisconnectSession();

    return FALSE;
  }
  return TRUE;
}

/**
 * Receive a message. Parse extension headers. Note that the
 * receive is done within a loop. We want this function to return
 * a TIP message, but the server may send messages with body type = none
 * containing only extension headers and no body.
 */

const char* 
Recv(Int32* bodyLen)
{
  XM_ErrorCode error     = XME_NoError;
  const Byte*  body      = NULL;
  int          extHdrLen = 0;
  Byte         xmp[XMP_MaxMessageLength];

  while ( TRUE )
  {
    error = (XM_ErrorCode) XM_Session_Recv(g->session, xmp);
    if (error  <= 0)
    {
      sprintf(ErrMsg, "Failed to receive: %s\n", XM_GetErrorMessage(error));

      DisconnectSession();

      return NULL;
    }
    
    extHdrLen = XMP_GetExtHeaderLen(xmp);

    body = (const unsigned char*) XMP_GetBody(xmp, bodyLen);

    /* if (g->verbosity >= 1)
      	fprintf(stderr, "Recv: body type %d: extension header %d bytes: body %d bytes: frame length %d bytes body : %s\n",
		      XMP_GetBodyType(xmp),
		      extHdrLen,
		      *bodyLen,
		      XMP_HeaderSize + extHdrLen + *bodyLen, body);
	  */

    if (extHdrLen > 0)
    {
      ParseExtHdr(xmp);
    }

    if (*bodyLen > 0)
      break;
  }

  return (const char*) body;
}

/**
 * Parse extension headers
 */

Boolean
ParseExtHdr(const Byte* xmp)
{
  XM_ErrorCode error           = XME_NoError;
  Int32        xmpExtensionLen = 0;
  Int32        xmpOffset       = 0;
  Int32        queueAlert      = 0;
  const char*  xmpExtension    = NULL;
  char*        ptr             = NULL;
  XMP_Tag      xmpExtensionTag = XMP_Tag_None;
  Int32        tsp[2];
  char         tspBuf [26];
  struct tm    tm;

  while ((error = XMP_GetExtHdrEntry (xmp, 
				      & xmpOffset, 
				      & xmpExtensionTag,
				      (const void**) & xmpExtension,
				      & xmpExtensionLen)) == XME_NoError)
  {
    switch (xmpExtensionTag)
    {
      /*
       * If timestamps are sent (normally they're not), there will be
       * one timestamp per frame. To limit the output to the console,
       * only display the timestamp when the verbosity level is fairly
       * high. Also, localtime_r is a very slow function.
       */
    case XMP_Tag_Timestamp:
      if (g->verbosity >= 2)
      {
				memcpy (tsp, xmpExtension, sizeof(tsp));
				tsp[0] = ntohl (tsp[0]);
				tsp[1] = ntohl (tsp[1]);
				
				localtime_r((const time_t*) & tsp[0], & tm);
				
				strftime(tspBuf,
					 sizeof(tspBuf),
					 "%Y-%m-%d %H:%M:%S",
					 & tm);
				
				if ((ptr = strchr(tspBuf, '\n')) != NULL)
				  *ptr = '\0';
				
				fprintf(stderr, "Extension header: timestamp: %s ; %d\n", tspBuf, tsp[1]); fflush(stderr);
      }
      break;
      
      /**
       * Queue alerts are sent when we cross the queue alert threshold.
       */
    case XMP_Tag_QueueAlert:
      memcpy (& queueAlert, xmpExtension, sizeof(queueAlert));

      queueAlert = ntohl(queueAlert);
      
      fprintf(stderr, "Extension header: queue alert %d msec\n", queueAlert); fflush(stderr);

      break;
      
    /**
     * Session states are sent when the session transitions from e.g. 
     * initiailzed to catch-up, catch-up to online etc. We don't really
     * need to keep track of the state, but the information is useful
     * nevertheless.
     */
  case XMP_Tag_SessionState:
    g->sessionState = (XMP_SS) ((char) (xmpExtension[0]));
    switch (g->sessionState)
    {
    case XMP_SS_None:
    case XMP_SS_Offline:
    case XMP_SS_Catchup:
    case XMP_SS_Online:
      fprintf(stderr, "Extension header: session state: %s\n", stateNames[g->sessionState]); fflush(stderr);
      break;
      
    default:
      fprintf(stderr, "Extension header: session state: 0x%x\n", g->sessionState); fflush(stderr);
      break;
    }
    break;
    
    default:
      break;
    }
  }
  
  if (error != XME_NoMoreEntries)
  {
    fprintf(stderr,  "Error parsing extension headers: %s\n", XM_GetErrorMessage(error)); fflush(stderr);
    
    return FALSE;
  }
  
  return TRUE;
}

void SignalStop(int sig)
{
	memset(ErrMsg,0,sizeof(ErrMsg));
	signalReceived = sig;
	sprintf(ErrMsg,"Signal Received : %d\n", sig);
	//fprintf(stdout, "Signal Received : %d\n", sig); fflush(stdout);
}

/**
 * Cleanup on exit
 */

Boolean 
Cleanup(void)
{
  if (g->fp != NULL)
    fclose(g->fp);

  DisconnectSession();

  free(g);
}

/**
 * Disconnect the session
 */

JNIEXPORT jint JNICALL FUNCTION_DISCONNECT
  (JNIEnv *je, jclass jc)
{
	return DisconnectSession();
}

Boolean
DisconnectSession(void)
{
  if (! g->connected)
    return;

  XM_Session_Disconnect(g->session);

  assert( XM_Service_Destroy(g->svc) == XME_NoError );

  g->connected = FALSE;
  
  return TRUE;
}

