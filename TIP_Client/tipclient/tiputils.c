/**
 * This is a sample TIP client.
 *
 * Copyright (c) 2008 OMX AB.
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OMX AB ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL OMX AB BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

/*****************************************************************
 * Definitions
 */

/*****************************************************************
 * Include files
 */

#include "tipclient.h"

/*****************************************************************
 * Type definitions
 */

/*****************************************************************
 * Module variables
 */

/*****************************************************************
 * Module function prototypes
 */
/*
static Boolean GetNextTag(char** src, char** dst);
static Boolean GetNextValue(char** src, char** dst);
*/
/*****************************************************************
 * Functions start here
 */

/*****************************************************************
 *****************************************************************
 ** PUBLIC INTERFACE
 *****************************************************************
 *****************************************************************/

/**
 * Append an integer tag
 */

void
AppendIntTag(char* msg, const char* tag, int num)
{
  char numstr[64];

  sprintf(numstr, "%d", num);

  AppendTag(msg, tag, numstr);
}

/**
 * Append a tag to a string
 */

Boolean
AppendTag(char* msg, const char* tag, const char* value)
{
  assert( msg != NULL );
  assert( tag != NULL );

  strcat(msg, tag);

  if ( value != NULL )
  {
    strcat(msg, value);
  }

  strcat(msg, TIP_DELIMITER_STRING);
}

/**
 * Parse a TIP message into an array of tag/value
 * pairs. 
 */
/*
#define MODULE "ParseMsg"

Boolean
ParseMsg(TIP_Msg* msg, const char* buffer, int len)
{
  int        nullValue = FALSE;
  int        n         = 0;
  char       c         = 0;
  char*      src       = NULL;
  char*      dst       = NULL;

  assert(msg != NULL);

  memcpy(msg->inputmsg, buffer, len);

  msg->length = len;

  msg->inputmsg[len] = 0;
*/
  /*
   * Verify that line termination is correct.
   */
/* if (msg->inputmsg[msg->length-1] != TIP_ENDOFMSG)
  {
    fprintf(stderr, "Message not correctly terminated [%.*s]\n", msg->length, msg->inputmsg);

    return FALSE;
  }

  msg->numtags = 0;
  msg->inputmsg[msg->length-1] = 0;

  src = msg->inputmsg;
  dst = msg->msgType;

  if (! GetNextTag(& src, & dst))
    return FALSE;

  if (*src++ != TIP_DELIMITER)
  {
    fprintf(stderr, "Missing field delimiter\n");

    return FALSE;
  }

  dst = msg->buf;
  msg->tags[n] = dst;

  while ( GetNextTag(& src, & dst) )
  {
    msg->values[n] = dst;

    if (! GetNextValue(& src, & dst) )
      return FALSE;

    ++n;
    msg->tags[n] = dst;
  }

  msg->numtags = n;

  return TRUE;
}

#undef MODULE
*/
/**
 * Find a tag and return a pointer to the tag value
 */
/*
#define MODULE "FindTag"

char*
FindTag(TIP_Msg* ptags, const char* ftag)
{
  int i = 0;

  for (i = 0; i < ptags->numtags; ++i)
  {
    if (strcmp(ptags->tags[i], ftag) == 0)
      return ptags->values[i];
  }

  return NULL;
}

#undef MODULE
*/
/**
 * Return the next tag
 */
/*
#define MODULE "GetNextTag"

static Boolean
GetNextTag(char** src, char** dst)
{
  Boolean rc   = TRUE;
  char    c    = 0;
  char*   sptr = *src;
  char*   dptr = *dst;

  while ((rc = (*sptr != 0)))
  {
    c = *sptr++;

    if (! isalpha(c))
    {
      fprintf(stderr, "Invalid character in tag. '%c'\n", c);

      return FALSE;
    }

    *dptr++ = c;

    if (islower(c))
    {
      *dptr++ = 0;
      break;
    }
  }

  *src = sptr;
  *dst = dptr;

  return rc;
}

#undef MODULE
*/
/**
 * Return the next value
 */
/*
#define MODULE "GetNextValue"

static Boolean
GetNextValue(char** src, char** dst)
{
  Boolean rc   = TRUE;
  char    c    = 0;
  char*   sptr = *src;
  char*   dptr = *dst;

  while ((rc = (*sptr != 0)))
  {
    c = *sptr++;

    if (c == TIP_ENDOFMSG)
    {
      fprintf(stderr, "Unexpected end-of-message in field value\n");

      return FALSE;
    }

    if (c == TIP_DELIMITER)
    {
      *dptr++ = 0;
      break;
    }

    if (c == TIP_ESCAPE)
    {
      c = *sptr++;
    }
    
    *dptr++ = c;
  }

  *src = sptr;
  *dst = dptr;

  return rc;
}

#undef MODULE
*/
