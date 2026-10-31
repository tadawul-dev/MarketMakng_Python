#include "xm.h"
#include <stdarg.h>

#if defined WIN32
#include <io.h>
#endif

static Boolean   XMI_Initialized = FALSE;
static Int32     XMI_TextualOutputFD = -1;

/************************************************************
 * XM_Init
 ************************************************************/

#define FUNCTION "XM_Init"

XM_ErrorCode
XM_Init (Int32 TextualOutputFD)
{
   if (! XMI_Initialized)
   {
      XMI_TextualOutputFD = TextualOutputFD;

#ifdef WIN32
      if (! XMI_WinSockInit ())
      {
         XMI_Print ("%s: failed to initialize Winsock DLL", FUNCTION);
         return XME_InternalError;
      }
#endif

      (void) XM_InitEntropy (NULL, 0);

      XMI_Initialized = TRUE;
   }

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_GetErrorMessage
 ************************************************************/

#define FUNCTION "XM_GetErrorMessage"

const char*
XM_GetErrorMessage (XM_ErrorCode Code)
{
   static struct
   {
      XM_ErrorCode Code;
      const char*  Message;
   }
   Map [] =
   {
      { XME_NoError, "No error" },
      { XME_NoMemory, "Out of memory" },
      { XME_BadParam, "Bad parameter" },
      { XME_BadLength, "Bad length" },
      { XME_NoRoom, "No room for operation" },
      { XME_NoServiceEntries, "No service entries" },
      { XME_NotSupported, "Operation not supported" },
      { XME_NetworkError, "Network error" },
      { XME_Timeout, "Operation timed out" },
      { XME_BufferTooSmall, "Buffer is too small" },
      { XME_InternalError, "Internal error" },
      { XME_NotInitialized, "XT not initialized" },
      { XME_NoMoreEntries, "Message is out of extension headers" },
      { XME_DC_EndOfFile, "Decompression - end of file" },
      { XME_DC_BadSize, "Decompression - bad size" },
      { XME_DC_BadBitStream, "Decompression - bad bit stream" },
      { XME_DC_BadOpcode, "Decompression - bad op code" },
      { XME_DC_BadHeaderType, "Decompression - bad header type" },
      { XME_DC_BadPhraseLength, "Decompresssion - bad phrase length" },
      { XME_DC_BadPhraseOffset, "Decompresssion - bad phrase offset" },
      { XME_BadAlgorithm, "Encryption - unknown algorithm" },
      { XME_BadKeyLength, "Encryption - unacceptable key length" },
      { XME_WeakKey, "Encryption - key too weak" }
   };
   unsigned int Pos;

   for (Pos = 0; Pos < sizeof (Map) / sizeof (Map [0]); Pos ++)
   {
      if (Map [Pos].Code == Code)
      {
         return Map [Pos].Message;
      }
   }

   return "<Invalid XMAPI error code>";
}

#undef FUNCTION

/************************************************************
 * XMI_Print
 ************************************************************/

#define FUNCTION "XMI_Print"

#define XMI_MaxPrintLen 512

void
XMI_Print (const char* Format, ...)
{
   va_list VA;
   char    Buf [XMI_MaxPrintLen];
   Uint32  L;

   if (XMI_TextualOutputFD < 0)
      return;

   va_start (VA, Format);
   (void) vsprintf (Buf, Format, VA);
   va_end (VA);
   L = strlen (Buf);
   Buf [ L ++ ] = '\n';
   Buf [ L ] = '\0';
#if ! defined WIN32
   (void) write (XMI_TextualOutputFD, Buf, strlen (Buf));
   (void) fsync (XMI_TextualOutputFD);
#else
   (void) _write (XMI_TextualOutputFD, Buf, strlen (Buf));
   (void) _commit (XMI_TextualOutputFD);
#endif
}

#undef FUNCTION
