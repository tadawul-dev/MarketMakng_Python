# TIP client for Java

A pure-Java client for the Nasdaq / OMX **TIP** text protocol (logon, realtime and basic-data streaming requests) on top of the **XMP** framing layer, ported from the OMX AB C sample client and its XMAPI library. No JNI, no native code. The only dependency is the **Log4j 2 API** (`log4j-api`); logging is configured with Log4j XML files. Requires Java 21 or later.

* [Overview](#overview) · [Build and test](#build-and-test) · [Using the library](#using-the-library) · [Command line](#command-line) · [Connectivity check](#connectivity-check) · [Logging](#logging) · [Migrating from the JNI wrapper](#migrating-from-the-jni-wrapper) · [Differences from the C code](#differences-from-the-c-code) · [Tests](#tests) · [Limitations](#limitations)

## Overview

```
tip.client   TIP text protocol and the public API
             TipClient        connect, logon, streaming requests, read loop, logoff
             TipMessage       parsing / building of  TYPE;TagValue;...;\n  messages
             TipClientMain    command line program (same options as the C sample)
             TipConnectCheck  connectivity verification program (see below)
tip.xm       XMP transport (what XMAPI did)
             XmSession        socket, framing, fail-over, optional encryption / compression
             XmpFrame, ExtHeader      frame and extension-header encoding
             Lz1Decompressor          LZ1 decompression with 8 KiB history across blocks
             XmCrypt          RC4 and DES-CBC, as in XMAPI
             KeyExchange      session key negotiation with a pre-shared key
             XmService        list of server addresses (max 10)
             XmError/XmException      XMAPI error codes -1 .. -23
```

Frame types handled: BinaryRecord (carries the TIP text), KeyExchange, Encrypted, Compressed. Extension headers carry the compression algorithm, queue alert, session state and timestamp.

## Build and test

Maven:

```
mvn package     # target/tip-client-1.0.0.jar, with the Log4j jars copied to target/lib, and runs the tests
java -jar target/tip-client-1.0.0.jar -a host:4000 -u uid -p pwd -R -v
```

> The `pom.xml` and the real Log4j 2 jars could not be used where this project was written (no access to Maven Central). The code and tests were compiled and run with plain `javac`/JUnit against a stand-in for the Log4j classes, so **the first thing to do on your side is `mvn test`** (or `scripts/run-tests.sh` below) to confirm the build against real Log4j 2.24.x. The additions to check are `log4j-api`/`log4j-core` 2.24.3, JUnit 4.13.2, surefire 3.5.2 and the dependency plugin.

Without Maven (JDK 21+; `junit-4.13.2`, `hamcrest-core-1.3`, `log4j-api-2.x` and `log4j-core-2.x` jars on disk):

```
scripts/run-tests.sh                                   # compile, run all tests, then the loopback self test
scripts/run-tests.sh tip.client.LoggingTest            # selected classes
LOG4J_API_JAR=/p/log4j-api-2.24.3.jar LOG4J_CORE_JAR=/p/log4j-core-2.24.3.jar JUNIT_JAR=/p/junit-4.13.2.jar HAMCREST_JAR=/p/hamcrest-core-1.3.jar scripts/run-tests.sh
```

Which jars: the library needs only `log4j-api`. `log4j-core` is needed to run the command line program (it sets the level from `-v`) and the tests; in an application you can use `log4j-core` or any other backend behind a Log4j API bridge.

## Using the library

```java
try (TipClient tip = TipClient.builder()
        .compression(true)                         // ask the server to compress
        .queueAlertMillis(500)                     // default 500
        .connectTimeout(Duration.ofSeconds(5))
        .recvTimeout(Duration.ofSeconds(35))       // default: wait forever
        .listener(new TipClient.ExtensionListener() {
            @Override public void onQueueAlert(int ms) { System.err.println("slow consumer: " + ms); }
            @Override public void onSessionState(Xmp.SessionState s, int raw) { System.err.println("state " + s); }
        })
        .build()) {

    tip.connect("10.0.0.5", 4000);                 // or connect(List<XmService.Address>) for fail-over
    TipMessage reply = tip.logon("user", "secret", null);
    if (!"0".equals(reply.get("Rc").orElse("?")))
        throw new IllegalStateException("logon refused: " + reply);

    tip.requestRealtime(1, 0, TipClient.currentDate(), TipClient.Scope.ALL);   // from sequence 1, open end
    while (true) {
        TipMessage m = tip.next();                 // nextRaw() -> String, nextBody() -> byte[]
        if (m.type().equals("EOs")) break;         // end of stream
        System.out.println(m);
    }
    tip.logoff();
}
```

| Request | Wire text |
|---|---|
| `requestRealtime(1, 0, 20261031, Scope.ALL)` | `RTr;Sn1;QDAy20261031;QSCo3;` |
| `requestRealtime(5, 9, 0, Scope.UPDATES)` | `RTr;Sn5;En9;QSCo2;` (date 0 is omitted) |
| `requestBasicData(20261031, Scope.ALL)` | `BDa;QDAy20261031;QSCo3;` |
| `logoff()` | `LGf;` |

Behaviour worth knowing:

* `logon` returns the server's reply, it does not throw on a refused logon (`Rc` is non-zero); the caller decides.
* Any I/O failure (peer closed, timeout, corrupt data) closes the client and throws `XmException` (an `IOException`) whose `error()` is the XMAPI error code. After that, call `connect` again.
* Calling methods before `connect` throws `IllegalStateException`; bad arguments throw `IllegalArgumentException` before anything is sent.
* `close()` is idempotent and may be called from another thread to unblock a thread waiting in `next()`.
* One thread should read while another may send requests; the reading side is not meant to be shared by several threads.
* Messages that contain only extension headers (session state, queue alert, timestamp) are consumed internally and reported to the listener; they are never returned from `next*()`.

### Encryption

```java
XmCrypt psk = XmCrypt.create(XmCrypt.Algorithm.RC4, preSharedKey, 42);   // key id agreed with the exchange
TipClient tip = TipClient.builder().initialCrypt(psk).build();
```

On `connect` the client performs the key exchange and then encrypts the whole session with the negotiated key. Key bytes are never logged.

## Command line

```
java -jar tip-client.jar -a host:4000 -u uid -p pwd -R [options]
```

| Option | Meaning |
|---|---|
| `-a host:port` | server address; repeat for fail-over (max 10) |
| `-u uid` / `-p pwd` | account and password |
| `-R` / `-B` | request realtime (RTr) / basic data (BDa) |
| `-s 1\|2\|3` | scope: none / updates / all (default 3) |
| `-d YYYYMMDD` | request date (default today) |
| `-x ver` | protocol version sent at logon |
| `-b n` / `-e n` | start / end sequence number (default 1 / open) |
| `-z` | enable compression |
| `-q ms` | queue alert threshold (default 500) |
| `-t secs` | fail if nothing arrives for this long (default: forever) |
| `-v`, `-vv`, `-vvv` | log INFO, DEBUG, TRACE (every message) to **stderr** |

Messages go to stdout, logs to stderr. Exit code: 0 after `EOs`, 1 on connection or protocol failure, 2 for bad usage. The password is never printed.

## Connectivity check

`tip.client.TipConnectCheck` verifies that a TIP server can be reached and that the account works, one layer at a time, so a failure points at what is broken. It requests no data, so it is safe against production and suitable for monitoring.

```
scripts/tip-check.sh -a 10.0.0.5:4000 -a 10.0.0.6:4000 -u alice -p secret -w 5
# or: java -cp "tip-client-1.0.0.jar:lib/*" tip.client.TipConnectCheck ...
```

```
TIP connectivity check: 2 address(es), logon as alice

10.0.0.5:4000
  [  OK  ] dns               1 ms  10.0.0.5
  [  OK  ] tcp               6 ms  connected to /10.0.0.5:4000
  [  OK  ] session           2 ms  compression=off encryption=off
  [  OK  ] logon            10 ms  GEn Rc=0 Rx=welcome
  [  OK  ] first-message   193 ms  received HBt
  [  OK  ] logoff            0 ms  reply GEn
  => OK

10.0.0.6:4000
  [  OK  ] dns               0 ms  10.0.0.6
  [ FAIL ] tcp               2 ms  Connection refused
  => FAILED

1 of 2 address(es) OK
```

| Step | Checks | Failure usually means |
|---|---|---|
| `dns` | host name resolves | wrong name, DNS |
| `tcp` | a TCP connection can be opened | firewall, wrong port, server down |
| `session` | XMP session; with `-E/-K` also the key exchange | wrong pre-shared key, key id or algorithm |
| `logon` | server accepts the account (needs `-u/-p`; `Rc` must be 0) | bad credentials, account locked, wrong protocol version |
| `first-message` | server sends something after logon (only with `-w secs`) | quiet feed: only a **warning** |
| `logoff` | orderly `LGf` | a missing reply is normal; only a warning if it cannot be sent |

Without `-u/-p` only `dns`, `tcp` and `session` run. Each address is tested independently. After a failed step the later steps of that address are not run.

| Option | Meaning |
|---|---|
| `-a host:port` | address to check; repeat for several servers |
| `-u uid -p pwd` | also log on (both or neither) |
| `-x ver`, `-z` | protocol version, compression |
| `-E RC4\|DES -K hex [-I keyid]` | check with encryption and this pre-shared key |
| `-T secs` / `-t secs` | connect timeout (default 5) / timeout for each server reply (default 10) |
| `-w secs` | wait this long for the first message after logon |
| `-v`, `-vv`, `-vvv` | technical log on stderr |

Exit code: **0** every address passed (warnings allowed), **1** at least one failed, **2** usage error. The password is never printed. `TipConnectCheck.check(address, options)` returns the steps as data (`Result`, `Step`, `Status`) if you want to call it from your own monitoring code.

## Logging

The library logs through the **Log4j 2 API** (`org.apache.logging.log4j.Logger`) and is configured with Log4j XML.

| Logger | What it says |
|---|---|
| `tip.client` | connecting, logon (user name only), request parameters, logoff, close, queue alerts, session state changes, read/send failures |
| `tip.client.wire` | every TIP message sent (`>>`) and received (`<<`); password shown as `PWd***` (TRACE) |
| `tip.client.main` | command line program: end of stream, fatal errors |
| `tip.xm.session` | connect attempts, fail-over, connected address, encryption on, frames sent/received (TRACE), peer close, timeouts |
| `tip.xm.keyexchange` | key exchange steps and why a reply was rejected |
| `tip.xm.crypt` | cipher creation, rejected keys (weak DES keys) |
| `tip.xm.lz1` | corrupt compressed blocks |
| `tip.xm.service` | address list changes |

| Level | Used for |
|---|---|
| `ERROR` | cannot connect to any address, key exchange failed, command line failure |
| `WARN` | one address failed (fail-over), peer closed, read/write failed, corrupt data, queue alert, key rejected |
| `INFO` | session lifecycle |
| `DEBUG` | internal steps, extension-only frames, receive timeouts |
| `TRACE` | each frame and TIP message |

A normal session produces nothing above INFO. **Never logged at any level:** passwords, pre-shared keys, session keys. TIP messages are logged through a masking `toString`, and `LoggingTest` asserts this.

### XML configuration

| File | For |
|---|---|
| [`config/log4j2.xml`](config/log4j2.xml) | **Your application.** Rolling file for everything, a separate rolling file for the wire trace, console for warnings, hot reload every 30 s. Use it with `-Dlog4j2.configurationFile=config/log4j2.xml` or copy it to your class path as `log4j2.xml`. Log directory: `-Dtip.log.dir=/var/log/tip` (default `logs`). |
| [`src/main/resources/tip-log4j2.xml`](src/main/resources/tip-log4j2.xml) | The **command line program** only (stderr, WARN). Used automatically unless you pass your own `-Dlog4j2.configurationFile`. The library jar does not contain a `log4j2.xml`, so it never fights with your application's configuration. |
| `src/test/resources/log4j2-test.xml` | The tests: keeps expected warnings off the console. |

Typical changes:

```xml
<!-- full message trace, written to tip-wire.log only -->
<Logger name="tip.client.wire" level="trace" additivity="false"><AppenderRef ref="wire-file"/></Logger>
<!-- debug the transport only -->
<Logger name="tip.xm" level="debug"/>
```

Command line: `-v` sets the `tip` loggers to INFO, `-vv` DEBUG, `-vvv` TRACE (on top of the XML file), output on stderr.

## Migrating from the JNI wrapper

| JNI `CNative...` | Java |
|---|---|
| `Initialize` | `TipClient.builder()...build()` (no globals, no signal handlers) |
| `Connect(host, port, compression)` | `connect(host, port)`; compression set on the builder |
| `Logon(user, pwd, ver)` | `logon(user, pwd, ver)` returns a parsed `TipMessage` |
| `StreamingRequest(type, scope, date, start, end)` | `streamingRequest(...)`, `requestRealtime`, `requestBasicData` |
| `GetNextMessage` | `nextRaw()` / `next()` / `nextBody()` |
| `Logoff` | `logoff()` (sends `LGf`; keep reading for the reply, as before) |
| `Disconnect` | `close()` |
| `GetErrorMessage` | exceptions (`XmException` carries an `XmError`) |
| `GetSignalReceived` | not needed: close the client or interrupt the reading thread |

## Differences from the C code

* Field values are escaped when sent (`;`, `\`, newline), so a password containing `;` no longer corrupts the logon message. The C code sent values verbatim.
* Errors are exceptions instead of an error buffer; nothing is global, so several clients can run in one JVM.
* Text is decoded as UTF-8 by default (what `NewStringUTF` did). For another encoding use `.charset(...)` (e.g. `windows-1256`) or read bytes with `nextBody()`.
* No receive timeout by default (the C client's 35 s value was never applied). Set `.recvTimeout(Duration)` to detect dead connections.
* The extension headers sent with each request (compression, queue alert) are built per connection; the C code appended them again on every reconnect.
* A basic-data request needs no sequence numbers.
* Key-exchange nonces come from `SecureRandom` instead of a seeded drand48.
* LZ1 phrases pointing before the start of the window are rejected with `DC_BAD_PHRASE_OFFSET` (undefined behaviour in C).

## Tests

262 JUnit 4 tests; they use only loopback sockets and finish in a few seconds. They all pass with `scripts/run-tests.sh` (repeated runs, no flakiness) against a stand-in for the Log4j classes; see the note under *Build and test* about running them against real Log4j.

| Class | Tests | Covers |
|---|---|---|
| `XmErrorTest` | 6 | error codes and messages match XMAPI |
| `ExtHeaderTest` | 15 | encode/decode, limits (255-byte entries, 255-byte header), truncated entries |
| `XmpFrameTest` | 11 | header layout, length limits, round trips, bad input |
| `Lz1DecompressorTest` | 21 | literals, phrases, overlapping copies, history across blocks, corrupt streams, golden vectors from the C code |
| `XmCryptTest` | 23 | RC4/DES behaviour, continuing key stream, padding, weak keys, golden vectors from the C code |
| `XmServiceTest` | 8 | address list, limit of 10, round-robin order |
| `KeyExchangeTest` | 8 | successful exchange, every kind of bad reply, timeout |
| `XmSessionTest` | 38 | connect, fail-over, send/recv, fragmentation, compression, encryption, closing, timeouts, oversize frames |
| `TipMessageTest` | 24 | syntax, escaping, tag rules, round trips, malformed input, password masking |
| `TipClientTest` | 47 | every API call and error path against a scripted server; headers sent; listener events; charsets; reconnect; fail-over; encrypted and compressed sessions; 5000-message stream; close from another thread |
| `TipConnectCheckTest` | 24 | command line, every step on its own (dns, tcp, session, logon, first message, logoff), refused logon, timeouts, hang-ups, garbage replies, encryption (good and wrong key), several addresses, report text, exit codes, no password in the output |
| `TipClientMainTest` | 19 | all options, every error case, full runs with exit codes, fail-over, verbosity-to-level mapping, packaged log configuration |
| `LoggingTest` | 18 | levels, what is logged where, no secrets, no WARNING on the happy path, WARNING/ERROR on each failure, message formatting |

Test infrastructure (`src/test/java/tip/testutil`): `FakeServer` is a scriptable XMP/TIP server (including key-exchange faults), `Lz1Writer` builds compressed blocks, `TestData` has helpers and a log capture (a programmatic Log4j appender). The golden files in `src/test/resources/golden` were produced by running the **original C code** (RC4, DES, LZ1) on thousands of random inputs, so the Java implementations are checked byte-for-byte without needing a C compiler. `tip.LoopbackSelfTest` is an older stand-alone end-to-end program that `scripts/run-tests.sh` also runs.

## Limitations

* **Not tested against a real exchange.** Behaviour was derived from the C sources and checked against a fake server. The C sample never used the key exchange, so that part has no real-world reference at all.
* RC4 and single DES are weak by modern standards; they are included only because XMP defines them.
* The Maven build and the tests against the real Log4j 2 jars have not been executed (see above); in particular `TestData.LogCapture` uses the Log4j-core programmatic configuration API.

## License

The protocol logic follows the OMX AB sample sources, which are BSD-licensed; see [NOTICE](NOTICE). The DES and RC4 implementations are written fresh, not ported from the C files.
