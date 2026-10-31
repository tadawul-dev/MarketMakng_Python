#ifndef XMSESSION_H__
#define XMSESSION_H__

typedef struct XMI_CookedInfoTag* XMI_CookedInfo;

struct XMI_Session
{
   XM_Timespan     RecvTimeout;
   XM_Timespan     ConnectTimeout;
   XM_SessionMode  Mode;

   XMI_CookedInfo  Cooked; /* Set if Mode == XM_Session_Cooked */

   XM_Service      Service;
   Int32           ServiceEntry;

   Int32           FD;
   char            ReadBuffer [XMI_ReadBufferSize];
   Int32           Used;
   Int32           ReadIndex;
   Int32           WriteIndex;
};


struct XMI_CookedInfoTag
{
   XM_Crypt     Decrypt;
   XM_Crypt     Encrypt;
   XM_DC        DC;

   Byte         Buffers [2][XMP_MaxMessageLength];
   Byte*        Buffer;
   Byte*        AltBuffer;

   const Byte*  Xmp;
   Int32        Used;
};


/* xmisession.c */

XM_ErrorCode XMI_Connect (XM_Session, const char* Host,
                          Int32 DestPort, Int32 SourcePort);
XM_ErrorCode XMI_Disconnect (XM_Session);
XM_ErrorCode XMI_SendMessage (XM_Session, const Byte* Message);
Int32        XMI_RecvMessages (XM_Session, Byte* Buffer [], Int32 Max,
                               XM_Timespan Timeout);
XM_ErrorCode XMI_SetRecvBufferSize (XM_Session Session, Int32 BufferSize);

#define XMI_MessageInBuffer(Sess)      \
    ((Sess)->Used >= XMP_HeaderSize && \
     (Sess)->Used >= XMI_GetFrameLen((Sess)->ReadBuffer + (Sess)->ReadIndex))

#endif  /* XMSESSION_H__ */
