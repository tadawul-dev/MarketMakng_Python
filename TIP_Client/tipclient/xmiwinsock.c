#ifdef WIN32

#include "xm.h"

#include <winsock2.h>

/************************************************************
 * XMI_WinSockInit
 ************************************************************/

#define FUNCTION "XMI_WinSockInit"

Boolean
XMI_WinSockInit (void)
{
   WORD    wVersionRequested;
   WSADATA wsaData;
   int     err;

   /* Request support for 2.0 */
   wVersionRequested = MAKEWORD (2, 0);

   err = WSAStartup (wVersionRequested, & wsaData);
   if (err != 0)
   {
      //fprintf (stderr, "Could not find a usable Winsock DLL\n");
      return FALSE;
   }

   if (LOBYTE (wsaData.wVersion) != 2 || HIBYTE (wsaData.wVersion) != 0)
   {
      //fprintf (stderr, "Could not find a usable Winsock DLL\n");
      WSACleanup();
      return FALSE;
   }

   return TRUE;
}

#undef FUNCTION

#endif    /* WIN32 */
