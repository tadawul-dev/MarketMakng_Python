/**************************************************************************** 
 * Exchange Message API (XM)                                                * 
 **************************************************************************** 
 *                                                                          * 
 * Revision: 40.7                                                           * 
 * Date: 2004-03-03                                                         * 
 *                                                                          * 
 * This file was generated at 2004-03-22 (14:38:23)                         * 
 * Generator Version: 1.16                                                  * 
 *                                                                          * 
 **************************************************************************** 
 * >> General info and copyright goes here <<                               * 
 ****************************************************************************/


#ifndef _xmapi_h_
#define _xmapi_h_

char ErrMsg[256];
#define assert(x)	if ((x) < 0) { sprintf(ErrMsg,"Error while assigning: %s", x); return FALSE; }
/**************************************************************************** 
 * Forward Declarations                                                     * 
 ****************************************************************************/

#define XMP_MaxMessageLength                3072
#define XMP_HeaderSize                      4
#define XMP_MaxExtHdrSize                   255
#define XMP_MaxCryptKeySize                 16
#define XM_NoTimeout                        ((XM_Timespan) -1)
#define XM_NoKeyId                          -1
#define XM_AnySourcePort                    0

#ifndef INT32_DEFINED


/**************************************************************************** 
 * Int32                                                                    * 
 ****************************************************************************/

typedef int Int32;
#endif


/**************************************************************************** 
 * XM_Timespan                                                              * 
 ****************************************************************************/

typedef 
Int32 XM_Timespan
;


/**************************************************************************** 
 * XM_SessionMode                                                           * 
 ****************************************************************************/

typedef enum XM_SessionMode
{
   XM_SessionMode_Cooked,
   XM_SessionMode_Raw
}
XM_SessionMode;


/**************************************************************************** 
 * XM_ErrorCode                                                             * 
 ****************************************************************************/

typedef enum XM_ErrorCode
{
   XME_NoError = 0,
   XME_NoMemory = -1,
   XME_BadParam = -2,
   XME_BadLength = -3,
   XME_NoRoom = -4,
   XME_NoServiceEntries = -5,
   XME_NotSupported = -6,
   XME_NetworkError = -8,
   XME_Timeout = -9,
   XME_BufferTooSmall = -10,
   XME_InternalError = -11,
   XME_NotInitialized = -12,
   XME_NoMoreEntries = -13,
   XME_DC_EndOfFile = -14,
   XME_DC_BadSize = -15,
   XME_DC_BadBitStream = -16,
   XME_DC_BadOpcode = -17,
   XME_DC_BadHeaderType = -18,
   XME_DC_BadPhraseLength = -19,
   XME_DC_BadPhraseOffset = -20,
   XME_BadAlgorithm = -21,
   XME_BadKeyLength = -22,
   XME_WeakKey = -23
}
XM_ErrorCode;


/**************************************************************************** 
 * XMP_Type                                                                 * 
 ****************************************************************************/

typedef enum XMP_Type
{
   XMP_Type_None = 0x00,
   XMP_Type_XTP_2 = 0x04,
   XMP_Type_BinaryRecord = 0x05,
   XMP_Type_BinaryTagged = 0x06,
   XMP_Type_KeyExchange = 0x07,
   XMP_Type_Encrypted = 0x81,
   XMP_Type_Compressed = 0x82
}
XMP_Type;


/**************************************************************************** 
 * XMP_Tag                                                                  * 
 ****************************************************************************/

typedef enum XMP_Tag
{
   XMP_Tag_None = 0,
   XMP_Tag_ClientTxnId = 1,
   XMP_Tag_ServiceTime = 2,
   XMP_Tag_KeepAlive = 3,
   XMP_Tag_Timestamp = 4,
   XMP_Tag_KeyId = 5,
   XMP_Tag_QueueAlert = 6,
   XMP_Tag_CompressionAlgorithm = 7,
   XMP_Tag_EncryptionAlgorithm = 8,
   XMP_Tag_SessionState = 9,
   XMP_Tag_ProtocolVersion = 0xa
}
XMP_Tag;


/**************************************************************************** 
 * XMP_CA                                                                   * 
 ****************************************************************************/

typedef enum XMP_CA
{
   XMP_CA_None = 0,
   XMP_CA_LZ1 = 1
}
XMP_CA;


/**************************************************************************** 
 * XMP_EA                                                                   * 
 ****************************************************************************/

typedef enum XMP_EA
{
   XMP_EA_None = 0,
   XMP_EA_RC4 = 1,
   XMP_EA_DES = 2
}
XMP_EA;


/**************************************************************************** 
 * XMP_SS                                                                   * 
 ****************************************************************************/

typedef enum XMP_SS
{
   XMP_SS_None = 0,
   XMP_SS_Offline = 1,
   XMP_SS_Catchup = 2,
   XMP_SS_Online = 3
}
XMP_SS;


/**************************************************************************** 
 * XMP_PV                                                                   * 
 ****************************************************************************/

typedef enum XMP_PV
{
   XMP_PV_None = 0,
   XMP_PV_Current = 1
}
XMP_PV;


/**************************************************************************** 
 * XM_Service                                                               * 
 ****************************************************************************/

typedef 
struct XMI_Service* XM_Service
;


/**************************************************************************** 
 * XM_Session                                                               * 
 ****************************************************************************/

typedef 
struct XMI_Session* XM_Session
;


/**************************************************************************** 
 * XM_DC                                                                    * 
 ****************************************************************************/

typedef 
struct XMI_DC* XM_DC
;


/**************************************************************************** 
 * XM_Crypt                                                                 * 
 ****************************************************************************/

typedef 
struct XMI_Crypt* XM_Crypt
;


/**************************************************************************** 
 * Initialization                                                           * 
 ****************************************************************************/
#ifdef __cplusplus
extern "C" {
#endif


XM_ErrorCode XM_Init (Int32 TextualOutputFD);


XM_ErrorCode XM_InitEntropy (const void* Entropy, Int32 Size);


/**************************************************************************** 
 * Error Diagnostics                                                        * 
 ****************************************************************************/


const char* XM_GetErrorMessage (XM_ErrorCode ErrorCode);


/**************************************************************************** 
 * Service                                                                  * 
 ****************************************************************************/


XM_ErrorCode XM_Service_Init (XM_Service* Service);


XM_ErrorCode XM_Service_Destroy (XM_Service Service);


XM_ErrorCode XM_Service_AddEntry (XM_Service Service,
                                  const char* Host,
                                  Int32 Port);


/**************************************************************************** 
 * Session                                                                  * 
 ****************************************************************************/


XM_ErrorCode XM_Session_ConnectService (XM_Session* Session,
                                        XM_Service Service,
                                        XM_SessionMode SessionMode,
                                        XM_Crypt OptionalCrypt,
                                        Int32 SourcePort,
                                        XM_Timespan ConnectTimeout);


XM_ErrorCode XM_Session_ConnectAddress (XM_Session* Session,
                                        const char* Host,
                                        Int32 Port,
                                        XM_SessionMode SessionMode,
                                        XM_Crypt OptionalCrypt,
                                        Int32 SourcePort,
                                        XM_Timespan ConnectTimeout);


XM_ErrorCode XM_Session_Disconnect (XM_Session Session);


XM_ErrorCode XM_Session_GetAddress (XM_Session Session,
                                    const char** Host,
                                    Int32* Port);


Int32 XM_Session_TestDistribution (XM_Session* Sessions,
                                   Int32 Count,
                                   XM_Session* Candidate);


XM_ErrorCode XM_Session_SetRecvTimeout (XM_Session Session,
                                        XM_Timespan Timeout);


XM_ErrorCode XM_Session_Send (XM_Session Session, const void* Message);


Int32 XM_Session_Recv (XM_Session Session, void* Reply);


Int32 XM_Session_MultiRecv (XM_Session Session, void** Replies, Int32 Max);


XM_ErrorCode XM_Session_KeyExchange (XM_Session Session,
                                     XM_Crypt KeyExchangeCrypt,
                                     XM_Crypt* Decrypt,
                                     XM_Crypt* Encrypt);


XM_ErrorCode XM_Session_SetRecvBufferSize (XM_Session Session,
                                           Int32 BufferSize);


/**************************************************************************** 
 * Decompression                                                            * 
 ****************************************************************************/


XM_ErrorCode XM_DC_Init (XM_DC* DC, XMP_CA CA);


XM_ErrorCode XM_DC_Destroy (XM_DC DC);


Int32 XM_DC_Decompress (XM_DC DC,
                        void* OutData,
                        const void* InData,
                        Int32 InSize);


/**************************************************************************** 
 * Encryption                                                               * 
 ****************************************************************************/


XM_ErrorCode XM_Crypt_Init (XM_Crypt* Crypt,
                            XMP_EA EA,
                            const void* Key,
                            Int32 KeySize,
                            Int32 KeyId);


XM_ErrorCode XM_Crypt_Destroy (XM_Crypt Crypt);


XM_ErrorCode XM_Crypt_Encrypt (XM_Crypt Crypt,
                               void* OutData,
                               Int32* OutSize,
                               const void* InData,
                               Int32 InSize);


XM_ErrorCode XM_Crypt_Decrypt (XM_Crypt Crypt,
                               void* OutData,
                               const void* InData,
                               Int32 Size);


/**************************************************************************** 
 * XMP                                                                      * 
 ****************************************************************************/


Int32 XMP_GetBodyType (const void* Message);
Int32 XMP_GetBodyLen (const void* MessageIn);


Int32 XMP_GetFrameLen (const void* Message);

Int32 XMP_GetExtHeaderLen (const void* Message);

const void* XMP_GetBody (const void* Message, Int32* BodyLen);


const void* XMP_GetNextFrame (const void* Buffer, Int32* RemainingLen);


XM_ErrorCode XMP_GetExtHdrEntry (const void* Message,
                                 Int32* Offset,
                                 XMP_Tag* Tag,
                                 const void** Data,
                                 Int32* DataLen);


XM_ErrorCode XMP_AddExtHdrEntry (void* ExtHdr,
                                 Int32* ExtHdrLen,
                                 XMP_Tag Tag,
                                 const void* Data,
                                 Int32 DataLen);


XM_ErrorCode XMP_CreateMessage (void* Buffer,
                                Int32 BufferSize,
                                XMP_Type Type,
                                const void* ExtHdr,
                                Int32 ExtHdrLen,
                                const void* Data,
                                Int32 DataLen);


XM_ErrorCode XMP_SetHeader (void* Buffer,
                            XMP_Type Type,
                            Int32 ExtHdrLen,
                            Int32 DataLen);
#ifdef __cplusplus
}
#endif


#endif /* _xmapi_h_ */
