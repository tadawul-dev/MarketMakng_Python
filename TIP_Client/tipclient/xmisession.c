#include "xm.h"
#include "xmsession.h"

#if defined WIN32
#include <winsock2.h>
#include <windows.h>
#include <io.h>
#else
#include <netdb.h>
#include <fcntl.h>
#include <sys/poll.h>
#endif

static Boolean XMI_NonBlockSocket (XM_Session, Int32* OldVal);
static Boolean XMI_RestoreSocket (XM_Session, Int32 Val);
static Int32   XMI_SelectSocket (XM_Session, XM_Timespan Timeout);

#if defined WIN32
#define CONNECTERROR(x) \
   (x == SOCKET_ERROR && WSAGetLastError () != WSAEWOULDBLOCK)
#define SOCKWRITE(s,p,l)    send ((SOCKET) s, (const char*) p, (int) l, 0)
#define SOCKCLOSE(s)        (void)closesocket (s)
#else
#define CONNECTERROR(x)     (x < 0 && errno != EINPROGRESS)
#define SOCKWRITE(s,p,l)    write (s, p, (size_t) l)
#define SOCKCLOSE(s)        (void)close(s)
#endif

/*
#if defined (Solaris) && ! defined (_SOCKLEN_T) && ! defined (_XPG4_2)
typedef Int32  socklen_t;
#endif

#if (defined (_XOPEN_SOURCE_EXTENDED))
typedef size_t socklen_t;
#endif

#if (defined (Solaris) && ! defined (_SOCKLEN_T)) && defined (_XPG4_2)
typedef size_t socklen_t;
#endif
*/

/************************************************************
 * XMI_Connect
 ************************************************************/

#define FUNCTION "XMI_Connect"

#define RECEIVEBUFFERSIZE 102400 // 100KB 1048576	// 1MB

XM_ErrorCode
XMI_Connect (XM_Session Session, const char* Host,
             Int32 DestPort, Int32 SourcePort)
{
   Int32               YesValue;
   struct sockaddr_in  SockAddress;
   Uint32              InAddr;
   Int32               OldVal = 0;
#if defined (RECEIVEBUFFERSIZE)
   Int32               ReceiveBufferSize;
#endif

   if ((Session->FD = socket (AF_INET, SOCK_STREAM, 0)) < 0)
   {
      XMI_Print ("%s: socket failed: %s", FUNCTION, strerror (errno));
      return XME_NetworkError;
   }

   YesValue = 1;
   if (setsockopt (Session->FD, SOL_SOCKET, SO_REUSEADDR,
                   (char*) & YesValue, sizeof (YesValue)) < 0)
   {
      XMI_Print ("%s: setsockopt SO_REUSEADDR failed: %s", FUNCTION,
                 strerror (errno));
      SOCKCLOSE (Session->FD);
      return XME_NetworkError;
   }

   if (SourcePort != XM_AnySourcePort)
   {
      struct sockaddr_in  MyAddr;

      MyAddr.sin_family = AF_INET;
      MyAddr.sin_addr.s_addr = ntohl (INADDR_ANY);
      MyAddr.sin_port = htons ((Uint16) SourcePort);

      if (bind (Session->FD, (struct sockaddr*) & MyAddr, sizeof (MyAddr)) < 0)
      {
         XMI_Print ("%s: bind failed: %s", FUNCTION, strerror (errno));
         SOCKCLOSE (Session->FD);
         return XME_NetworkError;
      }
   }

#if defined (RECEIVEBUFFERSIZE)
   ReceiveBufferSize = RECEIVEBUFFERSIZE;
   if (setsockopt (Session->FD, SOL_SOCKET, SO_RCVBUF,
                   (char*) & ReceiveBufferSize, sizeof (Int32)) < 0)
   {
      XMI_Print ("%s: setsockopt SO_RCVBUF failed: %s", FUNCTION,
                 strerror (errno));
      SOCKCLOSE (Session->FD);
      return XME_NetworkError;
   }
#endif
   //fprintf(stderr,"XMI_Connect : After setsockopt\n");fflush(stderr); // 02/Apr/2009

   if ((int) (InAddr = inet_addr (Host)) == -1)
   {
      struct hostent*  HostEnt;

      if ((HostEnt = gethostbyname (Host)) == NULL)
      {
         XMI_Print ("%s: Hostname=%s unavailable: %d",
                    FUNCTION, Host, h_errno);
         SOCKCLOSE (Session->FD);
         return XME_NetworkError;
      }

      /* Fixme: IPv6 not handled here now */
      assert (HostEnt->h_addrtype == AF_INET);
      assert (HostEnt->h_length == sizeof (Uint32));
      InAddr = * (Uint32*) HostEnt->h_addr;
   }

   if (Session->ConnectTimeout != XM_NoTimeout)
   {
      if (! XMI_NonBlockSocket (Session, & OldVal))
      {
         SOCKCLOSE (Session->FD);
         return XME_NetworkError;
      }
   }

   //fprintf(stderr,"XMI_Connect : After XMI_NonBlockSocket\n");fflush(stderr);	// 02/Apr/2009


   SockAddress.sin_family = AF_INET;
   /* InAddr is in network byte order already */
   SockAddress.sin_addr.s_addr = InAddr;
   SockAddress.sin_port = htons ((Uint16) DestPort);

   if (CONNECTERROR (connect (Session->FD, (struct sockaddr*) & SockAddress,
                              sizeof (SockAddress))))
   {
     // 		fprintf(stderr,"XMI_Connect : After connect\n");fflush(stderr);	// 02/Apr/2009

#if defined WIN32
      XMI_Print ("%s: connect %s:%d failed: %d\n", FUNCTION,
                 Host, DestPort, WSAGetLastError ());
#else
      XMI_Print ("%s: connect %s:%d failed: %s\n", FUNCTION,
                 Host, DestPort, strerror (errno));
#endif
      SOCKCLOSE (Session->FD);
      return XME_NetworkError; /* FIXME: Separate error code for conn fail */
   }

   if (Session->ConnectTimeout != XM_NoTimeout)
   {
      if (! XMI_SelectSocket (Session, Session->ConnectTimeout))
      {
         SOCKCLOSE (Session->FD);
         return XME_NetworkError;
      }
      if (! XMI_RestoreSocket (Session, OldVal))
      {
         SOCKCLOSE (Session->FD);
         return XME_NetworkError;
      }
   }

   Session->Used = 0;
   Session->ReadIndex = 0;
   Session->WriteIndex = 0;
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_Disconnect
 ************************************************************/

#define FUNCTION "XMI_Disconnect"

XM_ErrorCode
XMI_Disconnect (XM_Session Session)
{
   SOCKCLOSE (Session->FD);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_SendMessage
 ************************************************************/

#define FUNCTION "XMI_SendMessage"

XM_ErrorCode
XMI_SendMessage (XM_Session Session, const Byte* Message)
{
   const Byte*  Ptr;
   Int32        Len;

   Len = XMI_GetFrameLen (Message);

   Ptr = Message;

   while (Len > 0)
   {
      Int32  BytesWritten;

      if ((BytesWritten = SOCKWRITE (Session->FD, Ptr, Len)) <= 0)
      {
         XMI_Print ("%s: write fd=%d failed: %s\n", FUNCTION,
                  Session->FD, strerror (errno));
         return XME_NetworkError;
      }

      Ptr += BytesWritten;
      Len -= BytesWritten;
   }

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_RecvMessages
 ************************************************************/

#define FUNCTION "XMI_RecvMessages"

#define MSECS_PER_SEC 1000

Int32
XMI_RecvMessages (XM_Session Session, Byte* Buffer [], Int32 Max,
                  XM_Timespan Timeout)
{
   Int32          BytesRead;
   Int32          Pos;

   /*
    * This function minimises the number of (system) calls to recv/read by
    * reading all available data (up to XMI_ReadBufferSize) into an internal
    * per-session buffer. If there is already a message in the buffer,
    * XMI_Session_Recv returns that message immediately.
    */

   /* First test: XMI_GetFrameLen may not be evaluated if ReadBuffer doesn't
      contain a complete XMP header */
   while (! XMI_MessageInBuffer (Session))
   {
      if (Session->ReadIndex == Session->WriteIndex)
      {
         assert (Session->Used == 0);
         Session->ReadIndex = 0;
         Session->WriteIndex = 0;
      }
      else if (Session->WriteIndex > (Int32) (XMI_ReadBufferSize / 2))
      {
         (void) memmove (Session->ReadBuffer,
                         Session->ReadBuffer + Session->ReadIndex,
                         (size_t) Session->Used);
         Session->WriteIndex = Session->Used;
         Session->ReadIndex = 0;
      }

#if defined WIN32
      if (Timeout >= 0)
      {
         struct timeval  SelectTimeout;
         fd_set          Mask;

         FD_ZERO (& Mask);
         FD_SET ((u_int)Session->FD, & Mask);
         SelectTimeout.tv_sec = Timeout;
         SelectTimeout.tv_usec = 0;
         if (select (-1 /* Not used in Windows Sockets */, & Mask, NULL, NULL,
                     & SelectTimeout) == 0)
         {
            return (Int32) XME_Timeout;
         }
      }
      BytesRead = recv ((SOCKET) Session->FD,
                        Session->ReadBuffer + Session->WriteIndex,
                        XMI_ReadBufferSize - Session->WriteIndex, 0);
#else
      if (Timeout >= 0)
      {
         struct pollfd  Descriptor;

         Descriptor.fd = Session->FD;
         Descriptor.events = POLLRDNORM;
         Descriptor.revents = 0;

         if (poll (& Descriptor, 1, Timeout * MSECS_PER_SEC) == 0)
            return (Int32) XME_Timeout;
      }
      BytesRead = read (Session->FD, Session->ReadBuffer + Session->WriteIndex,
                        XMI_ReadBufferSize - (size_t) Session->WriteIndex);
#endif

      if (BytesRead <= 0)
      {
         if (BytesRead < 0 && errno != EINTR)
            XMI_Print ("%s: FD=%d error: %s\n", FUNCTION, Session->FD,
                       strerror (errno));

         return (Int32) XME_NetworkError;
      }

      Session->Used += BytesRead;
      Session->WriteIndex += BytesRead;
   }

   /*
    * There is at least one message now
    */

   for (Pos = 0; Pos < Max;)
   {
      Int32  Len;

      Len = XMI_GetFrameLen (Session->ReadBuffer + Session->ReadIndex);
      (void) memcpy (Buffer [Pos ++], Session->ReadBuffer + Session->ReadIndex,
                     (size_t) Len);
      Session->Used -= Len;
      Session->ReadIndex += Len;

      if (! XMI_MessageInBuffer (Session))
         break;
   }

   return Pos;
}

#undef FUNCTION

/************************************************************
 * XMI_SetRecvBufferSize
 ************************************************************/

#define FUNCTION "XMI_SetRecvBufferSize"

XM_ErrorCode
XMI_SetRecvBufferSize (XM_Session Session, Int32 BufferSize)
{
#if !defined WIN32
   Int32               ReceiveBufferSize;

   ReceiveBufferSize = BufferSize*1024;
   if (setsockopt (Session->FD, SOL_SOCKET, SO_RCVBUF,
                   (char*) & ReceiveBufferSize, sizeof (Int32)) < 0)
   {
      XMI_Print ("%s: setsockopt SO_RCVBUF failed: %s", FUNCTION,
                 strerror (errno));
      return XME_NotSupported;
   }
#endif

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_NonBlockSocket
 ************************************************************/

#define FUNCTION "XMI_NonBlockSocket"

static Boolean
XMI_NonBlockSocket (XM_Session Session, Int32* OldVal)
{
#if !defined WIN32
   if ((* OldVal = fcntl (Session->FD, F_GETFL)) < 0)
   {
      XMI_Print ("%s: fcntl failed: %s", FUNCTION, strerror (errno));
      SOCKCLOSE (Session->FD);
      return FALSE;
   }
   if (fcntl (Session->FD, F_SETFL, * OldVal | O_NONBLOCK) < 0)
   {
      XMI_Print ("%s: fcntl failed: %s", FUNCTION, strerror (errno));

      SOCKCLOSE (Session->FD);
      return FALSE;
   }
#else
   unsigned long  BlockingMode = 1;
   if (ioctlsocket (Session->FD, FIONBIO, & BlockingMode) == SOCKET_ERROR)
   {
      XMI_Print ("%s: ioctlsocket failed: %d", FUNCTION, WSAGetLastError ());
      SOCKCLOSE (Session->FD);
      return FALSE;
   }
#endif

   return TRUE;
}

#undef FUNCTION

/************************************************************
 * XMI_RestoreSocket
 ************************************************************/

#define FUNCTION "XMI_RestoreSocket"

static Boolean
XMI_RestoreSocket (XM_Session Session, Int32 Val)
{
#if !defined WIN32
   if (fcntl (Session->FD, F_SETFL, Val) < 0)
   {
      XMI_Print ("%s: fcntl failed: %s", FUNCTION, strerror (errno));
      SOCKCLOSE (Session->FD);
      return FALSE;
   }
#else
   unsigned long  BlockingMode = 0;
   if (ioctlsocket (Session->FD, FIONBIO, & BlockingMode) == SOCKET_ERROR)
   {
      XMI_Print ("%s: ioctlsocket failed: %d", FUNCTION, WSAGetLastError ());
      SOCKCLOSE (Session->FD);
      return FALSE;
   }
#endif

   return TRUE;
}

#undef FUNCTION

/************************************************************
 * XMI_SelectSocket
 ************************************************************/

#define FUNCTION "XMI_SelectSocket"

static Int32
XMI_SelectSocket (XM_Session Session, XM_Timespan Timeout)
{
#if !defined WIN32
   struct pollfd  Descriptor;
   Int32          Count;

   Descriptor.fd = Session->FD;
   Descriptor.events = POLLWRNORM | POLLRDNORM;
   Descriptor.revents = 0;

   Count = poll (& Descriptor, 1, Timeout * MSECS_PER_SEC);
   if (Count < 1 || Descriptor.revents != POLLWRNORM)
   {
      if (Count >= 0)
      {
         Int32      Error;
         socklen_t  Len;

         Len = (socklen_t) sizeof (Error);
         if (getsockopt (Session->FD, SOL_SOCKET, SO_ERROR, (char*) & Error,
                         & Len) >= 0)
            errno = Error;
      }

      XMI_Print ("%s: poll failed: %s", FUNCTION, strerror (errno));
      SOCKCLOSE (Session->FD);
      return FALSE;
   }
#else
   Int32           Count;
   fd_set          WriteMask;
   struct timeval  TimeOut;

   FD_ZERO (& WriteMask);
   FD_SET ((Uint32)Session->FD, & WriteMask);

   TimeOut.tv_usec = 0;
   TimeOut.tv_sec = Timeout;

   Count = select (-1, NULL,  & WriteMask, NULL, & TimeOut);
   if (Count == SOCKET_ERROR || FD_ISSET (Session->FD, & WriteMask) == 0)
   {
      XMI_Print ("%s: select failed: %d", FUNCTION, WSAGetLastError ());
      SOCKCLOSE (Session->FD);
      return FALSE;
   }
#endif

   return TRUE;
}

#undef FUNCTION

