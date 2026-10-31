/*
 * This pseudo-random number generator is identical to the one in the
 * drand48() family of functions, but the interface isn't compatible.
 */

#include "xm.h"

#if defined (WIN32)
typedef unsigned __int64   Uint64;
#define ULL(x) x ## ui64
#elif defined (Tru64)
typedef unsigned long      Uint64;
#define ULL(x) x ## UL
#else
typedef unsigned long long Uint64;
#define ULL(x) x ## ULL
#endif

/* Three xors swaps the values of two words without an auxiliary word. */
#define Swap(a, b) (a)^=(b), (b)^=(a), (a)^=(b)

static INLINE void   XMI_Random_Next (Uint16* Xsubi);
static INLINE double XMI_Random_Erand (Uint16* Xsubi);

/************************************************************
 * XMI_Random_Srand
 ************************************************************/

#define FUNCTION "XMI_Random_Srand"

void
XMI_Random_Srand (Uint32 Seed, XMI_Random Xsubi)
{
   Uint16* x = (Uint16*) Xsubi;

   x [0] = (Uint16) 0x330e;
   x [1] = (Uint16) ((Uint32) Seed & 0xffff);         /* Low 16  */
   x [2] = (Uint16) (((Uint32) Seed >> 16) & 0xffff); /* High 16 */
}

#undef FUNCTION

/************************************************************
 * XMI_Random_Uniform
 ************************************************************/

#define FUNCTION "XMI_Random_Uniform"

Int32
XMI_Random_Uniform (Int32 Low, Int32 High, XMI_Random Xsubi)
{
   Int32 Rand;

   if (High == Low)
      return Low;

   if (High < Low)
      Swap (High, Low);

   Rand = (Int32) (XMI_Random_Erand ((Uint16*) Xsubi) * (High - Low + 1));
   return Low + Rand;
}

#undef FUNCTION

/************************************************************
 * XMI_Random_Next
 ************************************************************/

#define FUNCTION "XMI_Random_Next"

static INLINE void
XMI_Random_Next (Uint16* Xsubi)
{
   Uint64 x = (Uint64) Xsubi [2] << 32 | (Uint64) Xsubi [1] << 16 | Xsubi [0];
   Uint64 Res = (ULL(0x5deece66d) * x + 0xb) % ULL(0x1000000000000);

   Xsubi [0] = (Uint16) (Res & 0xffff);
   Xsubi [1] = (Uint16) ((Res >> 16) & 0xffff);
   Xsubi [2] = (Uint16) ((Res >> 32) & 0xffff);
}

#undef FUNCTION

/************************************************************
 * XMI_Random_Erand
 ************************************************************/

#define FUNCTION "XMI_Random_Erand"

static INLINE double
XMI_Random_Erand (Uint16* Xsubi)
{
   const double two16m = 1.0 / ( 1L << 16);

   XMI_Random_Next (Xsubi);
   return two16m * ( two16m * (two16m * Xsubi [0] + Xsubi [1]) + Xsubi [2]);
}

#undef FUNCTION
