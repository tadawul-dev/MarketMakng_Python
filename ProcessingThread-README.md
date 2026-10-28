# ProcessingThread explained

This file explains `ProcessingThread.java` in simple sentences.

One thread handles one market maker and one symbol. The key looks like `M0110005-STCF6V`.

The thread stores every matching order first. It scores those orders once, after the order file is finished. The score does not change when a log line is written.

## How a run moves

1. The job creates one thread from one config row.
2. Matching orders are dropped into that thread's queue.
3. The thread stores each order.
4. When the queue is empty and the file is finished, `calculate()` runs once.
5. Reports read the saved totals. They do not score the orders again.

## Config values

These come from the MM config row and the time config. A blank number is stored as 0.

| Config | Variable | Meaning |
| --- | --- | --- |
| Market Maker | `mmid` | Market maker id, such as `M0110005` |
| Market Maker Name | `companyName` | Name printed on the reports |
| UnderlyingSym | `symbol` | Order-file code, such as `STCF6V`. This is also the thread key |
| Contract | `contract` | Display name, such as `7010FUT` |
| is_rollout_config | `isRolloutConfig` | True when the cell is 1 |
| Type | `marketType` | `SSF` or `IF` |
| TickSize | `tickSize` | Price of one tick. Example: `0.1` for SSF, `0.5` for IF |
| SpreadInTicks | `spreadInTicks` | Largest allowed gap between best buy and best sell |
| Depth and lots | `QtyAtEachPriceLevel` and `minLots` | Five lot sizes, B1 to B5. A missing buy level uses the matching S level |
| Price variance | `levelStep` | How many ticks apart the five prices must be. A blank or 0 becomes 1 |
| VolumePerDay | `configuredVolumePerDay` | Traded lots required for a YES |
| PresenceTime% | `configuredPresenceTime` | Presence required for a YES. `80` means 80 percent. `0.8` means the same ratio |
| Session start and end | `mmTimeConfigDto` | Open and close. The length is `marketTimeInSeconds` |
| Overnight included | `isOvernightSessionIncluded` | True: orders before the open still build the book. False: those orders are dropped |

The thread key is `mmid + "-" + symbol`. Two rows with the same market maker and the same underlying symbol share one thread. A rollout row with a different underlying symbol is a different thread.

## Variables that change while the thread runs

| Variable | What it holds |
| --- | --- |
| `resultOrder` | Queue of orders waiting to be stored |
| `pending` | Orders kept for the one score at the end |
| `sequence` | Arrival order, used when two orders have the same second |
| `calculated` | True after `calculate()` has run. Later report calls reuse that result |
| `totalVolumeTraded` | Lots counted from fills |
| `activeSpreadVarianceTime` | Seconds the spread check passed |
| `activeQuantityVarianceTime` | Seconds the five-level book passed |
| `activeSpreadQuantityVarianceTime` | Seconds both checks passed together |
| `percentOfTotalActiveSpreadQuantityVarianceTime` | Presence seconds divided by the full session, times 100 |
| `presenceTimeFlag` | True when presence seconds reach the required seconds |
| `totalFilledVolumeFlag` | True when lots reach the required lots |
| `eligibleFlag` | True only when presence and volume are both true |
| `varianceIntervals` | Start and end of each period where both checks passed. Used by the interval report |
| `streakRows` | Passed periods for the streak file |
| `auditRows` | Failed periods for the audit file |

## Small objects used inside the score

| Object | What it holds |
| --- | --- |
| `Held` | One stored order, its arrival number, and its time in seconds |
| `Rest` | One live order on the book: side, price in ticks, remaining size |
| `Seg` | One time slice: start, end, spread pass or fail, quantity pass or fail, and the fail reason |
| `Snap` | The previous price and size of an order chain. Used only to write the audit "what changed" text |
| `StreakRow` | One streak-file row |
| `AuditRow` | One audit-file row |

The book is a map from chain id to `Rest`. The chain id of a new order is its order number. A later update, fill, or cancel uses the initiating order number so it changes that same order.

## Step 1. Create the thread

`ProcessingThread(...)` copies the config into the fields above.

`lots()` builds the five minimum sizes. It reads B1, B2, B3, B4, B5. If a B level is missing, it uses S1, S2, S3, S4, or S5.

## Step 2. Collect orders

`addOrder()` puts one order on the queue.

`run()` loops:

1. Sleep for the configured pause.
2. Take the next order.
3. `remember()` stores it on `pending`.
4. Write the order log: market maker, symbol, order number, action, time, price, remaining size, side, and rollout flag.
5. If the queue is empty and the file is marked complete, call `ensureCalculated()` and stop the thread.

`ensureCalculated()` runs `calculate()` only the first time. `getReport()` and the other report methods call it too, so a report never scores twice.

## Step 3. Rollout volume-only row

`volumeOnly()` is true only when all four are true:

- rollout flag is true
- tick size is 0
- spread in ticks is 0
- presence is 0

For that row, `calculate()` sets spread, quantity, and presence to 0. Eligibility is false. Traded lots are still counted by `tradedLots()`. The five-level book is not checked.

A normal row continues. A rollout row that still has a tick, a spread, or a presence value also continues. It is scored like a normal contract.

## Step 4. Count traded lots

`tradedLots()` looks only at action `0779`.

- It adds `filledVolume`.
- A ticket that appears two or more times in this thread is a self-trade. Those lots are left out.
- A fill with no ticket number is counted.

## Step 5. Split the session into slices

`segments()` builds the book and returns one `Seg` for each stretch of time.

1. Keep orders that have an action time.
2. If overnight is off, drop orders before the open.
3. Sort by time, then by arrival order.
4. Apply every order that shares one second.
5. After the last order in that second, ask `spreadMet()` and `depthMet()`.
6. That result stays in force until the next second that has an order.
7. Orders at or after the close do not add time past the close.
8. If the first in-session order is after the open, the book built before the open covers the time from the open until that order.

`apply()` changes one order on the book.

`kind()` turns the action code into a word:

| Code | Word | What `apply()` does |
| --- | --- | --- |
| 0772, 0775, 0788, 0790 | entry | Keep it when remaining size and price are above 0 |
| 0779 | fill | Keep it when size remains. Remove it when size is 0 |
| 0776, 0778, 0793, 0795 | exit | Remove it |
| 0789, 0794 | pause | Remove it until a later resume |
| Anything else | ignore | Leave the book as it is |

`chainOf()` finds the original order. `0772` uses its own order number. Later actions follow the initiating order number.

`side()` reads the transaction type. `2` is buy. `1` is sell.

`toTicks()` converts a price with `round(price / tick size)`.

## Step 6. Spread check

`spreadMet()` is true only when both sides have a price and the gap is small enough.

- Best buy = highest buy price on the book (`bestPrice`)
- Best sell = lowest sell price on the book
- Pass when `best sell ticks - best buy ticks <= spreadInTicks`

Example: the limit is 2. Best buy is 42.10 and best sell is 42.30. The gap is 2 ticks, so the spread passes.

`spreadReason()` writes the audit text when it fails: no orders, missing buy, missing sell, or the gap is too wide.

## Step 7. Quantity check

`depthMet()` is true only when both sides have a full five-level ladder and the two ladder starts are inside the same spread limit.

`ladderStart()` tries each price on that side:

- Buy levels go down from the price by `levelStep` ticks.
- Sell levels go up from the price by `levelStep` ticks.
- Level 1 needs at least B1 lots, level 2 needs B2, and so on through B5.

If any level is short or the next price is not exactly one step away, that start fails. `sideReason()` writes that detail for the audit.

## Step 8. Add the seconds

`calculate()` walks the slices from `segments()`:

- Add the slice length to spread seconds when `seg.spread` is true.
- Add it to quantity seconds when `seg.depth` is true.
- Add it to presence seconds only when both are true.
- Save each both-true stretch in `varianceIntervals` for the interval report.

Presence is the overlap. It is not spread seconds plus quantity seconds.

## Step 9. YES or NO

`requiredSeconds()` turns the presence config into seconds.

- If the config is greater than 1, divide by 100. `80` becomes 0.80.
- If it is 1 or less, use it as a ratio. `0.8` stays 0.8.
- Required seconds = that ratio times the full session length, rounded.

Then:

- `presenceTimeFlag` is true when presence seconds are at least the required seconds.
- `totalFilledVolumeFlag` is true when lots are at least `configuredVolumePerDay`.
- `eligibleFlag` is true only when both are true.

The result log prints market maker, symbol, contract, rollout, the three second totals, the percent, the lots, and eligible.

## Step 10. Streak file

`buildStreaksAndAudit()` then `addStreaks()` walks the same slices. It does not change the totals.

It writes three kinds of passed periods:

- `Spread` when the spread check is true
- `Depth` when the quantity check is true
- `Fully eligible` when both are true

`addStreak()` stores start, end, and duration. The end shown in the file is the last second of the period. A slice from 10:00:33 up to but not including 10:01:50 is shown as end `10:01:49`, duration 77.

`getStreaks()` returns these rows.

## Step 11. Audit file

`addAudit()` walks the same slices and keeps the failed periods.

- Obligation `Spread` is a failed spread check.
- Obligation `Order book` is a failed quantity check.

Seconds with the same reason are joined into one row. `addAuditRow()` adds a full stop, and adds "What changed" when an order in the opening second caused the failure.

`changeText()` and `oneChange()` build that sentence from the action:

- 0772 new order
- 0775 price move or size change
- 0779 partial or full fill
- 0788 price adjusted
- 0776 cancelled, 0778 expired, 0790 resumed
- 0789 or 0794 suspended
- 0793 cancel on disconnect
- 0795 kill-switch cancel

`getAudits()` returns these rows.

`unmet()` keeps a reason only when the check failed. A passed second has no audit reason.

## Step 12. Report methods

| Method | What it returns | Which file uses it |
| --- | --- | --- |
| `getReport()` | Totals and YES or NO for every config row, including rollout | First daily performance report |
| `getIntervalReport()` | Start and end of each both-true period | Interval performance report |
| `getReportForSecondReport()` | Same totals, plus rollout flag, tick, spread, and presence config | Derivatives Daily Summary Report |
| `getStreaks()` | Passed periods | Streak file |
| `getAudits()` | Failed periods and reasons | Audit file |

The summary merge of two rollout rows is not done here. `MMUniqueThread` does that later. This class only sends the fields that merge reads.

## Log methods

These methods only write text. They do not change a total.

| Method | What it writes |
| --- | --- |
| `logPhases` and `logPhase` | Spread, quantity, or both started and ended |
| `ordersAt` and `orderText` | Order number and action at that second |
| `noteOpen` | A check that is already on at the open |
| `noteChange` | A check that starts or stops because of the orders in that second, with the seconds lasted |
| `logSessionClose` | A check that is still on at the close |
| `logOrdersThisSecond` | Best bid, best ask, gap, and whether quantity is on |
| `px` and `tickWord` | A tick count printed as a price, or as the word "tick" |

## What this class does not do

- It does not merge a rollout row into the normal row.
- It does not remove seconds for a contract suspension. Halt times are not loaded here.
- It does not send email or write the Excel file. The job does that after every thread finishes.
