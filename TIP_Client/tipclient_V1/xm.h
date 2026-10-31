#ifndef XM_H__
#define XM_H__

#include "xmapi.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
//#include <assert.h>
#include <errno.h>

#ifndef WIN32

#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/poll.h>
#include <sys/time.h>

#ifndef TRUE
#define TRUE                                (Boolean) 1
#endif
#ifndef FALSE
#define FALSE                               (Boolean) 0
#endif

#else
#include <winsock2.h>
#include <time.h>
#endif /* ! WIN32 */

#if defined (__GNUC__)
#define INLINE inline
#elif defined (WIN32)
#define INLINE __inline
#else
#define INLINE
#endif

#define XMI_MaxServiceEntries    10U
#define XMI_ReadBufferSize    32768U

typedef unsigned int   Uint32;
typedef unsigned short Uint16;
typedef unsigned char  Byte;
typedef Int32          Boolean;

typedef struct XMI_Address_Tag
{
   char* Host;
   Int32 Port;
}
XMI_Address;

struct XMI_Crypt
{
   XMP_EA Algorithm;
   Int32  KeyId;
   void*  State;
};
#define XMI_Crypt_GetKeyId(c)    (c)->KeyId
#define XMI_Crypt_GetEA(c)       (c)->Algorithm


#define XMI_GetBodyLen(h)  (((const Byte*)(h))[2] << 8 | ((const Byte*)(h))[3])
#define XMI_GetExtHeaderLen(h)  (((const Byte*) (h)) [1])
#define XMI_GetFullLen(h)       (XMI_GetBodyLen(h) + XMI_GetExtHeaderLen(h))
#define XMI_GetFrameLen(h)      (XMP_HeaderSize + XMI_GetFullLen(h))
#define XMI_GetHeaderType(h)    ((XMP_Type)(((const Byte*) (h)) [0]))


/* xmapi.c */

void         XMI_Print (const char* Format, ...);


/* xmservice.c */

XMI_Address* XMI_Service_GetEntryAddress (XM_Service Service, Int32 Entry);
Int32        XMI_Service_CountEntries (XM_Service);

Int32 XMI_GetLastIndex (XM_Service Service);
void XMI_SetLastIndex (XM_Service Service, Int32 LastIndex);

/* xmcrypt.c */

Int32        XMI_Crypt_GetAlignment (XM_Crypt Crypt, Int32 Len);

/* xmdes.c */

XM_ErrorCode XMI_DES_Init    (XM_Crypt, const void*, Int32);
XM_ErrorCode XMI_DES_Destroy (XM_Crypt);

XM_ErrorCode XMI_DES_Decrypt (XM_Crypt, void*, const void*, Int32);
XM_ErrorCode XMI_DES_Encrypt (XM_Crypt, void*, const void*, Int32);


/* xmrc4.c */

XM_ErrorCode XMI_RC4_Init    (XM_Crypt, const void*, Int32);
XM_ErrorCode XMI_RC4_Destroy (XM_Crypt);

XM_ErrorCode XMI_RC4_Decrypt (XM_Crypt, void*, const void*, Int32);
XM_ErrorCode XMI_RC4_Encrypt (XM_Crypt, void*, const void*, Int32);


/* xmirand.c */

typedef Int32 XMI_Random [2];

void         XMI_Random_Srand (unsigned int Seed, XMI_Random State);
Int32        XMI_Random_Uniform (Int32 Low, Int32 High, XMI_Random State);


/* xmiwinsock.c */

#ifdef WIN32

Boolean      XMI_WinSockInit (void);

#endif  /* WIN32 */

#endif  /* XM_H__ */
