/* Copyright (C) 1995-1998 Eric Young (eay@cryptsoft.com)
 * All rights reserved.
 *
 * This package is an SSL implementation written
 * by Eric Young (eay@cryptsoft.com).
 * The implementation was written so as to conform with Netscapes SSL.
 *
 * This library is free for commercial and non-commercial use as long as
 * the following conditions are aheared to.  The following conditions
 * apply to all code found in this distribution, be it the RC4, RSA,
 * lhash, DES, etc., code; not just the SSL code.  The SSL documentation
 * included with this distribution is covered by the same copyright terms
 * except that the holder is Tim Hudson (tjh@cryptsoft.com).
 *
 * Copyright remains Eric Young's, and as such any Copyright notices in
 * the code are not to be removed.
 * If this package is used in a product, Eric Young should be given attribution
 * as the author of the parts of the library used.
 * This can be in the form of a textual message at program startup or
 * in documentation (online or textual) provided with the package.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    "This product includes cryptographic software written by
 *     Eric Young (eay@cryptsoft.com)"
 *    The word 'cryptographic' can be left out if the rouines from the library
 *    being used are not cryptographic related :-).
 * 4. If you include any Windows specific code (or a derivative thereof) from
 *    the apps directory (application code) you must include an acknowledgement:
 *    "This product includes software written by Tim Hudson (tjh@cryptsoft.com)"
 *
 * THIS SOFTWARE IS PROVIDED BY ERIC YOUNG ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * The licence and distribution terms for any publically available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution licence
 * [including the GNU Public Licence.]
 */

#include "xm.h"

typedef struct DESI_State* DES_State;

#define DES_ENCRYPT     1
#define DES_DECRYPT     0

typedef Byte des_cblock [8];

typedef struct des_ks_struct
{
   union
   {
      des_cblock cblock;
      /* make sure things are correct size on machines with 8 byte longs */
      Uint32 deslong[2];
   } ks;
   int weak_key;
} des_key_schedule [16];

struct DESI_State
{
   Byte key  [8];
   Byte ivec [8];

   des_key_schedule schedule;
};

static void des_encrypt (Uint32* data, des_key_schedule ks, int enc);
static void des_cbc_encrypt (const unsigned char *in, unsigned char *out,
                             long length, des_key_schedule schedule,
                             des_cblock ivec, int enc);
static int des_is_weak_key (const Byte* key);
static int des_set_key (const Byte* key, des_key_schedule schedule);


/************************************************************
 * XMI_DES_Init
 ************************************************************/

#define FUNCTION "XMI_DES_Init"

XM_ErrorCode
XMI_DES_Init (XM_Crypt Crypt, const void* KeyIn, Int32 KeySize)
{
   DES_State     State;

   if (Crypt == NULL || Crypt->State != NULL)
      return XME_BadParam;

   if (KeySize != 8)
      return XME_BadKeyLength;

   State = (DES_State) malloc (sizeof (struct DESI_State));
   if (State == NULL)
      return XME_NoMemory;

   if (des_set_key ((const Byte*) KeyIn, State->schedule) == 0)
   {
      free (State);
      return XME_WeakKey;
   }
   (void) memset (State->ivec, 0, sizeof (State->ivec));

   Crypt->State = (void*) State;
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_DES_Destroy
 ************************************************************/

#define FUNCTION "XMI_DES_Destroy"

XM_ErrorCode
XMI_DES_Destroy (XM_Crypt Crypt)
{
   DES_State  State;

   if (Crypt == NULL || Crypt->State == NULL)
      return XME_BadParam;

   State = (DES_State) Crypt->State;

   (void) memset (State, 0, sizeof (struct DESI_State));
   free (State);

   Crypt->State = NULL;

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_DES_Encrypt
 ************************************************************/

#define FUNCTION "XMI_DES_Encrypt"

XM_ErrorCode
XMI_DES_Encrypt (XM_Crypt Crypt, void* Out, const void* In, Int32 Size)
{
   DES_State State;

   if (Crypt == NULL || Crypt->State == NULL)
      return XME_BadParam;

   State = (DES_State) Crypt->State;
   des_cbc_encrypt ((const Byte *) In, (Byte*) Out, Size, State->schedule,
                    State->ivec, DES_ENCRYPT);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_DES_Decrypt
 ************************************************************/

#define FUNCTION "XMI_DES_Decrypt"

XM_ErrorCode
XMI_DES_Decrypt (XM_Crypt Crypt, void* Out, const void* In, Int32 Size)
{
   DES_State State;

   if (Crypt == NULL || Crypt->State == NULL)
      return XME_BadParam;

   State = (DES_State) Crypt->State;
   des_cbc_encrypt ((const Byte*) In, (Byte*) Out, Size, State->schedule,
                    State->ivec, DES_DECRYPT);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * DES Internals
 ************************************************************/

#if defined (Tru64)
#define UL(x) x ## U
#define L(x)  x
#else
#define UL(x) x ## UL
#define L(x)  x ## L
#endif

#define ITERATIONS     16

#define CBC_ENC_C__DONT_UPDATE_IV

#define c2l(c,l)	(l =((Uint32)(*((c)++))),        \
			 l|=((Uint32)(*((c)++)))<< L(8), \
			 l|=((Uint32)(*((c)++)))<<L(16), \
			 l|=((Uint32)(*((c)++)))<<L(24))

/* NOTE - c is not incremented as per c2l */
#define c2ln(c,l1,l2,n)	{ \
			c+=n; \
			l1=l2=0; \
			switch (n) { \
			case 8: l2 =((Uint32)(*(--(c))))<<L(24); \
			case 7: l2|=((Uint32)(*(--(c))))<<L(16); \
			case 6: l2|=((Uint32)(*(--(c))))<< L(8); \
			case 5: l2|=((Uint32)(*(--(c))));        \
			case 4: l1 =((Uint32)(*(--(c))))<<L(24); \
			case 3: l1|=((Uint32)(*(--(c))))<<L(16); \
			case 2: l1|=((Uint32)(*(--(c))))<< L(8); \
			case 1: l1|=((Uint32)(*(--(c))));        \
				} \
			}

#define l2c(l,c)	(*((c)++)=(unsigned char)(((l)     )&0xff),   \
			 *((c)++)=(unsigned char)(((l)>> L(8))&0xff), \
			 *((c)++)=(unsigned char)(((l)>>L(16))&0xff), \
			 *((c)++)=(unsigned char)(((l)>>L(24))&0xff))

/* replacements for htonl and ntohl since I have no idea what to do
 * when faced with machines with 8 byte longs. */

#define n2l(c,l)	(l =((Uint32)(*((c)++)))<<L(24), \
			 l|=((Uint32)(*((c)++)))<<L(16), \
			 l|=((Uint32)(*((c)++)))<< L(8), \
			 l|=((Uint32)(*((c)++))))

#define l2n(l,c)	(*((c)++)=(unsigned char)(((l)>>L(24))&0xff), \
			 *((c)++)=(unsigned char)(((l)>>L(16))&0xff), \
			 *((c)++)=(unsigned char)(((l)>> L(8))&0xff), \
			 *((c)++)=(unsigned char)(((l)     )&0xff))

/* NOTE - c is not incremented as per l2c */
#define l2cn(l1,l2,c,n)	{ \
			c+=n; \
			switch (n) { \
			case 8: *(--(c))=(unsigned char)(((l2)>>L(24))&0xff); \
			case 7: *(--(c))=(unsigned char)(((l2)>>L(16))&0xff); \
			case 6: *(--(c))=(unsigned char)(((l2)>> L(8))&0xff); \
			case 5: *(--(c))=(unsigned char)(((l2)     )&0xff);   \
			case 4: *(--(c))=(unsigned char)(((l1)>>L(24))&0xff); \
			case 3: *(--(c))=(unsigned char)(((l1)>>L(16))&0xff); \
			case 2: *(--(c))=(unsigned char)(((l1)>> L(8))&0xff); \
			case 1: *(--(c))=(unsigned char)(((l1)     )&0xff);   \
				} \
			}

#if defined(WIN32)
#define	ROTATE(a,n)	(_lrotr(a,n))
#else
#define	ROTATE(a,n)	(((a)>>(n))+((a)<<(32-(n))))
#endif

#define LOAD_DATA(R,S,u,t,E0,E1,tmp) \
	u=R^s[S  ]; \
	t=R^s[S+1]

#define D_ENCRYPT(LL,R,S) \
{ \
   unsigned int u1,u2,u3; \
   LOAD_DATA(R,S,u,t,E0,E1,u1); \
   u2=(int)u>>L(8); \
   u1=(int)u&0xfc; \
   u2&=0xfc; \
   t=ROTATE(t,4); \
   u>>=L(16); \
   LL^= *(const Uint32 *)(SP      +u1); \
   LL^= *(const Uint32 *)(SP+0x200+u2); \
   u3=(int)(u>>L(8)); \
   u1=(int)u&0xfc; \
   u3&=0xfc; \
   LL^= *(const Uint32 *)(SP+0x400+u1); \
   LL^= *(const Uint32 *)(SP+0x600+u3); \
   u2=(int)t>>L(8); \
   u1=(int)t&0xfc; \
   u2&=0xfc; \
   t>>=L(16); \
   LL^= *(const Uint32 *)(SP+0x100+u1); \
   LL^= *(const Uint32 *)(SP+0x300+u2); \
   u3=(int)t>>L(8); \
   u1=(int)t&0xfc; \
   u3&=0xfc; \
   LL^= *(const Uint32 *)(SP+0x500+u1); \
   LL^= *(const Uint32 *)(SP+0x700+u3); \
}

#define PERM_OP(a,b,t,n,m) \
   ((t)=((((a)>>(n))^(b))&(m)),	(b)^=(t), (a)^=((t)<<(n)))

#define IP(l,r) \
{ \
   Uint32 tt; \
   PERM_OP(r,l,tt, 4,L(0x0f0f0f0f)); \
   PERM_OP(l,r,tt,16,L(0x0000ffff)); \
   PERM_OP(r,l,tt, 2,L(0x33333333)); \
   PERM_OP(l,r,tt, 8,L(0x00ff00ff)); \
   PERM_OP(r,l,tt, 1,L(0x55555555)); \
}

#define FP(l,r) \
{ \
   Uint32 tt; \
   PERM_OP(l,r,tt, 1,L(0x55555555)); \
   PERM_OP(r,l,tt, 8,L(0x00ff00ff)); \
   PERM_OP(l,r,tt, 2,L(0x33333333)); \
   PERM_OP(r,l,tt,16,L(0x0000ffff)); \
   PERM_OP(l,r,tt, 4,L(0x0f0f0f0f)); \
}


/**********************************************************************
 * des_cbc_encrypt
 **********************************************************************/

static void
des_cbc_encrypt (const unsigned char *in, unsigned char *out, long length,
                   des_key_schedule schedule, des_cblock ivec, int enc)
{
   Uint32 tin0,tin1;
   Uint32 tout0,tout1,xor0,xor1;
   long l=length;
   Uint32 tin[2];
   Byte *iv;

   iv = ivec;

   if (enc)
   {
      c2l(iv,tout0);
      c2l(iv,tout1);

      for (l-=8; l>=0; l-=8)
      {
         c2l(in,tin0);
         c2l(in,tin1);

         tin0^=tout0; tin[0]=tin0;
         tin1^=tout1; tin[1]=tin1;

         des_encrypt((Uint32 *)tin, schedule, DES_ENCRYPT);

         tout0=tin[0]; l2c(tout0,out);
         tout1=tin[1]; l2c(tout1,out);
      }
      if (l != -8)
      {
         c2ln(in,tin0,tin1,l+8);
         tin0^=tout0; tin[0]=tin0;
         tin1^=tout1; tin[1]=tin1;
         des_encrypt((Uint32 *)tin,schedule,DES_ENCRYPT);
         tout0=tin[0]; l2c(tout0,out);
         tout1=tin[1]; l2c(tout1,out);
      }
#ifndef CBC_ENC_C__DONT_UPDATE_IV
      iv = &(*ivec)[0];
      l2c(tout0,iv);
      l2c(tout1,iv);
#endif
   }
   else
   {
      c2l(iv,xor0);
      c2l(iv,xor1);
      for (l-=8; l>=0; l-=8)
      {
         c2l(in,tin0); tin[0]=tin0;
         c2l(in,tin1); tin[1]=tin1;
         des_encrypt((Uint32 *)tin,schedule,DES_DECRYPT);
         tout0=tin[0]^xor0;
         tout1=tin[1]^xor1;
         l2c(tout0,out);
         l2c(tout1,out);
         xor0=tin0;
         xor1=tin1;
      }
      if (l != -8)
      {
         c2l(in,tin0); tin[0]=tin0;
         c2l(in,tin1); tin[1]=tin1;
         des_encrypt((Uint32 *)tin,schedule,DES_DECRYPT);
         tout0=tin[0]^xor0;
         tout1=tin[1]^xor1;
         l2cn(tout0,tout1,out,l+8);
#ifndef CBC_ENC_C__DONT_UPDATE_IV
         xor0=tin0;
         xor1=tin1;
#endif
      }
#ifndef CBC_ENC_C__DONT_UPDATE_IV
      iv = &(*ivec)[0];
      l2c(xor0,iv);
      l2c(xor1,iv);
#endif
   }
   tin0=tin1=tout0=tout1=xor0=xor1=0;
   tin[0]=tin[1]=0;
}

/**********************************************************************
 * des_set_key
 **********************************************************************/

#define NUM_WEAK_KEY	16

static des_cblock weak_keys [NUM_WEAK_KEY] =
{
   /* weak keys */
   {0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01},
   {0xFE,0xFE,0xFE,0xFE,0xFE,0xFE,0xFE,0xFE},
   {0x1F,0x1F,0x1F,0x1F,0x0E,0x0E,0x0E,0x0E},
   {0xE0,0xE0,0xE0,0xE0,0xF1,0xF1,0xF1,0xF1},
   /* semi-weak keys */
   {0x01,0xFE,0x01,0xFE,0x01,0xFE,0x01,0xFE},
   {0xFE,0x01,0xFE,0x01,0xFE,0x01,0xFE,0x01},
   {0x1F,0xE0,0x1F,0xE0,0x0E,0xF1,0x0E,0xF1},
   {0xE0,0x1F,0xE0,0x1F,0xF1,0x0E,0xF1,0x0E},
   {0x01,0xE0,0x01,0xE0,0x01,0xF1,0x01,0xF1},
   {0xE0,0x01,0xE0,0x01,0xF1,0x01,0xF1,0x01},
   {0x1F,0xFE,0x1F,0xFE,0x0E,0xFE,0x0E,0xFE},
   {0xFE,0x1F,0xFE,0x1F,0xFE,0x0E,0xFE,0x0E},
   {0x01,0x1F,0x01,0x1F,0x01,0x0E,0x01,0x0E},
   {0x1F,0x01,0x1F,0x01,0x0E,0x01,0x0E,0x01},
   {0xE0,0xFE,0xE0,0xFE,0xF1,0xFE,0xF1,0xFE},
   {0xFE,0xE0,0xFE,0xE0,0xFE,0xF1,0xFE,0xF1}
};

static int
des_is_weak_key (const Byte* key)
{
   int i;

   for (i=0; i<NUM_WEAK_KEY; i++)
      if (memcmp(weak_keys[i],key,sizeof(des_cblock)) == 0)
         return 1;

   return 0;
}

#define HPERM_OP(a,t,n,m) \
   ((t)=((((a)<<(16-(n)))^(a))&(m)), (a)=(a)^(t)^(t>>(16-(n))))

static const Uint32 des_skb [8][64] =
{
   {
/* for C bits (numbered as per FIPS 46) 1 2 3 4 5 6 */
      UL(0x00000000),UL(0x00000010),UL(0x20000000),UL(0x20000010),
      UL(0x00010000),UL(0x00010010),UL(0x20010000),UL(0x20010010),
      UL(0x00000800),UL(0x00000810),UL(0x20000800),UL(0x20000810),
      UL(0x00010800),UL(0x00010810),UL(0x20010800),UL(0x20010810),
      UL(0x00000020),UL(0x00000030),UL(0x20000020),UL(0x20000030),
      UL(0x00010020),UL(0x00010030),UL(0x20010020),UL(0x20010030),
      UL(0x00000820),UL(0x00000830),UL(0x20000820),UL(0x20000830),
      UL(0x00010820),UL(0x00010830),UL(0x20010820),UL(0x20010830),
      UL(0x00080000),UL(0x00080010),UL(0x20080000),UL(0x20080010),
      UL(0x00090000),UL(0x00090010),UL(0x20090000),UL(0x20090010),
      UL(0x00080800),UL(0x00080810),UL(0x20080800),UL(0x20080810),
      UL(0x00090800),UL(0x00090810),UL(0x20090800),UL(0x20090810),
      UL(0x00080020),UL(0x00080030),UL(0x20080020),UL(0x20080030),
      UL(0x00090020),UL(0x00090030),UL(0x20090020),UL(0x20090030),
      UL(0x00080820),UL(0x00080830),UL(0x20080820),UL(0x20080830),
      UL(0x00090820),UL(0x00090830),UL(0x20090820),UL(0x20090830),
   },
   {
/* for C bits (numbered as per FIPS 46) 7 8 10 11 12 13 */
      UL(0x00000000),UL(0x02000000),UL(0x00002000),UL(0x02002000),
      UL(0x00200000),UL(0x02200000),UL(0x00202000),UL(0x02202000),
      UL(0x00000004),UL(0x02000004),UL(0x00002004),UL(0x02002004),
      UL(0x00200004),UL(0x02200004),UL(0x00202004),UL(0x02202004),
      UL(0x00000400),UL(0x02000400),UL(0x00002400),UL(0x02002400),
      UL(0x00200400),UL(0x02200400),UL(0x00202400),UL(0x02202400),
      UL(0x00000404),UL(0x02000404),UL(0x00002404),UL(0x02002404),
      UL(0x00200404),UL(0x02200404),UL(0x00202404),UL(0x02202404),
      UL(0x10000000),UL(0x12000000),UL(0x10002000),UL(0x12002000),
      UL(0x10200000),UL(0x12200000),UL(0x10202000),UL(0x12202000),
      UL(0x10000004),UL(0x12000004),UL(0x10002004),UL(0x12002004),
      UL(0x10200004),UL(0x12200004),UL(0x10202004),UL(0x12202004),
      UL(0x10000400),UL(0x12000400),UL(0x10002400),UL(0x12002400),
      UL(0x10200400),UL(0x12200400),UL(0x10202400),UL(0x12202400),
      UL(0x10000404),UL(0x12000404),UL(0x10002404),UL(0x12002404),
      UL(0x10200404),UL(0x12200404),UL(0x10202404),UL(0x12202404),
   },
   {
/* for C bits (numbered as per FIPS 46) 14 15 16 17 19 20 */
      UL(0x00000000),UL(0x00000001),UL(0x00040000),UL(0x00040001),
      UL(0x01000000),UL(0x01000001),UL(0x01040000),UL(0x01040001),
      UL(0x00000002),UL(0x00000003),UL(0x00040002),UL(0x00040003),
      UL(0x01000002),UL(0x01000003),UL(0x01040002),UL(0x01040003),
      UL(0x00000200),UL(0x00000201),UL(0x00040200),UL(0x00040201),
      UL(0x01000200),UL(0x01000201),UL(0x01040200),UL(0x01040201),
      UL(0x00000202),UL(0x00000203),UL(0x00040202),UL(0x00040203),
      UL(0x01000202),UL(0x01000203),UL(0x01040202),UL(0x01040203),
      UL(0x08000000),UL(0x08000001),UL(0x08040000),UL(0x08040001),
      UL(0x09000000),UL(0x09000001),UL(0x09040000),UL(0x09040001),
      UL(0x08000002),UL(0x08000003),UL(0x08040002),UL(0x08040003),
      UL(0x09000002),UL(0x09000003),UL(0x09040002),UL(0x09040003),
      UL(0x08000200),UL(0x08000201),UL(0x08040200),UL(0x08040201),
      UL(0x09000200),UL(0x09000201),UL(0x09040200),UL(0x09040201),
      UL(0x08000202),UL(0x08000203),UL(0x08040202),UL(0x08040203),
      UL(0x09000202),UL(0x09000203),UL(0x09040202),UL(0x09040203),
   },
   {
/* for C bits (numbered as per FIPS 46) 21 23 24 26 27 28 */
      UL(0x00000000),UL(0x00100000),UL(0x00000100),UL(0x00100100),
      UL(0x00000008),UL(0x00100008),UL(0x00000108),UL(0x00100108),
      UL(0x00001000),UL(0x00101000),UL(0x00001100),UL(0x00101100),
      UL(0x00001008),UL(0x00101008),UL(0x00001108),UL(0x00101108),
      UL(0x04000000),UL(0x04100000),UL(0x04000100),UL(0x04100100),
      UL(0x04000008),UL(0x04100008),UL(0x04000108),UL(0x04100108),
      UL(0x04001000),UL(0x04101000),UL(0x04001100),UL(0x04101100),
      UL(0x04001008),UL(0x04101008),UL(0x04001108),UL(0x04101108),
      UL(0x00020000),UL(0x00120000),UL(0x00020100),UL(0x00120100),
      UL(0x00020008),UL(0x00120008),UL(0x00020108),UL(0x00120108),
      UL(0x00021000),UL(0x00121000),UL(0x00021100),UL(0x00121100),
      UL(0x00021008),UL(0x00121008),UL(0x00021108),UL(0x00121108),
      UL(0x04020000),UL(0x04120000),UL(0x04020100),UL(0x04120100),
      UL(0x04020008),UL(0x04120008),UL(0x04020108),UL(0x04120108),
      UL(0x04021000),UL(0x04121000),UL(0x04021100),UL(0x04121100),
      UL(0x04021008),UL(0x04121008),UL(0x04021108),UL(0x04121108),
   },
   {
/* for D bits (numbered as per FIPS 46) 1 2 3 4 5 6 */
      UL(0x00000000),UL(0x10000000),UL(0x00010000),UL(0x10010000),
      UL(0x00000004),UL(0x10000004),UL(0x00010004),UL(0x10010004),
      UL(0x20000000),UL(0x30000000),UL(0x20010000),UL(0x30010000),
      UL(0x20000004),UL(0x30000004),UL(0x20010004),UL(0x30010004),
      UL(0x00100000),UL(0x10100000),UL(0x00110000),UL(0x10110000),
      UL(0x00100004),UL(0x10100004),UL(0x00110004),UL(0x10110004),
      UL(0x20100000),UL(0x30100000),UL(0x20110000),UL(0x30110000),
      UL(0x20100004),UL(0x30100004),UL(0x20110004),UL(0x30110004),
      UL(0x00001000),UL(0x10001000),UL(0x00011000),UL(0x10011000),
      UL(0x00001004),UL(0x10001004),UL(0x00011004),UL(0x10011004),
      UL(0x20001000),UL(0x30001000),UL(0x20011000),UL(0x30011000),
      UL(0x20001004),UL(0x30001004),UL(0x20011004),UL(0x30011004),
      UL(0x00101000),UL(0x10101000),UL(0x00111000),UL(0x10111000),
      UL(0x00101004),UL(0x10101004),UL(0x00111004),UL(0x10111004),
      UL(0x20101000),UL(0x30101000),UL(0x20111000),UL(0x30111000),
      UL(0x20101004),UL(0x30101004),UL(0x20111004),UL(0x30111004),
   },
   {
/* for D bits (numbered as per FIPS 46) 8 9 11 12 13 14 */
      UL(0x00000000),UL(0x08000000),UL(0x00000008),UL(0x08000008),
      UL(0x00000400),UL(0x08000400),UL(0x00000408),UL(0x08000408),
      UL(0x00020000),UL(0x08020000),UL(0x00020008),UL(0x08020008),
      UL(0x00020400),UL(0x08020400),UL(0x00020408),UL(0x08020408),
      UL(0x00000001),UL(0x08000001),UL(0x00000009),UL(0x08000009),
      UL(0x00000401),UL(0x08000401),UL(0x00000409),UL(0x08000409),
      UL(0x00020001),UL(0x08020001),UL(0x00020009),UL(0x08020009),
      UL(0x00020401),UL(0x08020401),UL(0x00020409),UL(0x08020409),
      UL(0x02000000),UL(0x0A000000),UL(0x02000008),UL(0x0A000008),
      UL(0x02000400),UL(0x0A000400),UL(0x02000408),UL(0x0A000408),
      UL(0x02020000),UL(0x0A020000),UL(0x02020008),UL(0x0A020008),
      UL(0x02020400),UL(0x0A020400),UL(0x02020408),UL(0x0A020408),
      UL(0x02000001),UL(0x0A000001),UL(0x02000009),UL(0x0A000009),
      UL(0x02000401),UL(0x0A000401),UL(0x02000409),UL(0x0A000409),
      UL(0x02020001),UL(0x0A020001),UL(0x02020009),UL(0x0A020009),
      UL(0x02020401),UL(0x0A020401),UL(0x02020409),UL(0x0A020409),
   },
   {
/* for D bits (numbered as per FIPS 46) 16 17 18 19 20 21 */
      UL(0x00000000),UL(0x00000100),UL(0x00080000),UL(0x00080100),
      UL(0x01000000),UL(0x01000100),UL(0x01080000),UL(0x01080100),
      UL(0x00000010),UL(0x00000110),UL(0x00080010),UL(0x00080110),
      UL(0x01000010),UL(0x01000110),UL(0x01080010),UL(0x01080110),
      UL(0x00200000),UL(0x00200100),UL(0x00280000),UL(0x00280100),
      UL(0x01200000),UL(0x01200100),UL(0x01280000),UL(0x01280100),
      UL(0x00200010),UL(0x00200110),UL(0x00280010),UL(0x00280110),
      UL(0x01200010),UL(0x01200110),UL(0x01280010),UL(0x01280110),
      UL(0x00000200),UL(0x00000300),UL(0x00080200),UL(0x00080300),
      UL(0x01000200),UL(0x01000300),UL(0x01080200),UL(0x01080300),
      UL(0x00000210),UL(0x00000310),UL(0x00080210),UL(0x00080310),
      UL(0x01000210),UL(0x01000310),UL(0x01080210),UL(0x01080310),
      UL(0x00200200),UL(0x00200300),UL(0x00280200),UL(0x00280300),
      UL(0x01200200),UL(0x01200300),UL(0x01280200),UL(0x01280300),
      UL(0x00200210),UL(0x00200310),UL(0x00280210),UL(0x00280310),
      UL(0x01200210),UL(0x01200310),UL(0x01280210),UL(0x01280310),
   },
   {
/* for D bits (numbered as per FIPS 46) 22 23 24 25 27 28 */
      UL(0x00000000),UL(0x04000000),UL(0x00040000),UL(0x04040000),
      UL(0x00000002),UL(0x04000002),UL(0x00040002),UL(0x04040002),
      UL(0x00002000),UL(0x04002000),UL(0x00042000),UL(0x04042000),
      UL(0x00002002),UL(0x04002002),UL(0x00042002),UL(0x04042002),
      UL(0x00000020),UL(0x04000020),UL(0x00040020),UL(0x04040020),
      UL(0x00000022),UL(0x04000022),UL(0x00040022),UL(0x04040022),
      UL(0x00002020),UL(0x04002020),UL(0x00042020),UL(0x04042020),
      UL(0x00002022),UL(0x04002022),UL(0x00042022),UL(0x04042022),
      UL(0x00000800),UL(0x04000800),UL(0x00040800),UL(0x04040800),
      UL(0x00000802),UL(0x04000802),UL(0x00040802),UL(0x04040802),
      UL(0x00002800),UL(0x04002800),UL(0x00042800),UL(0x04042800),
      UL(0x00002802),UL(0x04002802),UL(0x00042802),UL(0x04042802),
      UL(0x00000820),UL(0x04000820),UL(0x00040820),UL(0x04040820),
      UL(0x00000822),UL(0x04000822),UL(0x00040822),UL(0x04040822),
      UL(0x00002820),UL(0x04002820),UL(0x00042820),UL(0x04042820),
      UL(0x00002822),UL(0x04002822),UL(0x00042822),UL(0x04042822),
   }
};

/* return 0 if key parity is odd (correct),
 * return -1 if key parity error,
 * return -2 if illegal weak key.
 */

static int
des_set_key (const Byte* key, des_key_schedule schedule)
{
   static int shifts2[16]={0,0,1,1,1,1,1,1,0,1,1,1,1,1,1,0};

   Uint32 c,d,t,s,t2;
   const Byte* in;
   Uint32 *k;
   int i;

   if (des_is_weak_key (key))
      return 0;

   k = &schedule->ks.deslong[0];
   in = key;

   c2l(in,c);
   c2l(in,d);

   PERM_OP (d,c,t,4,L(0x0f0f0f0f));
   HPERM_OP(c,t,-2,UL(0xcccc0000));
   HPERM_OP(d,t,-2,UL(0xcccc0000));
   PERM_OP (d,c,t,1,L(0x55555555));
   PERM_OP (c,d,t,8,L(0x00ff00ff));
   PERM_OP (d,c,t,1,L(0x55555555));

   d=	(((d&L(0x000000ff))<<L(16))| (d&L(0x0000ff00))     |
         ((d&L(0x00ff0000))>>L(16))|((c&UL(0xf0000000))>>L(4)));

   c&=L(0x0fffffff);

   for (i=0; i<ITERATIONS; i++)
   {
      if (shifts2[i])
      {
         c=((c>>L(2))|(c<<L(26)));
         d=((d>>L(2))|(d<<L(26)));
      }
      else
      {
         c=((c>>L(1))|(c<<L(27)));
         d=((d>>L(1))|(d<<L(27)));
      }
      c&=L(0x0fffffff);
      d&=L(0x0fffffff);

      s= des_skb[0][ (c    )&0x3f                ]|
         des_skb[1][((c>> 6)&0x03)|((c>> L(7))&0x3c)]|
         des_skb[2][((c>>13)&0x0f)|((c>>L(14))&0x30)]|
         des_skb[3][((c>>20)&0x01)|((c>>L(21))&0x06) |
                   ((c>>L(22))&0x38)];

      t= des_skb[4][ (d    )&0x3f                ]|
         des_skb[5][((d>> L(7))&0x03)|((d>> L(8))&0x3c)]|
         des_skb[6][ (d>>L(15))&0x3f                ]|
         des_skb[7][((d>>L(21))&0x0f)|((d>>L(22))&0x30)];

      /* table contained 0213 4657 */
      t2=((t<<L(16))|(s&L(0x0000ffff)))&UL(0xffffffff);
      *(k++)=ROTATE(t2,30)&UL(0xffffffff);

      t2=((s>>L(16))|(t&UL(0xffff0000)));
      *(k++)=ROTATE(t2,26)&UL(0xffffffff);
   }
   return 1;
}

static const Uint32 SPtrans [8][64] =
{
   {
/* nibble 0 */
      UL(0x02080800), UL(0x00080000), UL(0x02000002), UL(0x02080802),
      UL(0x02000000), UL(0x00080802), UL(0x00080002), UL(0x02000002),
      UL(0x00080802), UL(0x02080800), UL(0x02080000), UL(0x00000802),
      UL(0x02000802), UL(0x02000000), UL(0x00000000), UL(0x00080002),
      UL(0x00080000), UL(0x00000002), UL(0x02000800), UL(0x00080800),
      UL(0x02080802), UL(0x02080000), UL(0x00000802), UL(0x02000800),
      UL(0x00000002), UL(0x00000800), UL(0x00080800), UL(0x02080002),
      UL(0x00000800), UL(0x02000802), UL(0x02080002), UL(0x00000000),
      UL(0x00000000), UL(0x02080802), UL(0x02000800), UL(0x00080002),
      UL(0x02080800), UL(0x00080000), UL(0x00000802), UL(0x02000800),
      UL(0x02080002), UL(0x00000800), UL(0x00080800), UL(0x02000002),
      UL(0x00080802), UL(0x00000002), UL(0x02000002), UL(0x02080000),
      UL(0x02080802), UL(0x00080800), UL(0x02080000), UL(0x02000802),
      UL(0x02000000), UL(0x00000802), UL(0x00080002), UL(0x00000000),
      UL(0x00080000), UL(0x02000000), UL(0x02000802), UL(0x02080800),
      UL(0x00000002), UL(0x02080002), UL(0x00000800), UL(0x00080802),
   },
   {
/* nibble 1 */
      UL(0x40108010), UL(0x00000000), UL(0x00108000), UL(0x40100000),
      UL(0x40000010), UL(0x00008010), UL(0x40008000), UL(0x00108000),
      UL(0x00008000), UL(0x40100010), UL(0x00000010), UL(0x40008000),
      UL(0x00100010), UL(0x40108000), UL(0x40100000), UL(0x00000010),
      UL(0x00100000), UL(0x40008010), UL(0x40100010), UL(0x00008000),
      UL(0x00108010), UL(0x40000000), UL(0x00000000), UL(0x00100010),
      UL(0x40008010), UL(0x00108010), UL(0x40108000), UL(0x40000010),
      UL(0x40000000), UL(0x00100000), UL(0x00008010), UL(0x40108010),
      UL(0x00100010), UL(0x40108000), UL(0x40008000), UL(0x00108010),
      UL(0x40108010), UL(0x00100010), UL(0x40000010), UL(0x00000000),
      UL(0x40000000), UL(0x00008010), UL(0x00100000), UL(0x40100010),
      UL(0x00008000), UL(0x40000000), UL(0x00108010), UL(0x40008010),
      UL(0x40108000), UL(0x00008000), UL(0x00000000), UL(0x40000010),
      UL(0x00000010), UL(0x40108010), UL(0x00108000), UL(0x40100000),
      UL(0x40100010), UL(0x00100000), UL(0x00008010), UL(0x40008000),
      UL(0x40008010), UL(0x00000010), UL(0x40100000), UL(0x00108000),
   },{
/* nibble 2 */
      UL(0x04000001), UL(0x04040100), UL(0x00000100), UL(0x04000101),
      UL(0x00040001), UL(0x04000000), UL(0x04000101), UL(0x00040100),
      UL(0x04000100), UL(0x00040000), UL(0x04040000), UL(0x00000001),
      UL(0x04040101), UL(0x00000101), UL(0x00000001), UL(0x04040001),
      UL(0x00000000), UL(0x00040001), UL(0x04040100), UL(0x00000100),
      UL(0x00000101), UL(0x04040101), UL(0x00040000), UL(0x04000001),
      UL(0x04040001), UL(0x04000100), UL(0x00040101), UL(0x04040000),
      UL(0x00040100), UL(0x00000000), UL(0x04000000), UL(0x00040101),
      UL(0x04040100), UL(0x00000100), UL(0x00000001), UL(0x00040000),
      UL(0x00000101), UL(0x00040001), UL(0x04040000), UL(0x04000101),
      UL(0x00000000), UL(0x04040100), UL(0x00040100), UL(0x04040001),
      UL(0x00040001), UL(0x04000000), UL(0x04040101), UL(0x00000001),
      UL(0x00040101), UL(0x04000001), UL(0x04000000), UL(0x04040101),
      UL(0x00040000), UL(0x04000100), UL(0x04000101), UL(0x00040100),
      UL(0x04000100), UL(0x00000000), UL(0x04040001), UL(0x00000101),
      UL(0x04000001), UL(0x00040101), UL(0x00000100), UL(0x04040000),
   },{
/* nibble 3 */
      UL(0x00401008), UL(0x10001000), UL(0x00000008), UL(0x10401008),
      UL(0x00000000), UL(0x10400000), UL(0x10001008), UL(0x00400008),
      UL(0x10401000), UL(0x10000008), UL(0x10000000), UL(0x00001008),
      UL(0x10000008), UL(0x00401008), UL(0x00400000), UL(0x10000000),
      UL(0x10400008), UL(0x00401000), UL(0x00001000), UL(0x00000008),
      UL(0x00401000), UL(0x10001008), UL(0x10400000), UL(0x00001000),
      UL(0x00001008), UL(0x00000000), UL(0x00400008), UL(0x10401000),
      UL(0x10001000), UL(0x10400008), UL(0x10401008), UL(0x00400000),
      UL(0x10400008), UL(0x00001008), UL(0x00400000), UL(0x10000008),
      UL(0x00401000), UL(0x10001000), UL(0x00000008), UL(0x10400000),
      UL(0x10001008), UL(0x00000000), UL(0x00001000), UL(0x00400008),
      UL(0x00000000), UL(0x10400008), UL(0x10401000), UL(0x00001000),
      UL(0x10000000), UL(0x10401008), UL(0x00401008), UL(0x00400000),
      UL(0x10401008), UL(0x00000008), UL(0x10001000), UL(0x00401008),
      UL(0x00400008), UL(0x00401000), UL(0x10400000), UL(0x10001008),
      UL(0x00001008), UL(0x10000000), UL(0x10000008), UL(0x10401000),
   },{
/* nibble 4 */
      UL(0x08000000), UL(0x00010000), UL(0x00000400), UL(0x08010420),
      UL(0x08010020), UL(0x08000400), UL(0x00010420), UL(0x08010000),
      UL(0x00010000), UL(0x00000020), UL(0x08000020), UL(0x00010400),
      UL(0x08000420), UL(0x08010020), UL(0x08010400), UL(0x00000000),
      UL(0x00010400), UL(0x08000000), UL(0x00010020), UL(0x00000420),
      UL(0x08000400), UL(0x00010420), UL(0x00000000), UL(0x08000020),
      UL(0x00000020), UL(0x08000420), UL(0x08010420), UL(0x00010020),
      UL(0x08010000), UL(0x00000400), UL(0x00000420), UL(0x08010400),
      UL(0x08010400), UL(0x08000420), UL(0x00010020), UL(0x08010000),
      UL(0x00010000), UL(0x00000020), UL(0x08000020), UL(0x08000400),
      UL(0x08000000), UL(0x00010400), UL(0x08010420), UL(0x00000000),
      UL(0x00010420), UL(0x08000000), UL(0x00000400), UL(0x00010020),
      UL(0x08000420), UL(0x00000400), UL(0x00000000), UL(0x08010420),
      UL(0x08010020), UL(0x08010400), UL(0x00000420), UL(0x00010000),
      UL(0x00010400), UL(0x08010020), UL(0x08000400), UL(0x00000420),
      UL(0x00000020), UL(0x00010420), UL(0x08010000), UL(0x08000020),
   },{
/* nibble 5 */
      UL(0x80000040), UL(0x00200040), UL(0x00000000), UL(0x80202000),
      UL(0x00200040), UL(0x00002000), UL(0x80002040), UL(0x00200000),
      UL(0x00002040), UL(0x80202040), UL(0x00202000), UL(0x80000000),
      UL(0x80002000), UL(0x80000040), UL(0x80200000), UL(0x00202040),
      UL(0x00200000), UL(0x80002040), UL(0x80200040), UL(0x00000000),
      UL(0x00002000), UL(0x00000040), UL(0x80202000), UL(0x80200040),
      UL(0x80202040), UL(0x80200000), UL(0x80000000), UL(0x00002040),
      UL(0x00000040), UL(0x00202000), UL(0x00202040), UL(0x80002000),
      UL(0x00002040), UL(0x80000000), UL(0x80002000), UL(0x00202040),
      UL(0x80202000), UL(0x00200040), UL(0x00000000), UL(0x80002000),
      UL(0x80000000), UL(0x00002000), UL(0x80200040), UL(0x00200000),
      UL(0x00200040), UL(0x80202040), UL(0x00202000), UL(0x00000040),
      UL(0x80202040), UL(0x00202000), UL(0x00200000), UL(0x80002040),
      UL(0x80000040), UL(0x80200000), UL(0x00202040), UL(0x00000000),
      UL(0x00002000), UL(0x80000040), UL(0x80002040), UL(0x80202000),
      UL(0x80200000), UL(0x00002040), UL(0x00000040), UL(0x80200040),
   },
   {
/* nibble 6 */
      UL(0x00004000), UL(0x00000200), UL(0x01000200), UL(0x01000004),
      UL(0x01004204), UL(0x00004004), UL(0x00004200), UL(0x00000000),
      UL(0x01000000), UL(0x01000204), UL(0x00000204), UL(0x01004000),
      UL(0x00000004), UL(0x01004200), UL(0x01004000), UL(0x00000204),
      UL(0x01000204), UL(0x00004000), UL(0x00004004), UL(0x01004204),
      UL(0x00000000), UL(0x01000200), UL(0x01000004), UL(0x00004200),
      UL(0x01004004), UL(0x00004204), UL(0x01004200), UL(0x00000004),
      UL(0x00004204), UL(0x01004004), UL(0x00000200), UL(0x01000000),
      UL(0x00004204), UL(0x01004000), UL(0x01004004), UL(0x00000204),
      UL(0x00004000), UL(0x00000200), UL(0x01000000), UL(0x01004004),
      UL(0x01000204), UL(0x00004204), UL(0x00004200), UL(0x00000000),
      UL(0x00000200), UL(0x01000004), UL(0x00000004), UL(0x01000200),
      UL(0x00000000), UL(0x01000204), UL(0x01000200), UL(0x00004200),
      UL(0x00000204), UL(0x00004000), UL(0x01004204), UL(0x01000000),
      UL(0x01004200), UL(0x00000004), UL(0x00004004), UL(0x01004204),
      UL(0x01000004), UL(0x01004200), UL(0x01004000), UL(0x00004004),
   },
   {
/* nibble 7 */
      UL(0x20800080), UL(0x20820000), UL(0x00020080), UL(0x00000000),
      UL(0x20020000), UL(0x00800080), UL(0x20800000), UL(0x20820080),
      UL(0x00000080), UL(0x20000000), UL(0x00820000), UL(0x00020080),
      UL(0x00820080), UL(0x20020080), UL(0x20000080), UL(0x20800000),
      UL(0x00020000), UL(0x00820080), UL(0x00800080), UL(0x20020000),
      UL(0x20820080), UL(0x20000080), UL(0x00000000), UL(0x00820000),
      UL(0x20000000), UL(0x00800000), UL(0x20020080), UL(0x20800080),
      UL(0x00800000), UL(0x00020000), UL(0x20820000), UL(0x00000080),
      UL(0x00800000), UL(0x00020000), UL(0x20000080), UL(0x20820080),
      UL(0x00020080), UL(0x20000000), UL(0x00000000), UL(0x00820000),
      UL(0x20800080), UL(0x20020080), UL(0x20020000), UL(0x00800080),
      UL(0x20820000), UL(0x00000080), UL(0x00800080), UL(0x20020000),
      UL(0x20820080), UL(0x00800000), UL(0x20800000), UL(0x20000080),
      UL(0x00820000), UL(0x00020080), UL(0x20020080), UL(0x20800000),
      UL(0x00000080), UL(0x20820000), UL(0x00820080), UL(0x00000000),
      UL(0x20000000), UL(0x20800080), UL(0x00020000), UL(0x00820080),
   }
};

/**********************************************************************
 * des_encrypt
 **********************************************************************/

static void
des_encrypt (Uint32* data, des_key_schedule ks, int enc)
{
   Uint32 l,r,t,u;
   const Byte* SP = (const Byte*) SPtrans;
   int i;
   Uint32 *s;

   r=data[0];
   l=data[1];

   IP(r,l);

   r=ROTATE(r,29)&UL(0xffffffff);
   l=ROTATE(l,29)&UL(0xffffffff);

   s=ks->ks.deslong;

   if (enc)
   {
      for (i=0; i<32; i+=8)
      {
         D_ENCRYPT(l,r,i+0); /*  1 */
         D_ENCRYPT(r,l,i+2); /*  2 */
         D_ENCRYPT(l,r,i+4); /*  3 */
         D_ENCRYPT(r,l,i+6); /*  4 */
      }
   }
   else
   {
      for (i=30; i>0; i-=8)
      {
         D_ENCRYPT(l,r,i-0); /* 16 */
         D_ENCRYPT(r,l,i-2); /* 15 */
         D_ENCRYPT(l,r,i-4); /* 14 */
         D_ENCRYPT(r,l,i-6); /* 13 */
      }
   }

   /* rotate and clear the top bits on machines with 8byte longs */
   l=ROTATE(l,3)&UL(0xffffffff);
   r=ROTATE(r,3)&UL(0xffffffff);

   FP(r,l);
   data[0]=l;
   data[1]=r;
   l=r=t=u=0;
}

