#include "xm.h"
#include "xmsession.h"
#ifndef WIN32
#include <pthread.h>
#endif

static XM_ErrorCode  XMI_Session_Init (XM_Session* Session,
                                       XM_SessionMode,
                                       XM_Crypt OptionalCrypt,
                                       XM_Timespan ConnectTimeout);
static void          XMI_FreeSession (XM_Session Session, XM_Crypt Crypt);
static XM_ErrorCode  XMI_KeyExchange (XM_Session Session, XM_Crypt Crypt);
static void          XMI_GenPermutation (Int32* Permutation, Int32 Count,
                                         XM_Service Service, unsigned int Seed);
static Int32         XMI_Uncook (XMI_CookedInfo Cooked);

/************************************************************
 * XMI_Session_Init
 ************************************************************/

#define FUNCTION "XMI_Session_Init"

static XM_ErrorCode
XMI_Session_Init (XM_Session* Session, XM_SessionMode Mode,
                  XM_Crypt OptionalCrypt, XM_Timespan ConnectTimeout)
{
   XM_Session    NewSession;
   XM_ErrorCode  Code;

   if (Session == NULL)
      return XME_BadParam;

   * Session = NULL;

   switch (Mode)
   {
    case XM_SessionMode_Cooked:
      break;
    case XM_SessionMode_Raw:
      if (OptionalCrypt != NULL)
         return XME_BadParam;
      break;
    default:
      return XME_BadParam;
   }

   NewSession = (XM_Session) malloc (sizeof (struct XMI_Session));
   if (NewSession == NULL)
      return XME_NoMemory;

   NewSession->Mode = Mode;

   if (Mode == XM_SessionMode_Cooked)
   {
      XMI_CookedInfo  Cooked;

      Cooked = (XMI_CookedInfo) malloc (sizeof (struct XMI_CookedInfoTag));
      if (Cooked == NULL)
      {
         XMI_FreeSession (NewSession, NULL);
         return XME_NoMemory;
      }

      Code = XM_DC_Init (& Cooked->DC, XMP_CA_LZ1);
      if (Code != XME_NoError)
      {
         XMI_FreeSession (NewSession, NULL);
         return Code;
      }

      Cooked->Buffer = Cooked->Buffers [0];
      Cooked->AltBuffer = Cooked->Buffers [1];
      Cooked->Decrypt = NULL;
      Cooked->Encrypt = NULL;
      Cooked->Xmp = NULL;
      Cooked->Used = 0;

      NewSession->Cooked = Cooked;
   }
   else
      NewSession->Cooked = NULL;

   NewSession->RecvTimeout = -1;
   NewSession->ConnectTimeout = ConnectTimeout;

   * Session = NewSession;
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_FreeSession
 ************************************************************/

#define FUNCTION "XMI_FreeSession"

static void
XMI_FreeSession (XM_Session Session, XM_Crypt Crypt)
{
   if (Session != NULL)
   {
      if (Session->Cooked != NULL)
      {
         (void) XM_DC_Destroy (Session->Cooked->DC);
         if (Session->Cooked->Decrypt != NULL)
            (void) XM_Crypt_Destroy (Session->Cooked->Decrypt);
         if (Session->Cooked->Encrypt != NULL)
            (void) XM_Crypt_Destroy (Session->Cooked->Encrypt);
         free (Session->Cooked);
      }
      free (Session);
   }

   if (Crypt != NULL)
      (void) XM_Crypt_Destroy (Crypt);
}

#undef FUNCTION

/************************************************************
 * XM_Session_ConnectService
 ************************************************************/

#define FUNCTION "XM_Session_ConnectService"

XM_ErrorCode
XM_Session_ConnectService (XM_Session* Session, XM_Service Service,
                           XM_SessionMode Mode, XM_Crypt OptionalCrypt,
                           Int32 SourcePort, XM_Timespan ConnectTimeout)
{
   XM_ErrorCode  Code;
   XM_Session    NewSession;
   Int32         Attempts;
   Int32         EntryPermutation [XMI_MaxServiceEntries];
   Int32         p1;

   if (Service == NULL)
   {
      XMI_FreeSession (NULL, OptionalCrypt);
      return XME_BadParam;
   }

   Attempts = XMI_Service_CountEntries (Service);
   if (Attempts <= 0)
   {
      XMI_FreeSession (NULL, OptionalCrypt);
      return (Attempts < 0) ? (XM_ErrorCode) Attempts : XME_NoServiceEntries;
   }

   Code = XMI_Session_Init (& NewSession, Mode, OptionalCrypt, ConnectTimeout);
   if (Code != XME_NoError)
   {
      XMI_FreeSession (NewSession, OptionalCrypt);
      return Code;
   }

   NewSession->Service = Service;

   if (Attempts == 1)
      EntryPermutation [0] = 0;
   else
   {
      /* Fixme: The seed could probably be improved. Most of all, would like
         to ensure that different session threads get different seeds even
         if they call ConnectService at the same time. */
      Uint32  Seed = SourcePort == 0 ? 0x5a5a5a5a : SourcePort & 0x5a5a0000;
#if !defined WIN32
      struct timeval  Tv;

      if (gettimeofday (& Tv, NULL) == 0)
         Seed = Seed ^ (Uint32) Tv.tv_sec ^ (Uint32)((Tv.tv_usec + 1) * 15259);
#else
      SYSTEMTIME  Time;

      GetLocalTime (& Time);
      Seed = Seed ^ time (NULL) ^ ((Time.wMilliseconds + 1) * 15259);
#endif

      XMI_GenPermutation (EntryPermutation, Attempts, Service, Seed);
   }

   for (p1 = 0; p1 < Attempts; p1 ++)
   {
      XMI_Address*  Address;

      NewSession->ServiceEntry = EntryPermutation [p1];
      Address = XMI_Service_GetEntryAddress (Service,
                                             NewSession->ServiceEntry);
      if (Address == NULL)
      {
         Code = XME_InternalError;
         break;
      }
      
			//fprintf(stderr,"Address Host : %s\n",Address->Host);fflush(stderr); // Guna - 02/Apr/2009
			
      Code = XMI_Connect (NewSession, Address->Host,
                          Address->Port, SourcePort);
      if (Code == XME_NoError)
      {
      		//fprintf(stderr,"XMI_Connect No Error Block\n");fflush(stderr); // 02/Apr/2009
         if (NewSession->Cooked != NULL && OptionalCrypt != NULL)
         {
            Code = XMI_KeyExchange (NewSession, OptionalCrypt);
            if (Code != XME_NoError)
            {
               XMI_FreeSession (NewSession, NULL);
               return Code;
            }
         }
         * Session = NewSession;
         return XME_NoError;
      }
   }

   XMI_FreeSession (NewSession, OptionalCrypt);
   return Code;
}

#undef FUNCTION

/************************************************************
 * XM_Session_ConnectAddress
 ************************************************************/

#define FUNCTION "XM_Session_ConnectAddress"

XM_ErrorCode
XM_Session_ConnectAddress (XM_Session* Session, const char* Host,
                           Int32 Port, XM_SessionMode Mode,
                           XM_Crypt OptionalCrypt, Int32 SourcePort,
                           XM_Timespan ConnectTimeout)
{
   XM_ErrorCode  Code;
   XM_Session    NewSession;

   Code = XMI_Session_Init (& NewSession, Mode, OptionalCrypt, ConnectTimeout);
   if (Code != XME_NoError)
   {
      XMI_FreeSession (NewSession, OptionalCrypt);
      return Code;
   }

   NewSession->Service = NULL;
   NewSession->ServiceEntry = -1;

   Code = XMI_Connect (NewSession, Host, Port, SourcePort);
   if (Code != XME_NoError)
   {
      XMI_FreeSession (NewSession, OptionalCrypt);
      return Code;
   }

   if (NewSession->Cooked != NULL && OptionalCrypt != NULL)
   {
      Code = XMI_KeyExchange (NewSession, OptionalCrypt);
      if (Code != XME_NoError)
      {
         XMI_FreeSession (NewSession, NULL);
         return Code;
      }
   }

   * Session = NewSession;
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_KeyExchange
 ************************************************************/

#define FUNCTION "XMI_KeyExchange"

static XM_ErrorCode
XMI_KeyExchange (XM_Session Session, XM_Crypt KeyExchangeCrypt)
{
   XM_ErrorCode  Code;

   Code = XM_Session_KeyExchange (Session, KeyExchangeCrypt,
                                  & Session->Cooked->Decrypt,
                                  & Session->Cooked->Encrypt);
   XMI_FreeSession (NULL, KeyExchangeCrypt);
   if (Code != XME_NoError)
   {
      XMI_Print ("%s: Key exchange failed: %s", FUNCTION,
                 XM_GetErrorMessage (Code));
      (void) XMI_Disconnect (Session);
   }

   return Code;
}

#undef FUNCTION

/************************************************************
 * XM_Session_Disconnect
 ************************************************************/

#define FUNCTION "XM_Session_Disconnect"

XM_ErrorCode
XM_Session_Disconnect (XM_Session Session)
{
   XM_ErrorCode  Code;

   if (Session == NULL)
      return XME_BadParam;

   Code = XMI_Disconnect (Session);
   XMI_FreeSession (Session, NULL);

   return Code;
}

#undef FUNCTION

/************************************************************
 * XM_Session_GetAddress
 ************************************************************/

#define FUNCTION "XM_Session_GetAddress"

XM_ErrorCode
XM_Session_GetAddress (XM_Session Session, const char** Host, Int32* Port)
{
   XMI_Address*  Address;

   if (Session == NULL || Host == NULL || Port == NULL)
      return XME_BadParam;

   Address = XMI_Service_GetEntryAddress (Session->Service,
                                          Session->ServiceEntry);
   if (Address == NULL)
      return XME_BadParam;

   * Host = Address->Host;
   * Port = Address->Port;

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_Session_SetRecvTimeout
 ************************************************************/

#define FUNCTION "XM_Session_SetRecvTimeout"

XM_ErrorCode
XM_Session_SetRecvTimeout (XM_Session Session, XM_Timespan Timeout)
{
   if (Session == NULL)
      return XME_BadParam;

   Session->RecvTimeout = Timeout;

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_Session_SetRecvBufferSize
 ************************************************************/

#define FUNCTION "XM_Session_SetRecvBufferSize"

XM_ErrorCode
XM_Session_SetRecvBufferSize (XM_Session Session,
                              Int32 BufferSize)
{
   if (Session == NULL)
      return XME_BadParam;

   return XMI_SetRecvBufferSize (Session, BufferSize);
}

#undef FUNCTION

/************************************************************
 * XM_Session_Send
 ************************************************************/

#define FUNCTION "XM_Session_Send"

XM_ErrorCode
XM_Session_Send (XM_Session Session, const void* Message)
{
   XMI_CookedInfo  Cooked;
   Int32           XmpLen;
   Int32           CryptedLen;

   if (Session == NULL || Message == NULL)
      return XME_BadParam;

   XmpLen = XMI_GetFrameLen (Message);
   if (XmpLen > XMP_MaxMessageLength)
      return XME_BadParam;    /* FIXME: Separate error code? */

   Cooked = Session->Cooked;
   if (Cooked == NULL || Cooked->Encrypt == NULL)
      return XMI_SendMessage (Session, (const Byte*) Message);
   else
   {
      /* This size is "too large".  The extra bytes are for the encryption */
      /* header and DES alignment which the user might not have taken into */
      /* account. Saxess accepts this violation of the contract. */
      Byte          Buffer [(XMP_HeaderSize + XMP_MaxMessageLength + 7) & ~7];
      XM_ErrorCode  Code;

      XmpLen = XMI_Crypt_GetAlignment (Cooked->Encrypt, XmpLen);
      Code = XM_Crypt_Encrypt (Cooked->Encrypt, & Buffer [XMP_HeaderSize],
                               & CryptedLen, Message, XmpLen);
      if (Code != XME_NoError)
         return Code;

      (void) XMP_SetHeader (Buffer, XMP_Type_Encrypted, 0, CryptedLen);

      return XMI_SendMessage (Session, Buffer);
   }
}

#undef FUNCTION

/************************************************************
 * XM_Session_Recv
 ************************************************************/

#define FUNCTION "XM_Session_Recv"

Int32
XM_Session_Recv (XM_Session Session, void* Reply)
{
   Int32  Count;

   Count = XM_Session_MultiRecv (Session, & Reply, 1);

   if (Count <= 0)
      return Count;
   else
      return XMI_GetFrameLen (Reply);
}

#undef FUNCTION

/************************************************************
 * XM_Session_MultiRecv
 ************************************************************/

#define FUNCTION "XM_Session_MultiRecv"

Int32
XM_Session_MultiRecv (XM_Session Session, void** Replies, Int32 Max)
{
   XMI_CookedInfo  Cooked;
   Int32           Pos;

   if (Session == NULL || Replies == NULL || Max < 1)
      return (Int32) XME_BadParam;

   Cooked = Session->Cooked;

	//fprintf(stderr,"XM_Session_MultiRecv : Before XMI_RecvMessages\n");fflush(stderr);	// 02/Apr/2009
   if (Cooked == NULL)
   {
		//fprintf(stderr,"XM_Session_MultiRecv : Inside Cooked NULL\n");fflush(stderr);	// 02/Apr/2009
      return XMI_RecvMessages (Session, (Byte**) Replies, Max,
                               Session->RecvTimeout);
   }                        
	
   /* Cooked session, i.e. try to decompress and decrypt */
   for (Pos = 0; Pos < Max; Pos ++)
   {
      Int32  XmpLen;

      if (Cooked->Used <= 0)
      {
         Int32  Count;

         if (Pos == 0 || XMI_MessageInBuffer (Session))
         {
            Count = XMI_RecvMessages (Session, & Cooked->Buffer, 1,
                                      Session->RecvTimeout);
            //fprintf(stderr,"XM_Session_MultiRecv : After XMI_RecvMessages\n");fflush(stderr);	// 02/Apr/2009
                          
         }                           
         else
            break;

         if (Count < 0)
            return Count;

         if (Count == 0)
            break;

         Cooked->Used = 0;
         Cooked->Xmp = Cooked->Buffer;
      }

      XmpLen = XMI_Uncook (Cooked);

      if (XmpLen <= 0)
         return XmpLen;

      (void) memcpy (Replies [Pos], Cooked->Xmp, (size_t) XmpLen);
      Cooked->Xmp = (const Byte*) XMP_GetNextFrame (Cooked->Xmp,
                                                    & Cooked->Used);
   }

   return Pos;
}

#undef FUNCTION

/************************************************************
 * XMI_Uncook
 ************************************************************/

#define FUNCTION "XMI_Uncook"

static Int32
XMI_Uncook (XMI_CookedInfo Cooked)
{
   XMP_Type      XmpType;
   Int32         XmpLen;
   XM_ErrorCode  Code;

   XmpType = (XMP_Type) XMP_GetBodyType (Cooked->Xmp);
   XmpLen = XMP_GetFrameLen (Cooked->Xmp);
   assert (XmpLen <= XMP_MaxMessageLength);

   while ((XmpType == XMP_Type_Encrypted && Cooked->Decrypt != NULL)
          || XmpType == XMP_Type_Compressed)
   {
      /* N.B. Any extension headers that are piggy-backed on the
       * encapsulation are discarded. Saxess does not send any
       * extension headers that way (except in key exchange, but
       * that is handled elsewhere).
       */
      const Byte*  Body;
      Int32        BodyLen;
      Byte*        TempPtr;

      Body = (const Byte*) XMP_GetBody (Cooked->Xmp, & BodyLen);

      if (BodyLen == 0)
         return 0;    /* This is not nice, but the only possibility (?) */

      if (XmpType == XMP_Type_Encrypted)
      {
         Code = XM_Crypt_Decrypt (Cooked->Decrypt, Cooked->AltBuffer, Body,
                                  BodyLen);

         if (Code != XME_NoError)
            return (Int32) Code;

         Cooked->Used = XMI_GetFrameLen (Cooked->AltBuffer);
      }
      else
      {
         Cooked->Used = XM_DC_Decompress (Cooked->DC, Cooked->AltBuffer,
                                          Body, BodyLen);

         if (Cooked->Used < 1)
            return Cooked->Used;
      }

      /* Swap buffers */
      TempPtr = Cooked->Buffer;
      Cooked->Buffer = Cooked->AltBuffer;
      Cooked->AltBuffer = TempPtr;

      Cooked->Xmp = Cooked->Buffer;
      XmpType = (XMP_Type) XMP_GetBodyType (Cooked->Xmp);
      XmpLen = XMP_GetFrameLen (Cooked->Xmp);
      assert (XmpLen <= XMP_MaxMessageLength);
   }

   return XmpLen;
}

#undef FUNCTION

/************************************************************
 * XM_Session_TestDistribution
 ************************************************************/

#define FUNCTION "XM_Session_TestDistribution"

Int32
XM_Session_TestDistribution (XM_Session* Sessions, Int32 Count,
                             XM_Session* Candidate)
{
   Uint32   Pos;
   Int32    Entries;
   Int32    DoubleSlot = -1;
   Boolean  EmptySlot = FALSE;
   struct {
     Int32      NumberOfSessionsForSlot;
     XM_Session AnySessionForSlot;
   } Slots [XMI_MaxServiceEntries];

   if (Sessions == NULL || Count <= 0 || Candidate == NULL)
      return (Int32) XME_BadParam;

   if (Count == 1)
      return 0;

   Entries = XMI_Service_CountEntries (Sessions [0]->Service);

   if (Entries < 0)
      return Entries;
   else if (Entries == 1)
      return 0;

   for (Pos = 0; Pos < XMI_MaxServiceEntries; Pos ++)
      Slots [Pos].NumberOfSessionsForSlot = 0;

   for (Pos = 0; Pos < (Uint32) Count; Pos ++)
   {
      XM_Session Session = Sessions [Pos];

      if (Session->Service != Sessions [0]->Service)
         return (Int32) XME_BadParam;

      Slots [Session->ServiceEntry].NumberOfSessionsForSlot ++;
      Slots [Session->ServiceEntry].AnySessionForSlot = Session;
   }

   for (Pos = 0; Pos < (Uint32) Entries; Pos ++)
   {
      if (Slots [Pos].NumberOfSessionsForSlot == 0)
         EmptySlot = TRUE;

      if (Slots [Pos].NumberOfSessionsForSlot > 1)
         DoubleSlot = (Int32) Pos;

      if (EmptySlot && DoubleSlot >= 0)
      {
         * Candidate = Slots [DoubleSlot].AnySessionForSlot;
         return 1;
      }
   }

   return 0;
}

#undef FUNCTION

/************************************************************
 * XMI_GenPermutation
 ************************************************************/

#define FUNCTION "XMI_GenPermutation"

static void
XMI_GenPermutation (Int32* Permutation, Int32 Count, XM_Service Service, Uint32 Seed)
{
   Int32       j;

#ifndef WIN32
   static pthread_mutex_t Lock = PTHREAD_MUTEX_INITIALIZER;
   Int32 LastStart;

   pthread_mutex_lock (&Lock);
   LastStart = XMI_GetLastIndex (Service);
   for (j = 0 ; j < Count ; j ++)
   {
      Permutation [j] = (LastStart + j) % Count;
      /*      fprintf (stderr, "Permutation [%d] = %d\n", j, Permutation [j]);*/
   }
   LastStart ++;
   if (LastStart == Count)
      LastStart = 0;

   XMI_SetLastIndex (Service, LastStart);
   pthread_mutex_unlock (&Lock);
#else
   XMI_Random  RandState;

   for (j = 0; j < Count; j ++)
      Permutation [j] = j;

   XMI_Random_Srand (Seed, RandState);

   /* Based on Knuth Vol 2 3rd edition, 3.4.2 Algorithm P */
   for (j = 0; j < Count; j ++)
   {
      Int32  k;

      k = XMI_Random_Uniform (0, j, RandState);

      /* k is [0..j-1] */
      Permutation [j] = Permutation [k];
      Permutation [k] = j;
   }
#endif

#if 0
   fprintf (stderr, "Perm: ");
   for (j = 0; j < Count; j ++) {
     fprintf (stderr, "%d:%d ", j, Permutation [j]);
   }

   fprintf (stderr, "\n");
#endif
}

#undef FUNCTION
