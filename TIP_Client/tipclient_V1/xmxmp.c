#include "xm.h"

#define NBIBY     8 /* Number of bits per byte. */
#define MAXUCHAR  ((1U << NBIBY) - 1)
#define MAXUSHORT ((1U << (2 * NBIBY)) - 1)

#define ExtHdrHdrSz    2

/************************************************************
 * XMP_GetBodyType
 ************************************************************/

#define FUNCTION "XMP_GetBodyType"

Int32
XMP_GetBodyType (const void* Message)
{
   if (Message == NULL)
      return (Int32) XME_BadParam;

   return (Int32) XMI_GetHeaderType (Message);
}

#undef FUNCTION

/************************************************************
 * XMP_GetFrameLen
 ************************************************************/

#define FUNCTION "XMP_GetFrameLen"

Int32
XMP_GetFrameLen (const void* Message)
{
   if (Message == NULL)
      return (Int32) XME_BadParam;

   return (Int32) XMI_GetFrameLen (Message);
}

#undef FUNCTION

/************************************************************
 * XMP_GetExtHeaderLen
 ************************************************************/

#define FUNCTION "XMP_GetExtHeaderLen"

Int32
XMP_GetExtHeaderLen (const void* Message)
{
   if (Message == NULL)
      return (Int32) XME_BadParam;

   return (Int32) XMI_GetExtHeaderLen (Message);
}

#undef FUNCTION

/************************************************************
 * XMP_GetBody
 ************************************************************/

#define FUNCTION "XMP_GetBody"

const void*
XMP_GetBody (const void* MessageIn, Int32* BodyLen)
{
   Int32        ExtLen;
   const Byte*  Message = (const Byte*) MessageIn;

   if (BodyLen == NULL)
      return NULL;

   * BodyLen = (Int32) XMI_GetBodyLen (Message);
   ExtLen = (Int32) XMI_GetExtHeaderLen (Message);

   if (* BodyLen == 0)
      return NULL;

   return & Message [XMP_HeaderSize + ExtLen];
}

#undef FUNCTION

/************************************************************
 * XMP_GetBodyLen
 ************************************************************/

#define FUNCTION "XMP_GetBodyLen"

Int32
XMP_GetBodyLen (const void* MessageIn)
{
   const Byte*  Message = (const Byte*) MessageIn;


   return (Int32) XMI_GetBodyLen (Message);
}

#undef FUNCTION

/************************************************************
 * XMP_GetNextFrame
 ************************************************************/

#define FUNCTION "XMP_GetNextFrame"

const void*
XMP_GetNextFrame (const void* BufferIn, Int32* RemainingLength)
{
   Int32        ThisFrameLen;
   const Byte*  Buffer = (const Byte*) BufferIn;

   if (* RemainingLength < XMP_HeaderSize)
      return NULL;

   ThisFrameLen = XMI_GetFrameLen (Buffer);
   if (ThisFrameLen > * RemainingLength)
      return NULL; /* Error */

   * RemainingLength -= ThisFrameLen;

   if (* RemainingLength < XMP_HeaderSize)
      return NULL;

   return & Buffer [ThisFrameLen];
}

#undef FUNCTION

/************************************************************
 * XMP_GetExtHdrEntry
 ************************************************************/

#define FUNCTION "XMP_GetExtHdrEntry"

XM_ErrorCode
XMP_GetExtHdrEntry (const void* MessageIn, Int32* Offset, XMP_Tag* Tag,
                    const void** Body, Int32* BodyLen)
{
   const Byte*  Message = (const Byte*) MessageIn;
   const Byte*  Entry;
   Int32        ExtHdrLen;

   if (Message == NULL || Offset == NULL || Tag == NULL)
      return XME_BadParam;

   if (Body == NULL || BodyLen == NULL)
      return XME_BadParam;

   if (* Offset < 0 || * Offset > (XMP_MaxExtHdrSize - ExtHdrHdrSz))
      return XME_BadParam;

   ExtHdrLen = (Int32) XMI_GetExtHeaderLen (Message);

   if (* Offset > ExtHdrLen - ExtHdrHdrSz)
      return XME_NoMoreEntries;

   Entry = & Message [XMP_HeaderSize + * Offset];
   * Tag = (XMP_Tag) Entry [0];
   * BodyLen = Entry [1];
   * Body = & Entry [2];

   /* Skip incomplete entries. */
   if (* BodyLen > ExtHdrLen - * Offset - ExtHdrHdrSz)
      return XME_NoMoreEntries;

   /* BodyLen can not be more than 8 bits so ntohl is not needed. */
   * Offset += * BodyLen + ExtHdrHdrSz;

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMP_AddExtHdrEntry
 ************************************************************/

#define FUNCTION "XMP_AddExtHdrEntry"

XM_ErrorCode
XMP_AddExtHdrEntry (void* ExtHdrIn, Int32* ExtHdrLen, XMP_Tag Tag,
                    const void* Body, Int32 BodyLen)
{
   Byte*  ExtHdr = (Byte*) ExtHdrIn;
   Byte*  Entry;

   if (ExtHdr == NULL || ExtHdrLen == NULL || * ExtHdrLen < 0)
      return XME_BadParam;

   if (Body == NULL && BodyLen != 0)
      return XME_BadParam;

   if (* ExtHdrLen + BodyLen + ExtHdrHdrSz > XMP_MaxExtHdrSize)
      return XME_NoRoom;

   Entry = & ExtHdr [* ExtHdrLen];
   Entry [0] = (Byte) Tag;
   Entry [1] = (Byte) BodyLen;

   if (Body != NULL)
      (void) memcpy (& Entry [ExtHdrHdrSz], Body, (size_t) BodyLen);

   * ExtHdrLen += BodyLen + ExtHdrHdrSz;
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMP_CreateMessage
 ************************************************************/

#define FUNCTION "XMP_CreateMessage"

XM_ErrorCode
XMP_CreateMessage (void* BufferIn, Int32 BufferSize, XMP_Type Type,
                   const void* ExtHdr, Int32 ExtHdrLen,
                   const void* Body, Int32 BodyLen)
{
   Byte*         Buffer = (Byte*) BufferIn;
   XM_ErrorCode  Code;

   if ( BodyLen == 0 && Type != XMP_Type_None )
     return XME_BadLength;
   
   if (BodyLen == 0 && Body != NULL)
      return XME_BadParam;

   if ((Body == NULL && BodyLen != 0) || (Body != NULL && BodyLen < 1))
      return XME_BadParam;

   if ((ExtHdr == NULL && ExtHdrLen != 0) || (ExtHdr != NULL && ExtHdrLen < 1))
      return XME_BadParam;

   if (Buffer == NULL || BufferSize < XMP_HeaderSize + ExtHdrLen + BodyLen)
      return XME_BufferTooSmall;

   Code = XMP_SetHeader (Buffer, Type, ExtHdrLen, BodyLen);

   if (Code != XME_NoError)
      return Code;

   if (ExtHdr != NULL)
      (void) memcpy (Buffer + XMP_HeaderSize, ExtHdr, (size_t) ExtHdrLen);

   if (Body != NULL)
      (void) memcpy (Buffer + XMP_HeaderSize + ExtHdrLen, Body,
                     (size_t) BodyLen);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMP_SetHeader
 ************************************************************/

#define FUNCTION "XMP_SetHeader"

XM_ErrorCode
XMP_SetHeader (void* BufferIn, XMP_Type Type, Int32 ExtHdrLen, Int32 BodyLen)
{
   Byte*  Buffer = (Byte*) BufferIn;

   if (Buffer == NULL)
      return XME_BadParam;

   if ((Int32) Type < 0 || (Uint32) Type > MAXUCHAR)
      return XME_BadParam;

   if (ExtHdrLen < 0 || ExtHdrLen > XMP_MaxExtHdrSize)
      return XME_BadParam;

   if (BodyLen < 0 || (Uint32) BodyLen > MAXUSHORT)
      return XME_BadParam;

   Buffer [0] = (Byte) Type;
   Buffer [1] = (Byte) ExtHdrLen;
   /* Optimization: htons and memcpy removed. */
   Buffer [2] = (Byte) (((Uint32) BodyLen & 0xff00) >> 8);
   Buffer [3] = (Byte) ((Uint32) BodyLen & 0xff);

   return XME_NoError;
}

#undef FUNCTION
