#include "xm.h"

struct XMI_Service
{
   XMI_Address Addresses [XMI_MaxServiceEntries];
   Int32       Count;
   Int32       LastServiceIndex;
};

/************************************************************
 * XM_Service_Init
 ************************************************************/

#define FUNCTION "XM_Service_Init"

XM_ErrorCode
XM_Service_Init (XM_Service* Service)
{
   XM_Service NewService;

   if (Service == NULL)
      return XME_BadParam;

   NewService = (XM_Service) malloc (sizeof (struct XMI_Service));

   if (NewService == NULL)
      return XME_NoMemory;

   (void) memset (NewService, 0, sizeof (struct XMI_Service));
   NewService->Count = 0;
   NewService->LastServiceIndex = 0;

   * Service = NewService;
   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_Service_Destroy
 ************************************************************/

#define FUNCTION "XM_Service_Destroy"

XM_ErrorCode
XM_Service_Destroy (XM_Service Service)
{
   Uint32 Pos;

   if (Service == NULL)
      return XME_BadParam;

   for (Pos = 0; Pos < XMI_MaxServiceEntries; Pos ++)
   {
      if (Service->Addresses [Pos].Host != NULL)
         free (Service->Addresses [Pos].Host);
   }

   (void) memset (Service, 0, sizeof (struct XMI_Service));
   free (Service);

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XM_Service_AddEntry
 ************************************************************/

#define FUNCTION "XM_Service_AddEntry"

XM_ErrorCode
XM_Service_AddEntry (XM_Service Service, const char* Host, Int32 Port)
{
   XMI_Address* Address;
   char*        Copy;

   if (Service == NULL || Host == NULL || Port < 1)
      return XME_BadParam;

   if (Service->Count >= (Int32) XMI_MaxServiceEntries)
      return XME_NoRoom;

   Copy = (char*) malloc (strlen (Host) + 1);
   if (Copy == NULL)
      return XME_NoMemory;
   strcpy (Copy, Host);

   Address = & Service->Addresses [Service->Count ++];
   Address->Host = Copy;
   Address->Port = Port;

   return XME_NoError;
}

#undef FUNCTION

/************************************************************
 * XMI_Service_GetEntryAddress
 ************************************************************/

#define FUNCTION "XMI_Service_GetEntryAddress"

XMI_Address*
XMI_Service_GetEntryAddress (XM_Service Service, Int32 Entry)
{
   if (Service == NULL || Entry < 0 || Entry >= (Int32) XMI_MaxServiceEntries)
      return NULL;

   return & Service->Addresses [Entry];
}

#undef FUNCTION

/************************************************************
 * XMI_GetLastIndex
 ************************************************************/

#define FUNCTION "XMI_GetLastIndex"

Int32
XMI_GetLastIndex (XM_Service Service)
{
  if (Service == NULL)
    abort ();
  
  return Service->LastServiceIndex;
}

#undef FUNCTION


/************************************************************
 * XMI_SetLastIndex
 ************************************************************/

#define FUNCTION "XMI_SetLastIndex"

void
XMI_SetLastIndex (XM_Service Service, Int32 LastIndex)
{
  if (Service == NULL)
    abort();

  Service->LastServiceIndex = LastIndex;
}

#undef FUNCTION

/************************************************************
 * XMI_Service_CountEntries
 ************************************************************/

#define FUNCTION "XMI_Service_CountEntries"

Int32
XMI_Service_CountEntries (XM_Service Service)
{
   if (Service == NULL)
      return (Int32) XME_BadParam;

   if (Service->Count < 0 || Service->Count > (Int32) XMI_MaxServiceEntries)
      return (Int32) XME_InternalError;

   return Service->Count;
}

#undef FUNCTION
