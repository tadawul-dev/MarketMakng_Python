#include "xm.h"

#define RC4_MINKEYSIZE 1
#define RC4_MAXKEYSIZE XMP_MaxCryptKeySize
#define RC4_ALIGNMENT  1
#define DES_MINKEYSIZE 8
#define DES_MAXKEYSIZE 8
#define DES_ALIGNMENT  8

typedef struct CryptInfoTag /* one entry per algorithm */
{
   const Int32   MinCryptKeySize;
   const Int32   MaxCryptKeySize;
   const Int32   AlignSize;            /* Must be a power of 2 */
   XM_ErrorCode  (*Init) (XM_Crypt, const void*, Int32);
   XM_ErrorCode  (*Destroy) (XM_Crypt);
   XM_ErrorCode  (*Decrypt) (XM_Crypt, void*, const void*, Int32);
   XM_ErrorCode  (*Encrypt) (XM_Crypt, void*, const void*, Int32);
}
CryptInfo;

static const CryptInfo CryptInfoTable [] =
{
   { RC4_MINKEYSIZE, RC4_MAXKEYSIZE, RC4_ALIGNMENT,
     XMI_RC4_Init, XMI_RC4_Destroy, XMI_RC4_Decrypt, XMI_RC4_Encrypt },
   { DES_MINKEYSIZE, DES_MAXKEYSIZE, DES_ALIGNMENT,
     XMI_DES_Init, XMI_DES_Destroy, XMI_DES_Decrypt, XMI_DES_Encrypt }
};

#define IsValidAlgorithm(a)    ((a) > XMP_EA_None && (a) <= XMP_EA_DES)

/* Alignment (assumes a is a power of 2) */
#define AlignedLength(l, a) \
        ((Int32)(((Uint32)(l) + ((Uint32)(a) - 1U)) & ~((Uint32)(a) - 1U)))

#define GetCryptInfo(ea)       (& CryptInfoTable [(int)(ea) - 1])

/************************************************************
 * XM_Crypt_Init
 ************************************************************/

#define FUNCTION "XM_Crypt_Init"

XM_ErrorCode
XM_Crypt_Init (XM_Crypt* Crypt, XMP_EA Algorithm, const void* KeyIn,
               Int32 KeySize, Int32 KeyId)
{
   const CryptInfo*  Info;
   const Byte*       Key = (const Byte*) KeyIn;
   XM_ErrorCode      Code;
   XM_Crypt          NewCrypt;

   if (! IsValidAlgorithm (Algorithm))
      return XME_BadAlgorithm;

   Info = GetCryptInfo (Algorithm);

   if (KeySize < Info->MinCryptKeySize)
      return XME_BadKeyLength;

   if (KeySize > Info->MaxCryptKeySize)
      KeySize = Info->MaxCryptKeySize;    /* Excessive bytes are ignored. */

   NewCrypt = (XM_Crypt) malloc (sizeof (struct XMI_Crypt));

   if (NewCrypt == NULL)
      return XME_NoMemory;

   NewCrypt->Algorithm = Algorithm;
   NewCrypt->KeyId = KeyId;
   NewCrypt->State = NULL;

   Code = (* Info->Init) (NewCrypt, Key, KeySize);

   if (Code != XME_NoError)
      free (NewCrypt);
   else
      * Crypt = NewCrypt;

   return Code;
}

#undef FUNCTION

/************************************************************
 * XM_Crypt_Destroy
 ************************************************************/

#define FUNCTION "XM_Crypt_Destroy"

XM_ErrorCode
XM_Crypt_Destroy (XM_Crypt Crypt)
{
   if (Crypt == NULL)
      return XME_BadParam;

   if (! IsValidAlgorithm (Crypt->Algorithm))
      return XME_BadParam;

   (* (GetCryptInfo (Crypt->Algorithm))->Destroy) (Crypt);
   free (Crypt);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_Crypt_Encrypt
 ************************************************************/

#define FUNCTION "XM_Crypt_Encrypt"

XM_ErrorCode
XM_Crypt_Encrypt (XM_Crypt Crypt, void* Out, Int32* OutSizeOut, const void* In,
                  Int32 Size)
{
   const CryptInfo*  Info;
   Int32             OutSize;
   XM_ErrorCode      Code;

   if (Crypt == NULL)
      return XME_BadParam;

   if (! IsValidAlgorithm (Crypt->Algorithm))
      return XME_BadParam;

   Info = GetCryptInfo (Crypt->Algorithm);

   OutSize = AlignedLength (Size, Info->AlignSize);
   Code = (* Info->Encrypt) (Crypt, Out, In, OutSize);

   if (Code == XME_NoError && OutSizeOut != NULL)
      * OutSizeOut = OutSize;

   return Code;
}

#undef FUNCTION

/************************************************************
 * XM_Crypt_Decrypt
 ************************************************************/

#define FUNCTION "XM_Crypt_Decrypt"

XM_ErrorCode
XM_Crypt_Decrypt (XM_Crypt Crypt, void* Out, const void* In, Int32 Size)
{
   const CryptInfo*  Info;

   if (Crypt == NULL)
      return XME_BadParam;

   if (! IsValidAlgorithm (Crypt->Algorithm))
      return XME_BadParam;

   Info = GetCryptInfo (Crypt->Algorithm);

   return (* Info->Decrypt) (Crypt, Out, In,
                             AlignedLength (Size, Info->AlignSize));
}

#undef FUNCTION

/************************************************************
 * XMI_Crypt_GetAlignment
 ************************************************************/

#define FUNCTION "XMI_Crypt_GetAlignment"

Int32
XMI_Crypt_GetAlignment (XM_Crypt Crypt, Int32 Len)
{
   if (! IsValidAlgorithm (Crypt->Algorithm))
      return -1;

   return AlignedLength (Len, (GetCryptInfo (Crypt->Algorithm))->AlignSize);
}

#undef FUNCTION
