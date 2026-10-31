#include "xm.h"

#define BOXSIZE 256

typedef struct RC4_State_Tag
{
   Int32  I;
   Int32  J;
   Byte   Box [BOXSIZE];
}
RC4_State;

#define mod256(a) ((a) & 0xff)

#define Swap(a, b, t)    (t)=(a), (a)=(b), (b)=(t)

static INLINE Int32 GetNextKeyByte (RC4_State*);

/************************************************************
 * GetNextKeyByte
 ************************************************************/

#define FUNCTION "GetNextKeyByte"

static INLINE Int32
GetNextKeyByte (RC4_State* State)
{
   Byte   Temp;
   Byte*  S = State->Box;

   State->I = mod256 (State->I + 1);
   State->J = mod256 (State->J + S [State->I]);
   Swap (S [State->I], S [State->J], Temp);

   return (Int32) (S [mod256 (S [State->I] + S [State->J])]);
}

#undef FUNCTION

/************************************************************
 * XMI_RC4_Init
 ************************************************************/

#define FUNCTION "XMI_RC4_Init"

XM_ErrorCode
XMI_RC4_Init (XM_Crypt Crypt, const void* KeyIn, Int32 KeySize)
{
   const Byte*  Key = (const Byte*) KeyIn;
   RC4_State*   State;
   Byte         Box [BOXSIZE];
   Byte*        S;
   Int32        I;
   Int32        J;

   if (Crypt == NULL || Crypt->State != NULL)
      return XME_BadParam;

   if (KeySize <= 0 || KeySize > XMP_MaxCryptKeySize)
      return XME_BadParam;

   State = (RC4_State*) malloc (sizeof (RC4_State));
   if (State == NULL)
      return XME_NoMemory;

   Crypt->State = (void*) State;

   S = State->Box;

   for (I = 0, J = 0; I < BOXSIZE; I ++, J ++)
   {
      if (J >= KeySize) /* restart if less than BOXSIZE bytes */
         J = 0;

      S [I] = (Byte) I;
      Box [I] = Key [J];
   }

   for (I = 0, J = 0; I < BOXSIZE; I ++)
   {
      Byte Temp;

      J = mod256 (J + S [I] + Box [I]);
      Swap (S [I], S [J], Temp);
   }

   State->I = 0;
   State->J = 0;

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_RC4_Destroy
 ************************************************************/

#define FUNCTION "XMI_RC4_Destroy"

XM_ErrorCode
XMI_RC4_Destroy (XM_Crypt Crypt)
{
   if (Crypt == NULL || Crypt->State == NULL)
      return XME_BadParam;

   (void) memset (Crypt->State, 0, sizeof (RC4_State));
   free (Crypt->State);
   Crypt->State = NULL;
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_RC4_Encrypt
 ************************************************************/

#define FUNCTION "XMI_RC4_Encrypt"

XM_ErrorCode
XMI_RC4_Encrypt (XM_Crypt Crypt, void* Out, const void* In, Int32 Size)
{
   RC4_State*   State;
   const Byte*  InData = (const Byte*) In;
   Byte*        OutData = (Byte*) Out;
   int          Pos;

   if (Crypt == NULL || Crypt->State == NULL)
      return XME_BadParam;

   State = (RC4_State*) Crypt->State;

   for (Pos = 0 ; Pos < Size ; Pos ++)
   {
      OutData [Pos] = (Byte) (InData [Pos] ^ (Byte) GetNextKeyByte (State));
   }
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_RC4_Decrypt
 ************************************************************/

#define FUNCTION "XMI_RC4_Decrypt"

XM_ErrorCode
XMI_RC4_Decrypt (XM_Crypt Crypt, void* Out, const void* In, Int32 Size)
{
   RC4_State*   State;
   const Byte*  InData = (const Byte*) In;
   Byte*        OutData = (Byte*) Out;
   int          Pos;

   if (Crypt == NULL || Crypt->State == NULL)
      return XME_BadParam;

   State = (RC4_State*) Crypt->State;

   for (Pos = 0 ; Pos < Size ; Pos ++)
   {
      OutData [Pos] = (Byte) (InData [Pos] ^ (Byte) GetNextKeyByte (State));
   }
   return XME_NoError;
}

#undef FUNCTION
