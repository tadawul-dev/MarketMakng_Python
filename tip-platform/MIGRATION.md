# TIP platform 2.1: migration notes

This tree contains two Maven modules, built together from the root `pom.xml`:

| Module | What it is |
|---|---|
| `tip-client` | Your pure-Java TIP/XMP client (from `TIP_Client_Java_v1`), unchanged except a `test-jar` so the listener can reuse its `FakeServer` |
| `tip-web-listener` | The listener: Java 21 (25-ready), no JNI, Log4j 2 API |

```bash
mvn clean package                    # builds tip-client, then tip-web-listener (+ target/lib/)
mvn clean package -Djava.release=25  # once a JDK 25 is installed
tip-web-listener/bin/run-listener.sh /path/to/efp.ini
```

### Opening in IntelliJ

1. Unzip, then **File > Open** and choose **`tip-platform/pom.xml`** (the root pom, not `tip-web-listener`). Pick **Open as Project**. IntelliJ then imports both Maven modules, and the listener sees `tip-client` as a module.
2. **File > Project Structure > Project > SDK = 21**. The build targets Java 21 by default (`java.release` in the root pom), because JDK 21 is what is installed; all code runs on 21.
3. If the Maven tool window shows red dependencies, click **Reload All Maven Projects**. The first import downloads `ojdbc11` and JUnit 5. Log4j 2.24.3 and the Maven plugins are already in your local repository.
4. Build: **Build > Build Project**, or `mvn clean package` from the root.

When JDK 25 is installed: set it as the Project SDK and change `<java.release>` in the root pom to 25. `bin/run-listener.sh` adds the JDK-25-only flag `-XX:+UseCompactObjectHeaders` automatically when it runs on Java 25.

## What changed in 2.1

### 1. libtipclient.so replaced by the pure-Java TIP client

* `AnalyserReader` no longer has `native` methods and no longer loads `XMP_LIBRARY`. There is nothing to compile on the server and no `--enable-native-access` flag. The reading is done by the new `TipFeedReader`, which uses `tip.client.TipClient`.
* Same flow as before:
  1. Connect, with the primary then the secondary address, and `TARGIN_CONNECT_MAX_RETRY_COUNT` attempts.
  2. Log on, then replay the feed log (file-based recovery).
  3. Send `RTr` from the last sequence, then read.
  4. On a lost connection, reconnect. On a stop, send `LGf` and wait for its reply.
* **Proven identical to the old library.** I rebuilt the original `libtipclient.so` from `TIP_Client/tipclient_V1` in the build sandbox and ran it and the new client against the same in-process TIP server:
  * The logon and streaming-request text on the wire are identical.
  * **All 407 messages are identical**: ASCII, Windows-1256 Arabic, UTF-8 Arabic, a message with no newline, an embedded NUL, and 400 random-byte messages.
  * The decoder (`FeedTextDecoder`, `FEED_CHARSET=JNI`) also matched `strncpy` + HotSpot `NewStringUTF` on **3,000,000 random byte strings with 0 differences**. That includes a HotSpot quirk: stray UTF-8 continuation bytes shorten the string. So the Arabic text reaching the DB layer, and its Cp1252 -> Windows-1256 fix-up, behave exactly as before.
* Improvements over the JNI reader:
  * **Reconnect:** an in-process reconnect resumes from the last sequence number. The old reader re-pushed the whole day's feed log into the queues on every reconnect.
  * **Resume point:** the resume sequence comes from the messages themselves (tag `n`), not from the parsers' asynchronous counter.
  * **Feed log:** it stores the exact bytes received, so there is no charset conversion on the hot path, and replay uses the same decoder as live data.
  * **Shutdown:** a JVM shutdown hook (SIGTERM/SIGINT/SIGHUP) logs off, lets the DB queues drain (`SHUTDOWN_DRAIN_SECS`) and gives the writers time to commit. The C library trapped SIGINT/SIGHUP/SIGTSTP; SIGTSTP (Ctrl-Z) is no longer a stop signal.
  * **Optional settings:** `TIP_RECV_TIMEOUT_SECS` detects a dead connection; the default is 0, which means wait forever, as before. `TIP_COMPRESSION` turns on LZ1. `FEED_CHARSET=windows-1256` decodes Arabic correctly at the source and switches the legacy fix-up off.
  * **Security:** the old C library printed the logon request, **including the TIP password**, to stderr and to `/tmp/Output.txt`. The Java client never logs the password. **Check existing servers for `/tmp/Output.txt` and rotate the TIP password.**

### 2. Logging: Log4j 2 API directly, XML configuration

* The `sa.com.tadawul.website.tip.log` wrapper (`Log`, `Log4jWrapper`, `LogFactory`) is gone. All 50 classes use `org.apache.logging.log4j.Logger` / `LogManager` directly.
* **517** concatenating `info`/`debug`/`warn` calls were converted to Log4j 2 parameterized messages (`"Seq# {} ...", seq`).
* `log4j.properties` and `commons-logging.properties` are not used. Everything is configured in `tip-web-listener/config/log4j2.xml`, and the launcher passes `-Dlog4j2.configurationFile`. That file covers:
  * the listener (`tiplog.log`)
  * the TIP client (`tip.client`, `tip.xm.*`)
  * a separate wire-trace file (`tip-wire.log`): set `tip.client.wire` to `trace` to record every TIP message
  * async appenders
  * hot reload every 30 s

  The application's shutdown hook flushes the logs last (`shutdownHook="disable"` in the XML), so shutdown messages are not lost.

### 3. Data structures

* **Symbol registries.** The `symbol -> array index` maps in `TIPDataObjects` and the reference maps in `LoadInitialData` are now `SymbolMap`: a `ConcurrentHashMap` that keeps `HashMap`'s null-key/null-value behaviour. They were `HashMap`s written by the low-volume parser (new BDSh/BDBo/BDIn symbols) **while** four other parser threads read them, which is a data race.
* **Logon reply.** `TIP_MsgVO` (256-slot parallel arrays with a linear tag search) is replaced by the client's `TipMessage`.
* **StringBuilder.** `StringBuffer` is replaced by `StringBuilder` in all DB managers.
* Already done in 2.0: lock-free `MessageChannel` queues, `FrameHolder` record, atomic dirty counters, and O(1) enum lookups.

### 4. Loops to streams, plus a measured speed-up

Every converted path was benchmarked against the original loop code. Where a stream was measurably slower on a hot path, the faster form was kept.

**Converted to streams / functional style (51 loops):**

| Where | Now |
|---|---|
| 23 message enums + `OrderBooks` (lookup tables) | `Arrays.stream(values()).collect(toUnmodifiableMap(...))` |
| `DBQueueUpdater`: market, index and 7 copy-pasted share/bond/OTC sweeps | `Arrays.stream(...).filter(vo -> vo.isUpdated).forEach(...)`. The 7 duplicates collapsed into one generic `flushUpdated(...)` helper. |
| `DBManagerMarket`: 4 SQL builders + the best-offer fill | `IntStream.rangeClosed(...).mapToObj(...).collect(joining())`, `IntStream.range(...).forEach(...)` |
| `LoadInitialData`: SHARED_LIST_MAPPING | `Arrays.stream(split).map(...).forEach(...)` |
| `MessageDatabaseQueue.totalPending` | `all().stream().mapToInt(Buffer::size).sum()` |
| `TipFeedReader` connect / logon retries | `IntStream.range(0, retries).takeWhile(...).anyMatch(this::tryConnect)` |
| Order-book copy (3 VO classes) and empty book | `BestBidOffVO.copyOf(...)` / `Arrays.setAll(...)` |
| Order-book level delete (6 copies) | one `shiftLevelsUp(...)` helper |

**Kept as loops on purpose (38).** Each would be slower or more fragile as a stream:

* **Per-message field parsing (24 loops in the parsers).** These sit on the hot path, stop early (`!finished`), and drive a large `switch` that sets dozens of fields. A stream adds a pipeline allocation per message and would need every switch body rewritten as a lambda.
* **Byte-level decoding and scans (`FeedTextDecoder`, `Utility.splitTagValue`, feed-log replay).** Primitive `byte[]`/`char` work with early exit; there is no `ByteStream`, and boxing would cost several times more.
* **JDBC loops (BBO parameter binding, 8 intraday delete-window loops, DB reconnect).** Every call throws the checked `SQLException`, which lambdas cannot propagate, and the counters (`paramIndex++`, delete windows) are mutable.
* **Logoff drain loop.** Blocking I/O with a checked `IOException` and an early return.

Measured on 10-element order-book arrays (ns per call; lower is better):

| | loop | `Arrays.setAll` | `stream().toArray()` |
|---|---|---|---|
| copy 10 levels | 82 | **82** | 130 |
| empty book | 72 | **75** | 127 |

So `copyOf` and the empty-book factory use `Arrays.setAll`: functional style at loop speed. The level shift stayed a plain loop, which was faster than both `System.arraycopy` and a stream for 10 elements.

**The real speed-up: `volatile` removed from 215 VO data fields.** Every field of every VO was `volatile`, so a `TIP_ShareVO` copy did about 140 volatile writes, each one a full memory fence. Copies are made for every trade, statistics and order-book message, and by every sweep. Now:

* Only `TIP_BaseVO.isUpdated` (the publication flag) and `current_state` (read across parser threads) stay `volatile`.
* This is safe because the parser writes the data and then sets `isUpdated` (volatile write), and the sweeper reads `isUpdated` (volatile read) before copying, which gives happens-before for the data.
* Copies travel through the `MessageChannel` queues, which publish them safely as well.
* The live VOs were already written by several parser threads at once, and per-field `volatile` never made a multi-field update consistent.

| Measured (same JVM, same harness) | 2.0 | 2.1 | Speed-up |
|---|---|---|---|
| `TIP_ShareVO` copy | 792 ns | 239 ns | **3.3x** |
| `TIP_BondVO` copy | 823 ns | 241 ns | **3.4x** |
| DBQueueUpdater sweep (200 shares, 10 dirty: scan + copy + enqueue) | 8.9 us | 3.0 us | **3.0x** |

If you want the old memory semantics back for any VO, add `volatile` back to that field. Nothing else depends on it.

### Verification for 2.1

* **Compile:** both modules compile with `javac --release 21 -Xlint:all`.
* **tip-client tests:** all **262** pass, run here for the first time **against the real Log4j 2 jars** (its README flagged that as never done).
* **Listener tests:** all **35** pass, including a test for the stream-based sweep helper and 7 end-to-end `TipFeedReader` tests against `FakeServer`:
  * logon and streaming request text
  * delivery and the newline rule
  * feed-log contents
  * replay then resume at `Sn500`
  * reconnect resumes at `Sn12` without replaying
  * refused logon and unreachable server shut down
  * stop while idle sends `LGf` and returns in about 2 s

**Still not run:** a live TIP session, Oracle, and a Maven build against Maven Central. Do one parallel UAT session next to the current listener and compare the DB tables before switching.

---

# 2.0: Java 25 migration notes (previous step)

The listener has been moved from Java 8 (Eclipse project, bundled jars) to **Java 25 LTS** as a Maven project. The work focused on the threading and queueing layer, where most of the waste was. Business logic (parsing rules, VO updates, SQL, BOD/EOD flow) is **unchanged** unless a change is listed below.

## Build & run

```bash
mvn clean package                    # needs JDK 25; target/tip-web-listener-2.0.0.jar + target/lib/
mvn clean package -Djava.release=21  # if your build agents only have JDK 21 for now

bin/run-listener.sh /path/to/efp.ini # see the script for heap/GC flags
```

* `-DLISTENER_INI_FILE` works as before. (In 2.1 `log4j2.xml` lives in `config/` and JNI is gone, see above.)
* Dependencies: Log4j **2.24.3**, **ojdbc11** 23.5 (it replaces ojdbc8; works on Java 21 and 25), JUnit 5 for tests. `jgl3.1.0.jar` and `log4j-web` are no longer needed. Check the versions in `pom.xml` against your internal repository.
* If you build a jlinked runtime, include `jdk.charsets`, because the Arabic re-encoding uses `windows-1256`.

## Performance changes

| Area | Before | After |
|---|---|---|
| Queues (16 DB buffers + 8 parser queues + reader buffer) | `com.objectspace.jgl.Queue` (1998), a monitor lock on every call, no way to wait for data | `MessageChannel`: a lock-free `ConcurrentLinkedQueue` with an O(1) size counter. The consumer parks until a producer publishes. |
| DataReader dispatcher | Busy-spin with no sleep: **one CPU core at 100% all session** | Blocks on the buffer (0% CPU when idle) |
| 7 parser threads | Drain, then `sleep(1–2 ms)`, about 500 wake-ups/s each when idle | Block on their own queue and wake when a message arrives |
| 19 DB writer/updater threads | Drain, then `sleep(100 ms)`, so an average of 50 ms extra latency before a row went to the DB batch | Block on their buffer with a 100 ms max wait, so housekeeping and periodic commits run at the same cadence as before |
| Tag/value split (every field of every message) | `split("[a-z]", 2)`: compiles a regex `Pattern` on every call | `Utility.splitTagValue`: a plain scan with an identical result, proven against the regex on 200k random inputs |
| Enum lookups (`getMessageValue`, 23 enums) | Clone `values()` + linear scan + writes to shared statics, per call | Immutable `Map` lookup |
| `Utility.appendTxWithToday` / `formatTime` | `static synchronized`, a global lock shared by all parsers | Stateless, no lock |
| `System.gc()` | Called in **19** `commitTrans` methods, so a full stop-the-world GC from every thread every 10 s | Removed |
| Logging | `%C` in the pattern (a stack walk per log line), root=DEBUG, synchronous file I/O, about 36 INFO lines **per message** (e.g. every order-book level) | `%c{1}`, root=INFO, Async appender, per-message lines moved to guarded DEBUG, and 151 concatenating `debug()` calls guarded |
| Raw feed log | `write()+flush()` syscall per message, platform charset | 64 KiB buffer flushed every 200 ms (configurable, 0 = legacy), explicit charset used for both write and replay |
| Per-message `Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"))` | Allocation + synchronized zone lookup | Cached `ZoneId` + `LocalTime` |
| Prepared statements | Each DB manager prepared every statement **twice** (base constructor + subclass constructor) and leaked the first set of open cursors | Prepared once |

Micro-benchmark run in the build sandbox (2 vCPU, JDK 21, 5,000 msg/s in bursts). The absolute numbers depend on the machine; what matters is the ratio:

```
legacy JGL + sleep(2ms) poll        p50  1041 us  p99  2120 us   consumer CPU 580 ms
legacy JGL + sleep(100ms) (DB thr)  p50 50413 us  p99 99196 us
new MessageChannel + awaitData      p50    74 us  p99   385 us   consumer CPU 150 ms

Idle 2 s: legacy DataReader busy-spin 1965 ms CPU | legacy parser poll 29 ms | new 1 ms
Tag split: legacy regex 828 ns/field vs splitTagValue 30 ns/field (27x)
```

## Correctness fixes made along the way

* **`shutdownFlag` is now `volatile`.** Without it, threads were not guaranteed to ever see the stop signal.
* **Dirty counters (`totShareUpdated`, ...) are now `AtomicInteger`, read and reset with `getAndSet(0)`.** The old `tmp = tot; tot = 0;` could lose an increment from a parser thread. Because a parser only increments when `isUpdated` was false, an instrument hit by that race could stay un-flushed (stale on the website) until another instrument updated.
* **Shared `CharsetEncoder`/`CharsetDecoder`** (not thread-safe) were used at the same time by the three index-intra writer threads. Each DB manager now has its own pair.
* **Shared static `SimpleDateFormat`** (not thread-safe) is now thread-confined. The lenient parsing is kept, so results are identical.
* **DataReader:** a typed message without `;` used to throw, and the frame was then **retried forever**, so the dispatcher stalled and the queue grew without bound. Such a message is now logged and dropped.
* **Reader:** an empty native message used to throw `StringIndexOutOfBounds` and end the read loop. It is now logged and skipped. A reconnect no longer leaks the previous feed-log file handle.
* **Explicit `commit()` on auto-commit connections:** the connections have always run with auto-commit **on** (nothing ever sets it off). ojdbc then throws "Could not commit with auto-commit set on", which was logged with a stack trace on every flush. `commitIfNeeded()` skips the call. Transaction semantics are unchanged.
* **Threads:** workers now run as **named** platform threads (`Parser-OrderBook`, `DBWriterShareIntra`, ...) with an uncaught-exception logger. The async EOD tasks run on virtual threads.

## Behaviour changes to be aware of

1. **Log content:** the root level is now INFO and about 36 per-message INFO lines are now DEBUG. Use `-Dtip.log.level=debug` to get everything back.
2. **Feed log format:** since 2.1 the feed log stores the raw TIP bytes (`FEED_LOG_CHARSET` was removed). Files written by 1.x/2.0 replay correctly as long as their non-ASCII text is UTF-8.
3. **Feed log durability:** with the 200 ms flush, a `kill -9` can lose up to 200 ms of feed log. Recovery still works (TIP re-sends from the last sequence). Set `FEED_LOG_FLUSH_INTERVAL_MS=0` for the legacy flush-every-message behaviour.
4. **Config overrides:** any efp.ini key can now be overridden by the environment variable `TIP_<KEY>` or by `-Dtip.<KEY>`. Use this to move `DB_PASS` and `TARGIN_PASSWORD` out of efp.ini (they are currently stored in plain text).

## Found but deliberately NOT changed (please review)

These change business output, so they need your decision:

* `DataParserTrdAndTSt` (TSt for main-market shares): when `IS_NEGDEALS_EARLY_START` is false, the same `shareToQ` is pushed to `tipShareIntraQ` **twice**: once unconditionally, then again inside `if (!IS_NEGDEALS_EARLY_START)`. That probably writes duplicate intraday rows. The equivalent SME block pushes only once.
* In the same block, the `else` branch (log "received before market pre-open") runs for **every** TSt when early start is off. It is now a DEBUG line, but the condition looks unintended.
* `DBManager.isConnectionOpen()` reconnects but does not re-prepare statements, so after a DB reconnect the old statements still point at the closed connection.
* `DBQueueUpdater.quantaStart` is never reset, so after the first 5 s the "max delay" gate is always open (a sweep every 2 ms). That is harmless, but `TIP_MAX_DELAY_TIME` has no effect.
* The async EOD threads share the DB manager's JDBC connection with the owning writer thread.

## Verification status

* The code compiles cleanly with `javac --release 21 -Xlint:all` (only a pre-existing fall-through warning remains). It only uses APIs that are final in Java 21, so it is valid Java 25.
* 17 unit tests (`src/test`) pass. They show that the new splitter, time formatting, date parsing and enum lookups give **identical results** to the legacy code, and they cover the queue semantics: FIFO, multi-producer ordering, wake-up, and Buffer peek/release.
* `log4j2.xml` was loaded and checked: levels and the async appender work.
* **Not run here:** a Maven build against Maven Central or a JDK 25 runtime (neither was reachable from the build environment), and anything that needs the live TIP feed or Oracle. Before production, do a parallel run against UAT and compare the DB tables with the current listener for one full session (BOD, trading, EOD).
