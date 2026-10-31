#
# This is a sample TIP client.
#
# Copyright (c) 2008 OMX AB.
# All rights reserved
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#    * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#    * Neither the name of the <organization> nor the
#       names of its contributors may be used to endorse or promote products
#       derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY OMX AB ``AS IS'' AND ANY
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL OMX AB BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
#

SOURCES	= tipclient.c tiputils.c \
          xmapi.c xmcrypt.c xmdc.c xmdes.c xmirand.c xmisession.c \
          xmkeyexch.c xmrc4.c xmservice.c xmsession.c xmxmp.c
EXE = libtipclient.so

OBJECTS = $(SOURCES:.c=.o)
CC	= gcc
CFLAGS = -g -fPIC -I /WebsiteJobs/java8/jdk1.8.0_161/include/ -I /WebsiteJobs/java8/jdk1.8.0_161/include/linux/ 
LDFLAGS = $(CFLAGS) -B-static -shared -static-libgcc

#LIBS	= -lrt -lnsl -lpthread
LIBS	= -lpthread

.PHONY: clean

all: $(EXE)

%.o: %.c
	$(CC) -c $(CFLAGS) $<

$(EXE): $(OBJECTS) 
	$(CC) $(LDFLAGS) -o $(EXE) $(LIBS) $(OBJECTS)

clean:
	rm -f tipclient $(OBJECTS)


