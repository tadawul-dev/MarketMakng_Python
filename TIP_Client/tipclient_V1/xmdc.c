#include "xm.h"

#define COMMAND_OPCODE_END_STREAM  1
#define PHRASE_LENGTH_ADJUST       3
#define HISTORY_SIZE_BITS         13

#define HISTORY_SIZE              (1 << HISTORY_SIZE_BITS)
#define PHRASE_OFFSET_COMMAND     HISTORY_SIZE
#define HIST_MODULO(_n)           ((_n) & (HISTORY_SIZE - 1))

typedef struct XMI_InDataBuffer   XMI_InDataBuffer;
typedef struct XMI_OutDataBuffer  XMI_OutDataBuffer;

struct XMI_InDataBuffer
{
   Int32 Size;
   Int32 Curr;

   const Byte* Data;
};

struct XMI_OutDataBuffer
{
   Int32 Size;
   Int32 Curr;

   Byte* Data;
};

struct XMI_DC
{
   Uint32 BitData;
   Int32 NumBits;

   XMI_InDataBuffer  Input;
   XMI_OutDataBuffer Output;
   XMI_OutDataBuffer History;

   Byte HistoryBuffer [HISTORY_SIZE];
};

static INLINE void         InitInDataBuffer (XMI_InDataBuffer* B,
                                             const Byte* Data, Int32 Size);
static INLINE void         InitOutDataBuffer (XMI_OutDataBuffer* B,
                                              Byte* Data, Int32 Size);
static INLINE void         ResetBitBuffer (XM_DC DC);
static INLINE XM_ErrorCode PutOctet (XM_DC DC, Int32 Chr);
static INLINE Int32        PeekBits (XM_DC DC, Int32 NumBits);
static INLINE Int32        GetBits (XM_DC DC, Int32 NumBits);
static INLINE Int32        GetLengthRepr (XM_DC DC);
static INLINE Int32        GetOffsetRepr (XM_DC DC);
static INLINE Int32        GetLiteralRepr (XM_DC DC);
static INLINE XM_ErrorCode GetLiteral (XM_DC DC);
static INLINE Int32        GetPhrase (XM_DC DC);

/************************************************************
 * InitInDataBuffer
 ************************************************************/

static INLINE void
InitInDataBuffer (XMI_InDataBuffer* B, const Byte* Data, Int32 Size)
{
   B->Data = Data;
   B->Size = Size;
   B->Curr = 0;
}

/************************************************************
 * InitOutDataBuffer
 ************************************************************/

static INLINE void
InitOutDataBuffer (XMI_OutDataBuffer* B, Byte* Data, Int32 Size)
{
   B->Data = Data;
   B->Size = Size;
   B->Curr = 0;
}

/************************************************************
 * ResetBitBuffer
 ************************************************************/

static INLINE void
ResetBitBuffer (XM_DC DC)
{
   DC->BitData = 0;
   DC->NumBits = 0;
}

/************************************************************
 * PutOctet
 ************************************************************/

static INLINE XM_ErrorCode
PutOctet (XM_DC DC, Int32 Chr)
{
   XMI_OutDataBuffer* B = & DC->Output;

   if (B->Curr >= B->Size)
      return XME_BufferTooSmall;

   B->Data [B->Curr ++] = (Byte) Chr;
   return XME_NoError;
}

/************************************************************
 * PeekBits
 ************************************************************/

static INLINE Int32
PeekBits (XM_DC DC, Int32 NumBits)
{
   assert (NumBits > 0 && NumBits <= 24);

   if (DC->NumBits < NumBits)
   {
      while (DC->NumBits <= 24)
      {
         XMI_InDataBuffer* B = & DC->Input;
         Uint32 Temp;

         if (B->Curr >= B->Size)
            break;

         Temp = (Uint32) (B->Data [B->Curr ++]);

         DC->BitData |= Temp << (24 - DC->NumBits);
         DC->NumBits += 8;
      }
      if (DC->NumBits == 0)
         return (Int32) XME_DC_EndOfFile;
   }
   return (Int32) (DC->BitData >> (32 - NumBits));
}

/************************************************************
 * GetBits
 ************************************************************/

static INLINE Int32
GetBits (XM_DC DC, Int32 NumBits)
{
   Int32 Temp;

   if (DC->NumBits >= NumBits)
   {
      Temp = (Int32) (DC->BitData >> (32 - NumBits));
   }
   else
   {
      Temp = PeekBits (DC, NumBits);

      if (Temp < 0)
         return Temp;
   }
   DC->BitData <<= NumBits;
   DC->NumBits  -= NumBits;

   return Temp;
}

/************************************************************
 * GetLengthRepr
 ************************************************************/

static INLINE Int32
GetLengthRepr (XM_DC DC)
{
   static Int32 HBits [16] = { 1,1,1,1,1,1,1,1,2,2,2,2,3,3,4,4 };
   static Int32 DBits [16] = { 0,0,0,0,0,0,0,0,1,1,1,1,2,2,3,4 };
   static Int32 Bases [5]  = { 0,1,3,7,15 };

   Int32 Bits, Data, Header;

   Header = PeekBits (DC, 4);

   if (Header < 0) /* Indicates an error has occured */
      return Header;

   Bits = HBits [Header];

   Data = GetBits (DC, Bits); /* or SkipBits */
   if (Data < 0)
      return Data;

   Bits = DBits [Header];
   Data = (Bits == 0) ? 0 : GetBits (DC, DBits [Header]);

   if (Data < 0)
      return Data;

   return Bases [Bits] + Data;
}

/************************************************************
 * GetOffsetRepr
 ************************************************************/

static INLINE Int32
GetOffsetRepr (XM_DC DC)
{
   Int32 Bits;

   switch (GetBits (DC, 1))
   {
    case 0:
      return GetBits (DC, 8);

    case 1:
      Bits = GetBits (DC, 13);

      if (Bits >= 0)
         return Bits + 256;
      break;

    default:
      break;
   }

   return (Int32) XME_DC_EndOfFile;
}

/************************************************************
 * GetLiteralRepr
 ************************************************************/

static INLINE Int32
GetLiteralRepr (XM_DC DC)
{
   Int32 Code;

   static char Literals [16] =
   {
      '\0',' ','0','1','2','3','4','5','6','7','8','9'
   };
   Int32 Repr;

   Code = GetBits (DC, 1);
   switch (Code)
   {
    case XME_DC_EndOfFile:
      return Code;

    case 0:
      Repr = GetBits (DC, 4);

      if (Repr < 0)
         return (Int32) XME_DC_EndOfFile;

      return (Int32) (Literals [Repr]);

    case 1:
      return GetBits (DC, 8);

    default:
      return (Int32) XME_DC_BadBitStream;
   }
}

/************************************************************
 * GetLiteral
 ************************************************************/

static INLINE XM_ErrorCode
GetLiteral (XM_DC DC)
{
   XMI_OutDataBuffer* H = & DC->History;
   Int32 Chr = GetLiteralRepr (DC);
   XM_ErrorCode Code;

   if (Chr < 0)
      return (XM_ErrorCode) Chr;

   Code = PutOctet (DC, Chr);
   if (Code != XME_NoError)
      return Code;

   H->Data [H->Curr] = (Byte) Chr;
   H->Curr = HIST_MODULO (H->Curr + 1);

   return XME_NoError;
}

/************************************************************
 * GetPhrase
 * returns non-zero command opcode if a command was found
 * PHRASE_OFFSET_COMMAND indicates a command
 ************************************************************/

static INLINE Int32
GetPhrase (XM_DC DC)
{
   Int32 Count, Length, Offset, XLength, XOffset, Limit, Curr;

   Curr = DC->History.Curr;

   XOffset = GetOffsetRepr (DC);

   if (XOffset < 0)
      return XOffset;

   XLength = GetLengthRepr (DC);

   if (XLength < 0)
      return XLength;

   if (XOffset == PHRASE_OFFSET_COMMAND)
      return XLength; /* return Opcode */

   Offset = DC->History.Curr - XOffset + HISTORY_SIZE;
   Length = XLength + PHRASE_LENGTH_ADJUST;

   /* Copy phrase from history */

   Limit = HISTORY_SIZE - Length - 1;

   if (Offset < Limit && Curr < Limit)
   {
      Byte* D1 = DC->History.Data + Offset;
      Byte* D2 = DC->History.Data + Curr;
      Byte* D3 = D1 + Length;

      while (D1 < D3)
      {
         * D2 ++ = * D1 ++;
      }
      DC->History.Curr += Length;
   }
   else
   {
      for (Count = 0; Count < Length; Count ++)
      {
         XMI_OutDataBuffer* H = & DC->History;
         Int32 Chr = H->Data [HIST_MODULO (Offset + Count)];

         H->Data [H->Curr] = (Byte) Chr;
         H->Curr = HIST_MODULO (H->Curr + 1);
      }
   }

   /* Fast put bypasses the modulo arithmentic neccessary to implement
      a ring buffer history. If the current phrase won't wrap the code
      below is executed. The put buffer is only checked once. */

   if (Curr < Limit)
   {
      XMI_OutDataBuffer* B = & DC->Output;

      if (B->Curr + Length >= B->Size)
         return (Int32) XME_BufferTooSmall;

      (void) memcpy (B->Data + B->Curr, DC->History.Data + Curr,
                     (size_t) Length);

      B->Curr += Length;
   }
   else
   {
      for (Count = 0; Count < Length; Count ++)
      {
         Int32 Chr = DC->History.Data [HIST_MODULO (Curr + Count)];
         XM_ErrorCode Code = PutOctet (DC, Chr);

         if (Code != XME_NoError)
            return (Int32) Code;
      }
   }

   return (Int32) XME_NoError; /* a phrase was found */
}

/************************************************************
 * Inflate
 ************************************************************/

static XM_ErrorCode
Inflate (XM_DC DC)
{
   for (;;)
   {
      Int32        Hdr = GetBits (DC, 1);
      Int32        Opcode;
      XM_ErrorCode Code;

      if (Hdr == (Int32) XME_DC_EndOfFile)
         return XME_DC_BadBitStream;

      switch (Hdr)
      {
       case 0:
         Code = GetLiteral (DC);
         if (Code != XME_NoError)
            return Code;
         break;

       case 1:
         Opcode = GetPhrase (DC);

         if (Opcode == 0) /* Normal case */
            break;

         if (Opcode == COMMAND_OPCODE_END_STREAM)
            return XME_NoError;

         if (Opcode > 0) /* Undefined opcode */
            return XME_DC_BadOpcode;

         return (XM_ErrorCode) Opcode; /* Error from called function */

       default:
         return XME_DC_BadHeaderType;
      }
   }
}

/************************************************************
 * XM_DC_Init
 ************************************************************/

#define FUNCTION "XM_DC_Init"

XM_ErrorCode
XM_DC_Init (XM_DC* DC, XMP_CA CA)
{
   XM_DC NewDC;

   if (DC == NULL)
      return XME_BadParam;

   if (CA != XMP_CA_LZ1)
      return XME_NotSupported;

   NewDC = (XM_DC) malloc (sizeof (struct XMI_DC));
   * DC = NewDC;

   if (NewDC == NULL)
      return XME_NoMemory;

   (void) memset (NewDC, 0, sizeof (struct XMI_DC));

   InitInDataBuffer (& NewDC->Input, NULL, 0);
   InitOutDataBuffer (& NewDC->Output, NULL, 0);
   InitOutDataBuffer (& NewDC->History, NewDC->HistoryBuffer, HISTORY_SIZE);

   ResetBitBuffer (NewDC);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_DC_Destroy
 ************************************************************/

#define FUNCTION "XM_DC_Destroy"

XM_ErrorCode
XM_DC_Destroy (XM_DC DC)
{
   free (DC);
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_DC_Decompress
 ************************************************************/

#define FUNCTION "XM_DC_Decompress"

Int32
XM_DC_Decompress (XM_DC DC, void* OutData, const void* InpData, Int32 InpSize)
{
   XM_ErrorCode Code;

   if (InpData == OutData) /* A separate buffer is required */
      return (Int32) XME_BadParam;

   if (InpSize <= 0) /* Must be positive */
      return (Int32) XME_BadParam;

   /* An important contract with server side is that no decompressed frame
      may be larger than XMP_MaxMessageLength, i.e. the other
      XME_BufferTooSmall tests in this file should not fail. */
   /* Removed the OutSize parameter. If the OutData area is too small an
      overwrite will occur.  It would have anyway if the area was to small,
      but OutSize lied. */

   InitInDataBuffer (& DC->Input, (const Byte*) InpData, InpSize);
   InitOutDataBuffer (& DC->Output, (Byte*) OutData, XMP_MaxMessageLength);

   ResetBitBuffer (DC);

   Code = Inflate (DC);
   if (Code != XME_NoError)
      return (Int32) Code;

   return DC->Output.Curr;
}

#undef FUNCTION
