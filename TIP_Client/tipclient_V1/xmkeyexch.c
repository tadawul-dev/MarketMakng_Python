#include "xm.h"
#include "xmsession.h"

#include <time.h>

#define KX_MESSAGE_SZ    32
#define KX_NONCE_SZ       7
#define KX_DES_FUZZ       7

#define ExtHdrHdrSz       2

typedef struct XMI_KeyExchangeMsgTag
{
   Byte   OpCode;
#  define KX_OP_REQUEST 1
#  define KX_OP_REPLY   2

   Byte   Nonce [KX_NONCE_SZ];
   Int32  KeyId;
   Int32  Time;
   Byte   Key [XMP_MaxCryptKeySize];
}
XMI_KeyExchangeMsg;


static Byte         XMI_Entropy [256];


static XM_ErrorCode XMI_KX_RequestKey (XM_Session Session,
                                       XM_Crypt InitialCrypt,
                                       XMP_EA EA);

static XM_ErrorCode XMI_KX_ReceiveReply (XM_Session Session,
                                         XM_Crypt InitialCrypt,
                                         Byte* Key,
                                         Int32* KeySize,
                                         XMP_EA EA);

static XM_ErrorCode XMI_KX_RequestTime (XM_Session Session,
                                        Int32* UnixTime);

static Boolean      XMI_KX_MessageExport (Byte* Message,
                                          const XMI_KeyExchangeMsg* Out);

static Boolean      XMI_KX_MessageImport (const Byte* Message,
                                          XMI_KeyExchangeMsg* In);

static void         XMI_KX_SeedGenerator (Uint32 Seed,
                                          XMI_Random RandState);

static void         XMI_KX_SetRandomBytes (Byte* Ptr,
                                           Int32 Size,
                                           XMI_Random RandState);

/************************************************************
 * XMI_KeyExchange
 ************************************************************/

#define FUNCTION "XM_KeyExchange"

XM_ErrorCode
XM_Session_KeyExchange (XM_Session Session, XM_Crypt InitialCrypt,
                        XM_Crypt* Decrypt, XM_Crypt* Encrypt)
{
   XM_ErrorCode  Code;
   Byte          Key [XMP_MaxCryptKeySize];
   Int32         KeySize;
   XMP_EA        EA;

   if (Session == NULL || InitialCrypt == NULL)
      return XME_BadParam;

   if (Decrypt == NULL && Encrypt == NULL)
      return XME_BadParam;

   EA = XMI_Crypt_GetEA (InitialCrypt);
   if (EA != XMP_EA_RC4 && EA != XMP_EA_DES)
      return XME_BadAlgorithm;

   if (Decrypt != NULL)
      * Decrypt = NULL;

   if (Encrypt != NULL)
      * Encrypt = NULL;

   Code = XMI_KX_RequestKey (Session, InitialCrypt, EA);
   if (Code != XME_NoError)
      return Code;

   Code = XMI_KX_ReceiveReply (Session, InitialCrypt, Key, & KeySize, EA);
   if (Code != XME_NoError)
      return Code;

   if (Decrypt != NULL)
   {
      Code = XM_Crypt_Init (Decrypt, EA, Key, KeySize, XM_NoKeyId);
      if (Code != XME_NoError)
      {
         * Decrypt = NULL;
         return Code;
      }
   }

   if (Encrypt != NULL)
   {
      Code = XM_Crypt_Init (Encrypt, EA, Key, KeySize, XM_NoKeyId);
      if (Code != XME_NoError)
      {
         if (Decrypt != NULL)
         {
            (void) XM_Crypt_Destroy (* Decrypt);
            * Decrypt = NULL;
         }
         * Encrypt = NULL;
         return Code;
      }
   }

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_KX_RequestKey
 ************************************************************/

#define FUNCTION "XMI_KX_RequestKey"

static XM_ErrorCode
XMI_KX_RequestKey (XM_Session Session, XM_Crypt InitialCrypt, XMP_EA EA)
{
   XM_ErrorCode       Code;
   Byte               Request [XMP_MaxMessageLength];
   Byte*              Ptr;
   Byte               TmpBuffer [KX_MESSAGE_SZ + KX_DES_FUZZ];
   Int32              ExtHdrLen = 0;
   Int32              KeyId;
   Int32              BodyLen;
   Int32              CryptedLen;
   Int32              AlignedLen;
   Int32              UnixTime;
   Uint32             TmpInt;
   Byte               TmpByte;
   XMI_Random         RandState;
   XMI_KeyExchangeMsg KXMsg;

   KeyId = XMI_Crypt_GetKeyId (InitialCrypt);
   if (KeyId == XM_NoKeyId)
      return XME_BadParam;

   Code = XMI_KX_RequestTime (Session, & UnixTime);
   if (Code != XME_NoError)
      return Code;

   Ptr = & Request [XMP_HeaderSize];

   TmpInt = htonl ((Uint32) KeyId);
   Code = XMP_AddExtHdrEntry (Ptr, & ExtHdrLen, XMP_Tag_KeyId,
                              (const Byte*) & TmpInt, sizeof (Int32));
   if (Code != XME_NoError)
      return Code;

   TmpByte = (Byte) EA;
   Code = XMP_AddExtHdrEntry (Ptr, & ExtHdrLen, XMP_Tag_EncryptionAlgorithm,
                              & TmpByte, sizeof (Byte));
   if (Code != XME_NoError)
      return Code;

   Ptr += ExtHdrLen;

   KXMsg.OpCode = KX_OP_REQUEST;
   KXMsg.KeyId = KeyId;
   KXMsg.Time = UnixTime;

   XMI_KX_SeedGenerator ((Uint32) KeyId, RandState);
   XMI_KX_SetRandomBytes (KXMsg.Nonce, sizeof (KXMsg.Nonce), RandState);
   XMI_KX_SetRandomBytes (KXMsg.Key, sizeof (KXMsg.Key), RandState);

   XMI_KX_MessageExport (TmpBuffer, & KXMsg);
   BodyLen = XMI_GetFrameLen (TmpBuffer);

   AlignedLen = XMI_Crypt_GetAlignment (InitialCrypt, BodyLen);
   if (AlignedLen > BodyLen)
   {
      XMI_KX_SetRandomBytes (KXMsg.Key + XMP_MaxCryptKeySize,
                             AlignedLen - BodyLen, RandState);
      BodyLen = AlignedLen;
   }

   Code = XM_Crypt_Encrypt (InitialCrypt, Ptr, & CryptedLen, TmpBuffer,
                            BodyLen);
   if (Code != XME_NoError)
      return Code;

   Code = XMP_SetHeader (Request, XMP_Type_Encrypted, ExtHdrLen, CryptedLen);
   if (Code != XME_NoError)
      return Code;

   return XMI_SendMessage (Session, Request);
}

#undef FUNCTION

/************************************************************
 * XMI_KX_ReceiveReply
 ************************************************************/

#define FUNCTION "XMI_KX_ReceiveReply"

static XM_ErrorCode
XMI_KX_ReceiveReply (XM_Session Session, XM_Crypt InitialCrypt,
                          Byte* Key, Int32* KeySize, XMP_EA EA)
{
   Int32              Count;
   XM_ErrorCode       Code;
   Byte               Buffer [XMP_MaxMessageLength];
   Byte*              Ptr;
   Byte               Decrypted [XMP_HeaderSize + KX_MESSAGE_SZ + KX_DES_FUZZ];
   Int32              Offset = 0;
   XMP_Type           XmpType;
   const Byte*        Body;
   Int32              BodyLen;
   Int32              AlignedLen;
   Int32              KeyId;
   Boolean            KeyIdOk = FALSE;
   Boolean            AlgorithmOk = FALSE;
   XMI_KeyExchangeMsg KXMsg;

   Ptr = Buffer;
   Count = XMI_RecvMessages (Session, & Ptr, 1, Session->RecvTimeout);
   if (Count != 1)
      return Count < 0 ? (XM_ErrorCode) Count : XME_InternalError;

   XmpType = XMI_GetHeaderType (Buffer);
   if (XmpType != XMP_Type_Encrypted)
      return XME_InternalError;  /* FIXME: Needs a new error code? */

   KeyId = XMI_Crypt_GetKeyId (InitialCrypt);
   if (KeyId == XM_NoKeyId)
      return XME_InternalError;

   do
   {
      XMP_Tag      Tag;
      const void*  ExtHdr;
      Int32        ExtHdrLen;
      Uint32       TmpInt;
      Byte         TmpByte;

      Code = XMP_GetExtHdrEntry (Buffer, & Offset, & Tag,
                                 & ExtHdr, & ExtHdrLen);
      switch (Code)
      {
       case XME_NoError:
         switch (Tag)
         {
          case XMP_Tag_KeyId:
            if (ExtHdrLen != (Int32) sizeof (Int32))
               return XME_InternalError;   /* FIXME: */
            (void) memcpy ((Byte*) & TmpInt, ExtHdr, sizeof (Int32));
            KeyIdOk = (ntohl (TmpInt) == (Uint32) KeyId);
            if (! KeyIdOk)
               return XME_InternalError;  /* FIXME: */
            break;

          case XMP_Tag_EncryptionAlgorithm:
            if (ExtHdrLen != (Int32) sizeof (Byte))
               return XME_InternalError;   /* FIXME: */
            (void) memcpy ((Byte*) & TmpByte, ExtHdr, sizeof (Byte));
            AlgorithmOk = (TmpByte == (Byte) EA);
            if (! AlgorithmOk)
               return XME_InternalError;  /* FIXME: */
            break;

           default:
             break;
         }
         break;

       case XME_NoMoreEntries:
         break;

       default:
         XMI_Print ("%s: Error parsing Key Exchange reply", FUNCTION);
         return Code;
      }
   }
   while (Code == XME_NoError);

   if (! (KeyIdOk && AlgorithmOk))
      return XME_InternalError; /* FIXME: */

   Body = (const Byte*) XMP_GetBody (Buffer, & BodyLen);
   AlignedLen = XMI_Crypt_GetAlignment (InitialCrypt,
                                        XMP_HeaderSize + KX_MESSAGE_SZ);
   if (BodyLen != AlignedLen)
      return XME_InternalError; /* FIXME: */

   Code = XM_Crypt_Decrypt (InitialCrypt, Decrypted, Body, BodyLen);
   if (Code != XME_NoError)
      return Code;

   if (! XMI_KX_MessageImport (Decrypted, & KXMsg))
      return XME_InternalError;  /* FIXME: Needs a new error code? */

   if (KXMsg.KeyId != KeyId)
      return XME_InternalError;  /* FIXME: Needs a new error code? */

   (void) memcpy (Key, KXMsg.Key, sizeof (KXMsg.Key));
   * KeySize = sizeof (KXMsg.Key);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_KX_MessageExport
 ************************************************************/

#define FUNCTION "XMI_KX_MessageExport"

static XM_ErrorCode
XMI_KX_RequestTime (XM_Session Session, Int32* UnixTime)
{
   XM_ErrorCode  Code;
   Byte          Buffer [XMP_HeaderSize + XMP_MaxExtHdrSize];
   Byte*         Ptr;
   Int32         Offset = 0;
   Int32         Count;
   Boolean       Found = FALSE;

   Code = XMP_AddExtHdrEntry (& Buffer [XMP_HeaderSize], & Offset,
                              XMP_Tag_Timestamp, NULL, 0);
   if (Code != XME_NoError)
      return Code;

   Code = XMP_SetHeader (Buffer, XMP_Type_None, Offset, 0);
   if (Code != XME_NoError)
      return Code;

   Code = XMI_SendMessage (Session, Buffer);
   if (Code != XME_NoError)
      return Code;

   Ptr = Buffer;
   Count = XMI_RecvMessages (Session, & Ptr, 1, Session->RecvTimeout);
   if (Count != 1)
     return Count < 0 ? (XM_ErrorCode) Count : XME_InternalError;

   Offset = 0;
   do
   {
      XMP_Tag      Tag;
      const void*  ExtHdr;
      Int32        ExtHdrLen;
      Int32        TmpInt;

      Code = XMP_GetExtHdrEntry (Buffer, & Offset, & Tag, & ExtHdr,
                                 & ExtHdrLen);
      switch (Code)
      {
       case XME_NoError:
         if (Tag == XMP_Tag_Timestamp)
         {
            (void) memcpy (& TmpInt, ExtHdr, sizeof (Int32));
            * UnixTime = ntohl (TmpInt);
            return XME_NoError;
         }
         break;

       case XME_NoMoreEntries:
         break;

       default:
         XMI_Print ("%s: Error parsing reply to timestamp-request", FUNCTION);
         return Code;
      }
   } while (Code == XME_NoError && ! Found);

   return XME_InternalError; /* FIXME: better error code? */
}

#undef FUNCTION

/************************************************************
 * XMI_KX_MessageExport
 ************************************************************/

#define FUNCTION "XMI_KX_MessageExport"

static Boolean
XMI_KX_MessageExport (Byte* Message, const XMI_KeyExchangeMsg* Out)
{
   Int32         TmpInt;
   XM_ErrorCode  Code;

   Code = XMP_SetHeader (Message, XMP_Type_KeyExchange, 0, KX_MESSAGE_SZ);
   assert (Code == XME_NoError);

   Message += XMP_HeaderSize;
   * Message ++ = Out->OpCode;

   (void) memcpy (Message, Out->Nonce, sizeof (Out->Nonce));
   Message += sizeof (Out->Nonce);

   TmpInt = htonl (Out->KeyId);
   (void) memcpy (Message, & TmpInt, sizeof (Int32));
   Message += sizeof (Int32);

   TmpInt = htonl (Out->Time);
   (void) memcpy (Message, & TmpInt, sizeof (Int32));
   Message += sizeof (Int32);

   (void) memcpy (Message, Out->Key, sizeof (Out->Key));
}

#undef FUNCTION

/************************************************************
 * XMI_KX_MessageImport
 ************************************************************/

#define FUNCTION "XMI_KX_MessageImport"

static Boolean
XMI_KX_MessageImport (const Byte* Message, XMI_KeyExchangeMsg* In)
{
   const Byte*  Body;
   Int32        BodyLen;

   if (XMI_GetHeaderType (Message) != XMP_Type_KeyExchange)
      return FALSE;

   Body = (const Byte*) XMP_GetBody (Message, & BodyLen);
   if (BodyLen != KX_MESSAGE_SZ)
      return FALSE;

   In->OpCode = * Body;
   if (In->OpCode != KX_OP_REPLY && In->OpCode != KX_OP_REQUEST)
      return FALSE;

   Body += (sizeof (In->OpCode) + sizeof (In->Nonce));
   (void) memcpy ((Byte*) & In->KeyId, Body, sizeof (Int32));
   In->KeyId = ntohl (In->KeyId);

   Body += sizeof (In->KeyId);
   (void) memcpy ((Byte*) & In->Time, Body, sizeof (Int32));
   In->Time = ntohl (In->Time);

   Body += sizeof (In->Time);
   (void) memcpy (In->Key, Body, sizeof (In->Key));

   return TRUE;
}

#undef FUNCTION

/************************************************************
 * XMI_KX_SeedGenerator
 ************************************************************/

#define FUNCTION "XMI_KX_SeedGenerator"

static void
XMI_KX_SeedGenerator (Uint32 Seed, XMI_Random RandState)
{
#if !defined WIN32
   struct timeval     Tv;
#else
   SYSTEMTIME         Time;
#endif

   /* Fixme: The seed could probably be improved. Most of all, would like
      to ensure that different session threads get different seeds even
      if they call KeyExchange at the same time. */
#if !defined WIN32
   if (gettimeofday (& Tv, NULL) == 0)
      Seed = Seed ^ (Uint32) Tv.tv_sec ^ (Uint32) ((Tv.tv_usec + 1) * 15259);
#else
   GetLocalTime (& Time);
   Seed = Seed ^ time (NULL) ^ ((Time.wMilliseconds + 1) * 15259);
#endif

   XMI_Random_Srand (Seed, RandState);
}

#undef FUNCTION

/************************************************************
 * XMI_KX_SetRandomBytes
 ************************************************************/

#define FUNCTION "XMI_KX_SetRandomBytes"

static void
XMI_KX_SetRandomBytes (Byte* Ptr, Int32 Size, XMI_Random RandState)
{
   Int32  p1;

   for (p1 = 0; p1 < Size; p1 ++)
      Ptr [p1] = (Byte) XMI_Entropy [XMI_Random_Uniform (0, 255, RandState)];
}

#undef FUNCTION

/************************************************************
 * XM_InitEntropy
 ************************************************************/

#define FUNCTION "XM_InitEntropy"

XM_ErrorCode
XM_InitEntropy (const void* EntropyIn, Int32 Size)
{
   const Byte* Entropy = (const Byte*) EntropyIn;
   Uint32      p1 = 0;
   Int32       p2 = 0;

   if (Size < 0 || (Size > 0 && Entropy == NULL))
      return XME_BadParam;

   if (Size == 0)     /* Special case, fill XMI_Entropy with default values. */
   {
      for (p1 = 0; p1 < sizeof (XMI_Entropy); p1 ++)
         XMI_Entropy [p1] = (Byte) p1;
   }
   else
   {
      while (p1 < sizeof (XMI_Entropy))
      {
         XMI_Entropy [p1 ++] = Entropy [p2 ++];
         if (p2 == Size)
            p2 = 0;
      }
   }

   return XME_NoError;
}

#undef FUNCTION
