package com.muqassa.marketmaking.process;

import com.muqassa.marketmaking.dto.*;
import com.muqassa.marketmaking.enums.MarketType;
import com.muqassa.marketmaking.shared.SharedState;
import com.muqassa.marketmaking.thread.ThreadCompleteListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * One market maker and one contract. Orders are collected on the existing queue,
 * then scored once: resting size is volume remaining, a second counts after its last
 * order, and presence is the overlap of spread and the five-level book.
 */
public class ProcessingThread implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ProcessingThread.class);

    private static final int ACTION_INSERT = 772;
    private static final int ACTION_UPDATE = 775;
    private static final int ACTION_CANCELLED = 776;
    private static final int ACTION_EXPIRED = 778;
    private static final int ACTION_FILLED = 779;
    private static final int ACTION_PRICE_ADJUSTED = 788;
    private static final int ACTION_SUSPENDED = 789;
    private static final int ACTION_RESUMED = 790;
    private static final int ACTION_CANCEL_ON_DISCONNECT = 793;
    private static final int ACTION_SUSPEND_ON_DISCONNECT = 794;
    private static final int ACTION_FORCED_CANCEL = 795;

    private final boolean isOvernightSessionIncluded;
    private final SharedState sharedState;
    private final String key;
    private final ThreadCompleteListener listener;
    private final String mmid;
    private final String companyName;
    private final String symbol;
    private final String contract;
    private final Boolean isRolloutConfig;
    private final MarketType marketType;
    private final int spreadInTicks;
    private final double tickSize;
    private final String QtyAtEachPriceLevel;
    private final int[] minLots;
    private final int levelStep;
    private final long configuredVolumePerDay;
    private final double configuredPresenceTime;
    private final List<MMTimeConfigDto> mmTimeConfigDto;
    private final long marketTimeInSeconds;

    private final List<ReportforStarttimeEndTimeDto.Interval> varianceIntervals = new ArrayList<>();
    private final List<Held> pending = new ArrayList<>();
    private final BlockingDeque<OrderFileDto> resultOrder = new LinkedBlockingDeque<>();
    private final Object calcLock = new Object();

    private int sequence;
    private boolean calculated;

    private long activeSpreadVarianceTime;
    private long activeQuantityVarianceTime;
    private long activeSpreadQuantityVarianceTime;
    private double percentOfTotalActiveSpreadQuantityVarianceTime;
    private long totalVolumeTraded;
    private boolean totalFilledVolumeFlag;
    private boolean presenceTimeFlag;
    private boolean eligibleFlag;
    private final List<StreakRow> streakRows = new ArrayList<>();
    private final List<AuditRow> auditRows = new ArrayList<>();

    public ProcessingThread(String mmid, String companyName, String symbol, String contract, Boolean isRolloutConfig,
                            MarketType marketType, Integer spreadInTicks, Double tickSize, String QtyAtEachPriceLevel,
                            Map<String, Integer> buyLevels, Map<String, Integer> sellLevels, Integer qtyTickSize,
                            Long configuredVolumePerDay, Double configuredPresenceTime, List<MMTimeConfigDto> mmTimeConfigDto,
                            SharedState sharedState, Boolean isOvernightSessionIncluded, ThreadCompleteListener listener) {
        this.mmid = mmid;
        this.companyName = companyName;
        this.symbol = symbol;
        this.contract = contract;
        this.isRolloutConfig = isRolloutConfig;
        this.marketType = marketType;
        this.spreadInTicks = spreadInTicks == null ? 0 : spreadInTicks;
        this.tickSize = tickSize == null ? 0.0 : tickSize;
        this.QtyAtEachPriceLevel = QtyAtEachPriceLevel;
        this.minLots = lots(buyLevels, sellLevels);
        this.levelStep = qtyTickSize == null || qtyTickSize < 1 ? 1 : qtyTickSize;
        this.configuredVolumePerDay = configuredVolumePerDay == null ? 0L : configuredVolumePerDay;
        this.configuredPresenceTime = configuredPresenceTime == null ? 0.0 : configuredPresenceTime;
        this.mmTimeConfigDto = mmTimeConfigDto;
        this.marketTimeInSeconds = mmTimeConfigDto.getFirst().getTotalMarketTimeInSeconds();
        this.sharedState = sharedState;
        this.isOvernightSessionIncluded = Boolean.TRUE.equals(isOvernightSessionIncluded);
        this.key = mmid + "-" + symbol;
        this.listener = listener;
    }

    public void addOrder(OrderFileDto orderFileDto) {
        resultOrder.offer(orderFileDto);
    }

    @Override
    public void run() {
        log.info("[THREAD STARTED] Worker initialized for key: {}", key);
        while (true) {
            try {
                Thread.sleep(sharedState.getProcessThreadSleepTimeout());
                OrderFileDto orderFileDto = resultOrder.take();
                sharedState.incrementQueueTakeCount();
                remember(orderFileDto);
                sharedState.incrementTotalOrderProcessed();
                log.info("**************************************************************************");
                log.info("[ORDER PROCESS START] {}-{} | Order No: {} action {} time {} price {} remaining {} side {} rollout {} ThreadName :{} queue {}",
                        mmid, symbol, orderFileDto.getOrderNumber(), orderFileDto.getOrderAction(), orderFileDto.getActionTime(),
                        orderFileDto.getDisclosedPrice(), orderFileDto.getVolumeRemaining(), side(orderFileDto),
                        Boolean.TRUE.equals(isRolloutConfig), Thread.currentThread().getName(), resultOrder.size());
                log.info("Order Starts processing {}-{} at {} order {}", mmid, symbol, orderFileDto.getActionTime(), orderFileDto.getOrderNumber());
                log.info("Total FileOrder Count :{} : Total Order Processed count : {} Total UnMatched Orders : {} Total OutofMarketTimeOrders :{}",
                        sharedState.getOrderFileLineCount(), sharedState.getTotalOrderProcessedCount(),
                        sharedState.getTotalUnMatchedCount(), sharedState.getOutOfMarketTimeOrderOrderCount());
                if (resultOrder.isEmpty() && sharedState.getFileCompleted() == 1) {
                    ensureCalculated();
                    if (listener != null) {
                        listener.onThreadComplete(key);
                    }
                    break;
                }
            } catch (InterruptedException e) {
                log.error("Processing interrupted for {}-{}", mmid, symbol, e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void remember(@NotNull OrderFileDto order) {
        synchronized (calcLock) {
            pending.add(new Held(order, sequence++));
        }
    }

    private void ensureCalculated() {
        synchronized (calcLock) {
            if (calculated) {
                return;
            }
            calculate();
            calculated = true;
        }
    }

    private void calculate() {
        varianceIntervals.clear();
        streakRows.clear();
        auditRows.clear();
        totalVolumeTraded = tradedLots();
        int windowStart = mmTimeConfigDto.getFirst().getStartTime().toSecondOfDay();
        int windowEnd = mmTimeConfigDto.getFirst().getEndTime().toSecondOfDay();

        if (volumeOnly()) {
            activeSpreadVarianceTime = 0;
            activeQuantityVarianceTime = 0;
            activeSpreadQuantityVarianceTime = 0;
            percentOfTotalActiveSpreadQuantityVarianceTime = 0;
            presenceTimeFlag = false;
            totalFilledVolumeFlag = totalVolumeTraded >= configuredVolumePerDay;
            eligibleFlag = false;
            log.info("ROLLOUT volume only. No spread or quantity. mmid={} symbol={} contract={} rollout=true lots={}",
                    mmid, symbol, contract, totalVolumeTraded);
            for (Held held : pending) {
                OrderFileDto order = held.order;
                log.info("this order comes under the rollover configuration {}-{} order {} action {} time {} rollout true",
                        mmid, symbol, order.getOrderNumber(), order.getOrderAction(), order.getActionTime());
            }
            return;
        }

        long spread = 0;
        long depth = 0;
        long combined = 0;
        Integer streak = null;
        List<Seg> scored = segments(windowStart, windowEnd);
        logPhases(scored);
        buildStreaksAndAudit(scored, windowEnd);
        for (Seg seg : scored) {
            int seconds = seg.end - seg.start;
            if (seg.spread) {
                spread += seconds;
            }
            if (seg.depth) {
                depth += seconds;
            }
            boolean both = seg.spread && seg.depth;
            if (both) {
                combined += seconds;
            }
            if (both && streak == null) {
                streak = seg.start;
            } else if (!both && streak != null) {
                varianceIntervals.add(new ReportforStarttimeEndTimeDto.Interval(fromSec(streak), fromSec(seg.start)));
                streak = null;
            }
        }
        if (streak != null) {
            varianceIntervals.add(new ReportforStarttimeEndTimeDto.Interval(fromSec(streak), fromSec(windowEnd)));
        }

        activeSpreadVarianceTime = spread;
        activeQuantityVarianceTime = depth;
        activeSpreadQuantityVarianceTime = combined;
        percentOfTotalActiveSpreadQuantityVarianceTime = marketTimeInSeconds == 0 ? 0
                : combined * 100.0 / marketTimeInSeconds;
        presenceTimeFlag = combined >= requiredSeconds();
        totalFilledVolumeFlag = totalVolumeTraded >= configuredVolumePerDay;
        eligibleFlag = presenceTimeFlag && totalFilledVolumeFlag;
        log.info("RESULT mmid={} symbol={} contract={} rollout={} spread {}s quantity {}s both {}s ({}%) lots {} eligible {}",
                mmid, symbol, contract, Boolean.TRUE.equals(isRolloutConfig), spread, depth, combined,
                String.format(Locale.US, "%.2f", percentOfTotalActiveSpreadQuantityVarianceTime),
                totalVolumeTraded, eligibleFlag);
        log.info("TotalFilledVolumeTraded:{} (Required: {}) → {}", totalVolumeTraded, configuredVolumePerDay, totalFilledVolumeFlag);
        log.info("ActiveSpreadQuantityVarianceTime: {} tot {}s (Required: {}) → {}",
                activeSpreadQuantityVarianceTime, percentOfTotalActiveSpreadQuantityVarianceTime,
                configuredPresenceTime, presenceTimeFlag);
        log.info("MMID: {} Symbol: {} EligibleFlag: {} rollout {}", mmid, symbol, eligibleFlag, Boolean.TRUE.equals(isRolloutConfig));
    }

    /** Logs only. Does not change the scored segments. */
    private void logPhases(List<Seg> segs) {
        boolean spreadOn = false;
        boolean quantityOn = false;
        boolean bothOn = false;
        for (Seg seg : segs) {
            if (seg.end <= seg.start) {
                continue;
            }
            if (seg.spread && !spreadOn) {
                logPhase("SPREAD", "STARTED", seg.start);
                spreadOn = true;
            } else if (!seg.spread && spreadOn) {
                logPhase("SPREAD", "ENDED", seg.start);
                spreadOn = false;
            }
            if (seg.depth && !quantityOn) {
                logPhase("QUANTITY", "STARTED", seg.start);
                quantityOn = true;
            } else if (!seg.depth && quantityOn) {
                logPhase("QUANTITY", "FAILED", seg.start);
                quantityOn = false;
            }
            boolean both = seg.spread && seg.depth;
            if (both && !bothOn) {
                logPhase("SPREAD+QUANTITY", "STARTED", seg.start);
                bothOn = true;
            } else if (!both && bothOn) {
                logPhase("SPREAD+QUANTITY", "ENDED", seg.start);
                bothOn = false;
            }
        }
        if (segs.isEmpty()) {
            return;
        }
        int end = segs.get(segs.size() - 1).end;
        if (bothOn) {
            logPhase("SPREAD+QUANTITY", "ENDED", end);
        }
        if (spreadOn) {
            logPhase("SPREAD", "ENDED", end);
        }
        if (quantityOn) {
            logPhase("QUANTITY", "ENDED", end);
        }
    }

    private void logPhase(String check, String state, int timeSec) {
        log.info("{} {} mmid={} symbol={} contract={} rollout={} time={} order={}",
                check, state, mmid, symbol, contract, Boolean.TRUE.equals(isRolloutConfig),
                fromSec(timeSec), ordersAt(timeSec));
    }

    private String ordersAt(int timeSec) {
        StringBuilder text = new StringBuilder();
        for (Held held : pending) {
            if (held.order.getActionTime() == null || held.order.getActionTime().toSecondOfDay() != timeSec) {
                continue;
            }
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(held.order.getOrderNumber()).append('/').append(held.order.getOrderAction());
        }
        return text.length() == 0 ? "-" : text.toString();
    }

    private static String orderText(List<OrderFileDto> orders) {
        StringBuilder text = new StringBuilder();
        for (OrderFileDto order : orders) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(order.getOrderNumber()).append('/').append(order.getOrderAction());
        }
        return text.length() == 0 ? "-" : text.toString();
    }

    private void buildStreaksAndAudit(List<Seg> segs, int windowEnd) {
        addStreaks(segs, windowEnd, "Spread", true, false);
        addStreaks(segs, windowEnd, "Depth", false, true);
        addStreaks(segs, windowEnd, "Fully eligible", true, true);
        addAudit(segs, "Spread", true);
        addAudit(segs, "Order book", false);
    }

    private void addStreaks(List<Seg> segs, int windowEnd, String metric, boolean needSpread, boolean needDepth) {
        Integer open = null;
        for (Seg seg : segs) {
            boolean ok = (!needSpread || seg.spread) && (!needDepth || seg.depth);
            if (ok && open == null) {
                open = seg.start;
            } else if (!ok && open != null) {
                addStreak(metric, open, seg.start);
                open = null;
            }
        }
        if (open != null) {
            addStreak(metric, open, windowEnd);
        }
    }

    private void addStreak(String metric, int start, int end) {
        if (end <= start) {
            return;
        }
        streakRows.add(new StreakRow(mmid, symbol, contract, metric, fromSec(start), fromSec(end - 1), end - start));
    }

    private void addAudit(List<Seg> segs, String obligation, boolean spreadMetric) {
        Integer from = null;
        int to = 0;
        String reason = null;
        String changed = "";
        for (Seg seg : segs) {
            if (seg.end <= seg.start) {
                continue;
            }
            String now = spreadMetric ? seg.spreadReason : seg.depthReason;
            if (now == null) {
                if (from != null) {
                    addAuditRow(obligation, from, to, reason, changed);
                    from = null;
                }
                continue;
            }
            if (from != null && !now.equals(reason)) {
                addAuditRow(obligation, from, to, reason, changed);
                from = null;
            }
            if (from == null) {
                from = seg.start;
                reason = now;
                changed = seg.changed;
            }
            to = seg.end - 1;
        }
        if (from != null) {
            addAuditRow(obligation, from, to, reason, changed);
        }
    }

    private void addAuditRow(String obligation, int from, int to, String reason, String changed) {
        if (to < from) {
            return;
        }
        String text = reason + ".";
        if (changed != null && !changed.isBlank()) {
            text += " What changed: " + changed + ".";
        }
        auditRows.add(new AuditRow(mmid, symbol, obligation, fromSec(from), fromSec(to), to - from + 1, text));
    }

    private static String unmet(boolean met, String reason) {
        if (met) {
            return null;
        }
        return reason == null || reason.isBlank() ? "Not met" : reason;
    }

    private String[] failReasons(Map<String, Rest> book) {
        return new String[]{spreadReason(book), bookReason(book)};
    }

    private String spreadReason(Map<String, Rest> book) {
        Integer bid = bestPrice(book, "BUY");
        Integer ask = bestPrice(book, "SELL");
        if (bid == null && ask == null) {
            return "No buy and no sell orders";
        }
        if (bid == null) {
            return "No buy orders";
        }
        if (ask == null) {
            return "No sell orders";
        }
        int gap = ask - bid;
        if (gap > spreadInTicks) {
            return "Spread of " + gap + " ticks (" + px(ask) + " - " + px(bid) + "); maximum " + spreadInTicks + " ticks";
        }
        return null;
    }

    private String bookReason(Map<String, Rest> book) {
        Integer bid = ladderStart(book, "BUY");
        Integer ask = ladderStart(book, "SELL");
        if (bid != null && ask != null) {
            int gap = ask - bid;
            if (gap <= spreadInTicks) {
                return null;
            }
            return "Buy order book starts at " + px(bid) + " and sell order book at " + px(ask)
                    + "; " + gap + " ticks apart; maximum " + spreadInTicks + " ticks";
        }
        if (prices(book, "BUY").isEmpty() && prices(book, "SELL").isEmpty()) {
            return "No buy and no sell orders";
        }
        List<String> parts = new ArrayList<>();
        if (bid == null) {
            parts.add(sideReason(book, "BUY"));
        }
        if (ask == null) {
            parts.add(sideReason(book, "SELL"));
        }
        return String.join("; ", parts);
    }

    private String sideReason(Map<String, Rest> book, String side) {
        String name = "BUY".equals(side) ? "Buy" : "Sell";
        Map<Integer, Integer> levels = prices(book, side);
        if (levels.isEmpty()) {
            return "No " + name.toLowerCase(Locale.ROOT) + " orders";
        }
        int step = ("BUY".equals(side) ? -1 : 1) * levelStep;
        int first = "BUY".equals(side) ? Collections.max(levels.keySet()) : Collections.min(levels.keySet());
        List<String> parts = new ArrayList<>();
        for (int level = 0; level < minLots.length; level++) {
            int price = first + level * step;
            int volume = levels.getOrDefault(price, 0);
            int required = minLots[level];
            if (volume >= required) {
                continue;
            }
            if (volume > 0) {
                String part = name + " level " + (level + 1) + " (" + px(price) + ") has " + volume + " lots; " + required + " required";
                if (level == 0) {
                    part += "; no complete order book behind it";
                }
                parts.add(part);
                continue;
            }
            List<Integer> beyond = new ArrayList<>();
            for (int quote : levels.keySet()) {
                if ("BUY".equals(side) ? quote < price : quote > price) {
                    beyond.add(quote);
                }
            }
            if (beyond.isEmpty()) {
                parts.add(name + " side has " + level + " price level" + (level == 1 ? "" : "s") + "; 5 required");
                break;
            }
            int next = "BUY".equals(side) ? Collections.max(beyond) : Collections.min(beyond);
            int previous = price - step;
            parts.add(name + " level " + (level + 1) + " should be at " + px(price) + " (" + tickWord(levelStep)
                    + " from level " + level + ") but the next " + name.toLowerCase(Locale.ROOT) + " order is at "
                    + px(next) + " (" + tickWord(Math.abs(next - previous)) + " away)");
            break;
        }
        return String.join("; ", parts);
    }

    private String changeText(List<OrderFileDto> orders, Map<String, Snap> snaps, Map<String, String> chains) {
        List<String> parts = new ArrayList<>();
        for (OrderFileDto order : orders) {
            int action = order.getOrderAction() == null ? 0 : order.getOrderAction();
            String chain = chainOf(order, chains, action);
            Snap prev = chain == null ? null : snaps.get(chain);
            String text = oneChange(order, prev);
            if (text != null) {
                parts.add(text);
            }
            if (chain != null) {
                int volume = order.getVolumeRemaining() == null ? 0 : order.getVolumeRemaining().intValue();
                double price = order.getDisclosedPrice() == null ? 0.0 : order.getDisclosedPrice();
                String kind = kind(action);
                boolean live = ("entry".equals(kind) || "fill".equals(kind)) && volume > 0 && price > 0;
                int ticks = price > 0 ? toTicks(price) : (prev == null ? 0 : prev.ticks);
                snaps.put(chain, new Snap(side(order), ticks, volume, live));
            }
        }
        return String.join("; ", parts);
    }

    private String oneChange(OrderFileDto order, Snap prev) {
        int action = order.getOrderAction() == null ? 0 : order.getOrderAction();
        String sideName = "BUY".equals(side(order)) ? "buy" : (side(order) == null ? "order" : "sell");
        int ticks = order.getDisclosedPrice() != null && order.getDisclosedPrice() > 0
                ? toTicks(order.getDisclosedPrice()) : (prev == null ? 0 : prev.ticks);
        String at = ticks <= 0 ? "" : " at " + px(ticks);
        int volume = order.getVolumeRemaining() == null ? 0 : order.getVolumeRemaining().intValue();
        return switch (action) {
            case ACTION_INSERT -> volume > 0 && ticks > 0
                    ? "new " + sideName + " order" + at + ", " + volume + " lots (0772)" : null;
            case ACTION_UPDATE -> updateText(sideName, prev, ticks, volume);
            case ACTION_FILLED -> volume > 0
                    ? sideName + " order" + at + " partially filled (0779); " + volume + " lots left"
                    : sideName + " order" + at + " fully filled (0779)";
            case ACTION_PRICE_ADJUSTED -> sideName + " order price adjusted to " + px(ticks) + " (0788)";
            case ACTION_CANCELLED -> sideName + " order" + at + " cancelled by the market maker (0776)";
            case ACTION_EXPIRED -> sideName + " order" + at + " expired (0778)";
            case ACTION_RESUMED -> sideName + " order" + at + " resumed (0790)";
            case ACTION_SUSPENDED -> sideName + " order" + at + " suspended (0789)";
            case ACTION_SUSPEND_ON_DISCONNECT -> sideName + " order" + at + " suspended (0794)";
            case ACTION_CANCEL_ON_DISCONNECT -> sideName + " order" + at + " cancelled on disconnect (0793)";
            case ACTION_FORCED_CANCEL -> sideName + " order" + at + " cancelled through the Kill Switch (0795)";
            default -> null;
        };
    }

    private String updateText(String sideName, Snap prev, int ticks, int volume) {
        if (prev != null && prev.ticks > 0 && prev.ticks != ticks) {
            return sideName + " order moved from " + px(prev.ticks) + " to " + px(ticks) + " (0775)";
        }
        if (prev != null && prev.volume != volume) {
            return sideName + " order" + (ticks > 0 ? " at " + px(ticks) : "") + " changed from " + prev.volume + " to " + volume + " lots (0775)";
        }
        return sideName + " order" + (ticks > 0 ? " at " + px(ticks) : "") + " changed (0775)";
    }

    private String px(int ticks) {
        return String.format(Locale.US, "%.2f", ticks * tickSize);
    }

    private static String tickWord(int ticks) {
        return ticks == 1 ? "1 tick" : ticks + " ticks";
    }

    /** Existing rollover row: no spread, no book, lots only. */
    private boolean volumeOnly() {
        return Boolean.TRUE.equals(isRolloutConfig) && tickSize == 0.0 && spreadInTicks == 0 && configuredPresenceTime == 0.0;
    }

    private int requiredSeconds() {
        double ratio = configuredPresenceTime > 1 ? configuredPresenceTime / 100.0 : configuredPresenceTime;
        return (int) Math.round(ratio * marketTimeInSeconds);
    }

    private List<Seg> segments(int windowStart, int windowEnd) {
        Map<String, Rest> book = new HashMap<>();
        Map<String, String> chains = new HashMap<>();
        boolean spread = spreadMet(book);
        boolean depth = depthMet(book);
        List<Held> events = new ArrayList<>();
        for (Held held : pending) {
            if (held.order.getActionTime() == null) {
                continue;
            }
            int time = held.order.getActionTime().toSecondOfDay();
            if (!isOvernightSessionIncluded && time < windowStart) {
                continue;
            }
            events.add(new Held(held.order, held.seq, time));
        }
        events.sort(Comparator.comparingInt((Held event) -> event.timeSec).thenComparingInt(event -> event.seq));

        List<Seg> points = new ArrayList<>();
        Map<String, Snap> snaps = new HashMap<>();
        Integer spreadLogStart = null;
        long spreadLogTotal = 0;
        Integer quantityLogStart = null;
        long quantityLogTotal = 0;
        Integer bothLogStart = null;
        long bothLogTotal = 0;
        int i = 0;
        while (i < events.size()) {
            int time = events.get(i).timeSec;
            String[] beforeReason = failReasons(book);
            List<OrderFileDto> secondOrders = new ArrayList<>();
            while (i < events.size() && events.get(i).timeSec == time) {
                secondOrders.add(events.get(i).order);
                apply(book, chains, events.get(i).order);
                i++;
            }
            String changed = changeText(secondOrders, snaps, chains);
            boolean endSpread = spreadMet(book);
            boolean endDepth = depthMet(book);
            String[] afterReason = failReasons(book);
            if (time >= windowStart && time < windowEnd) {
                if (points.isEmpty() && time > windowStart) {
                    spreadLogStart = noteOpen(spread, spreadLogStart, windowStart, "Spread");
                    quantityLogStart = noteOpen(depth, quantityLogStart, windowStart, "Quantity");
                    bothLogStart = noteOpen(spread && depth, bothLogStart, windowStart, "Spread+Quantity");
                }
                spreadLogTotal = noteChange(spread, endSpread, spreadLogStart, spreadLogTotal, time, secondOrders, "Spread");
                if (spread != endSpread) {
                    spreadLogStart = endSpread ? time : null;
                }
                quantityLogTotal = noteChange(depth, endDepth, quantityLogStart, quantityLogTotal, time, secondOrders, "Quantity");
                if (depth != endDepth) {
                    quantityLogStart = endDepth ? time : null;
                }
                bothLogTotal = noteChange(spread && depth, endSpread && endDepth, bothLogStart, bothLogTotal, time, secondOrders, "Spread+Quantity");
                if ((spread && depth) != (endSpread && endDepth)) {
                    bothLogStart = endSpread && endDepth ? time : null;
                }
                logOrdersThisSecond(secondOrders, book, endSpread, endDepth, afterReason, time);
            }
            if (time >= windowEnd) {
                break;
            }
            if (time >= windowStart) {
                if (points.isEmpty() && time > windowStart) {
                    points.add(new Seg(windowStart, spread, depth,
                            unmet(spread, beforeReason[0]), unmet(depth, beforeReason[1]), ""));
                }
                points.add(new Seg(time, endSpread, endDepth,
                        unmet(endSpread, afterReason[0]), unmet(endDepth, afterReason[1]), changed));
            }
            spread = endSpread;
            depth = endDepth;
        }
        if (spreadLogStart == null && spread) {
            spreadLogStart = noteOpen(true, null, windowStart, "Spread");
        }
        if (quantityLogStart == null && depth) {
            quantityLogStart = noteOpen(true, null, windowStart, "Quantity");
        }
        if (bothLogStart == null && spread && depth) {
            bothLogStart = noteOpen(true, null, windowStart, "Spread+Quantity");
        }
        logSessionClose(spreadLogStart, spreadLogTotal, windowEnd, "Spread");
        logSessionClose(quantityLogStart, quantityLogTotal, windowEnd, "Quantity");
        logSessionClose(bothLogStart, bothLogTotal, windowEnd, "Spread+Quantity");
        if (points.isEmpty()) {
            String[] reason = failReasons(book);
            points.add(new Seg(windowStart, spread, depth,
                    unmet(spread, reason[0]), unmet(depth, reason[1]), ""));
        }
        List<Seg> out = new ArrayList<>();
        for (int k = 0; k < points.size(); k++) {
            Seg point = points.get(k);
            int end = k + 1 < points.size() ? points.get(k + 1).start : windowEnd;
            out.add(new Seg(point.start, end, point.spread, point.depth, point.spreadReason, point.depthReason, point.changed));
        }
        return out;
    }

    /** Log only. The spread and quantity flags passed in are already decided. */
    private Integer noteOpen(boolean on, Integer start, int time, String name) {
        if (!on || start != null) {
            return start;
        }
        log.info("{} variance passes, time started for {}-{} at {} -Order Number session open rollout {}",
                name, mmid, symbol, fromSec(time), Boolean.TRUE.equals(isRolloutConfig));
        return time;
    }

    private long noteChange(boolean before, boolean after, Integer start, long total, int time, List<OrderFileDto> orders, String name) {
        if (before == after) {
            return total;
        }
        String ordersText = orderText(orders);
        if (after) {
            log.info("{} variance passes, time started for {}-{} at {} -Order Number {} rollout {}",
                    name, mmid, symbol, fromSec(time), ordersText, Boolean.TRUE.equals(isRolloutConfig));
            return total;
        }
        int from = start == null ? time : start;
        long lasted = Math.max(0, time - from);
        long next = total + lasted;
        log.info("{} variance stopped for ordernumber {} {}-{} at {}, lasted {} seconds, total active time: {} seconds. And StartTime {} , variance on false rollout {}",
                name, ordersText, mmid, symbol, fromSec(time), lasted, next, fromSec(from), Boolean.TRUE.equals(isRolloutConfig));
        return next;
    }

    private void logSessionClose(Integer start, long total, int windowEnd, String name) {
        if (start == null) {
            return;
        }
        long lasted = Math.max(0, windowEnd - start);
        log.info("{} variance passes for all orders got the Market end time as stop time {}-{} , lasted {} seconds, total active time: {} seconds. And StartTime {} , variance on true rollout {}",
                name, mmid, symbol, lasted, total + lasted, fromSec(start), Boolean.TRUE.equals(isRolloutConfig));
    }

    private void logOrdersThisSecond(List<OrderFileDto> orders, Map<String, Rest> book, boolean spreadOn, boolean quantityOn,
                                     String[] reason, int time) {
        Integer bid = bestPrice(book, "BUY");
        Integer ask = bestPrice(book, "SELL");
        int gap = bid != null && ask != null ? ask - bid : -1;
        for (OrderFileDto order : orders) {
            log.info("Spread Checking {}-{} at {} order {} action {} spreadMet {} bestBid {} bestAsk {} gap {} maxSpread {} rollout {}",
                    mmid, symbol, fromSec(time), order.getOrderNumber(), order.getOrderAction(), spreadOn,
                    bid == null ? "none" : px(bid), ask == null ? "none" : px(ask), gap, spreadInTicks,
                    Boolean.TRUE.equals(isRolloutConfig));
            if (quantityOn) {
                log.info("Quantity variance ongoing for {}-{} at {} -{} rollout {}",
                        mmid, symbol, fromSec(time), order.getOrderNumber(), Boolean.TRUE.equals(isRolloutConfig));
            } else {
                log.info("Quantity variance hasn't started for {}-{} at {} order {} reason {} rollout {}",
                        mmid, symbol, fromSec(time), order.getOrderNumber(), reason[1], Boolean.TRUE.equals(isRolloutConfig));
            }
            if (spreadOn && quantityOn) {
                log.info("Spread+Quantity variance ongoing for the Marketer {} {} order {} at {} rollout {}",
                        mmid, symbol, order.getOrderNumber(), fromSec(time), Boolean.TRUE.equals(isRolloutConfig));
            } else if (!spreadOn) {
                log.info("Spread variance hasn't started for ordernumber {} {}-{} at {} reason {} rollout {}",
                        order.getOrderNumber(), mmid, symbol, fromSec(time), reason[0], Boolean.TRUE.equals(isRolloutConfig));
            }
        }
    }

    private void apply(Map<String, Rest> book, Map<String, String> chains, OrderFileDto order) {
        int action = order.getOrderAction() == null ? 0 : order.getOrderAction();
        String kind = kind(action);
        if ("ignore".equals(kind)) {
            return;
        }
        String chain = chainOf(order, chains, action);
        if (chain == null || chain.isBlank()) {
            return;
        }
        int volume = order.getVolumeRemaining() == null ? 0 : order.getVolumeRemaining().intValue();
        double price = order.getDisclosedPrice() == null ? 0.0 : order.getDisclosedPrice();
        boolean live = ("entry".equals(kind) || "fill".equals(kind)) && volume > 0 && price > 0;
        if (!live) {
            book.remove(chain);
        } else {
            String side = side(order);
            if (side == null) {
                book.remove(chain);
            } else {
                book.put(chain, new Rest(side, toTicks(price), volume));
            }
        }
        if (order.getOrderNumber() != null) {
            chains.put(order.getOrderNumber(), chain);
        }
    }

    private static String kind(int action) {
        return switch (action) {
            case ACTION_INSERT, ACTION_UPDATE, ACTION_PRICE_ADJUSTED, ACTION_RESUMED -> "entry";
            case ACTION_FILLED -> "fill";
            case ACTION_CANCELLED, ACTION_EXPIRED, ACTION_CANCEL_ON_DISCONNECT, ACTION_FORCED_CANCEL -> "exit";
            case ACTION_SUSPENDED, ACTION_SUSPEND_ON_DISCONNECT -> "pause";
            default -> "ignore";
        };
    }

    private static String chainOf(OrderFileDto order, Map<String, String> chains, int action) {
        if (action == ACTION_INSERT) {
            return order.getOrderNumber();
        }
        if (order.getInitiatingOrderNumber() != null && chains.containsKey(order.getInitiatingOrderNumber())) {
            return chains.get(order.getInitiatingOrderNumber());
        }
        if (order.getOrderNumber() != null && chains.containsKey(order.getOrderNumber())) {
            return chains.get(order.getOrderNumber());
        }
        return order.getOrderNumber();
    }

    private static String side(OrderFileDto order) {
        if (order.getOrderTransaction() == null) {
            return null;
        }
        if (order.getOrderTransaction() == 2) {
            return "BUY";
        }
        if (order.getOrderTransaction() == 1) {
            return "SELL";
        }
        return null;
    }

    private int toTicks(double price) {
        if (price <= 0 || tickSize <= 0) {
            return 0;
        }
        return (int) Math.rint(price / tickSize);
    }

    private boolean spreadMet(Map<String, Rest> book) {
        Integer bid = bestPrice(book, "BUY");
        Integer ask = bestPrice(book, "SELL");
        return bid != null && ask != null && ask - bid <= spreadInTicks;
    }

    private boolean depthMet(Map<String, Rest> book) {
        Integer bid = ladderStart(book, "BUY");
        Integer ask = ladderStart(book, "SELL");
        return bid != null && ask != null && ask - bid <= spreadInTicks;
    }

    private Integer bestPrice(Map<String, Rest> book, String side) {
        Map<Integer, Integer> prices = prices(book, side);
        if (prices.isEmpty()) {
            return null;
        }
        return "BUY".equals(side) ? Collections.max(prices.keySet()) : Collections.min(prices.keySet());
    }

    private Integer ladderStart(Map<String, Rest> book, String side) {
        Map<Integer, Integer> prices = prices(book, side);
        if (prices.isEmpty()) {
            return null;
        }
        List<Integer> ordered = new ArrayList<>(prices.keySet());
        ordered.sort("BUY".equals(side) ? Comparator.reverseOrder() : Comparator.naturalOrder());
        int step = ("BUY".equals(side) ? -1 : 1) * levelStep;
        for (int price : ordered) {
            boolean ok = true;
            for (int level = 0; level < minLots.length; level++) {
                if (prices.getOrDefault(price + level * step, 0) < minLots[level]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return price;
            }
        }
        return null;
    }

    private static Map<Integer, Integer> prices(Map<String, Rest> book, String side) {
        Map<Integer, Integer> out = new HashMap<>();
        for (Rest rest : book.values()) {
            if (!side.equals(rest.side) || rest.volume < 1) {
                continue;
            }
            out.merge(rest.priceTicks, rest.volume, Integer::sum);
        }
        return out;
    }

    private long tradedLots() {
        Map<String, Long> lotsByTicket = new HashMap<>();
        Map<String, Integer> rowsByTicket = new HashMap<>();
        long total = 0;
        for (Held held : pending) {
            OrderFileDto order = held.order;
            if (order.getOrderAction() == null || order.getOrderAction() != ACTION_FILLED) {
                continue;
            }
            long filled = order.getFilledVolume() == null ? 0L : order.getFilledVolume();
            String ticket = order.getTicketNumber();
            if (ticket == null || ticket.isBlank()) {
                total += filled;
                continue;
            }
            lotsByTicket.merge(ticket, filled, Long::sum);
            rowsByTicket.merge(ticket, 1, Integer::sum);
        }
        for (Map.Entry<String, Long> entry : lotsByTicket.entrySet()) {
            if (rowsByTicket.getOrDefault(entry.getKey(), 0) < 2) {
                total += entry.getValue();
            }
        }
        return total;
    }

    /** B1–B5 for both sides. A missing buy level falls back to the matching S level. */
    private static int[] lots(Map<String, Integer> buyLevels, Map<String, Integer> sellLevels) {
        int[] lots = new int[5];
        for (int i = 0; i < lots.length; i++) {
            Integer buy = buyLevels == null ? null : buyLevels.get("B" + (i + 1));
            Integer sell = sellLevels == null ? null : sellLevels.get("S" + (i + 1));
            if (buy != null) {
                lots[i] = buy;
            } else if (sell != null) {
                lots[i] = sell;
            }
        }
        return lots;
    }

    private static LocalTime fromSec(int seconds) {
        if (seconds <= 0) {
            return LocalTime.MIDNIGHT;
        }
        if (seconds >= 24 * 3600) {
            return LocalTime.of(23, 59, 59);
        }
        return LocalTime.ofSecondOfDay(seconds);
    }

    public List<StreakRow> getStreaks() {
        ensureCalculated();
        return streakRows;
    }

    public List<AuditRow> getAudits() {
        ensureCalculated();
        return auditRows;
    }

    public ReportforMMIDSymbolDto getReport() {
        ensureCalculated();
        return new ReportforMMIDSymbolDto(mmid, companyName, symbol, contract, Boolean.TRUE.equals(isRolloutConfig),
                activeSpreadVarianceTime, activeQuantityVarianceTime, activeSpreadQuantityVarianceTime,
                percentOfTotalActiveSpreadQuantityVarianceTime, totalVolumeTraded, eligibleFlag, marketType,
                configuredVolumePerDay, totalFilledVolumeFlag, presenceTimeFlag);
    }

    public ReportforStarttimeEndTimeDto getIntervalReport() {
        ensureCalculated();
        ReportforStarttimeEndTimeDto dto = new ReportforStarttimeEndTimeDto(mmid, symbol);
        for (ReportforStarttimeEndTimeDto.Interval interval : varianceIntervals) {
            dto.addInterval(interval.getStart(), interval.getEnd());
        }
        return dto;
    }

    public ReportforMMIDSymbolSecondReportDto getReportForSecondReport() {
        ensureCalculated();
        return new ReportforMMIDSymbolSecondReportDto(mmid, companyName, symbol, contract, isRolloutConfig,
                tickSize, spreadInTicks, QtyAtEachPriceLevel, configuredPresenceTime, activeSpreadVarianceTime,
                activeQuantityVarianceTime, activeSpreadQuantityVarianceTime,
                percentOfTotalActiveSpreadQuantityVarianceTime, totalVolumeTraded, eligibleFlag, marketType,
                configuredVolumePerDay, totalFilledVolumeFlag, presenceTimeFlag);
    }

    private static final class Rest {
        final String side;
        final int priceTicks;
        final int volume;

        Rest(String side, int priceTicks, int volume) {
            this.side = side;
            this.priceTicks = priceTicks;
            this.volume = volume;
        }
    }

    private static final class Held {
        final OrderFileDto order;
        final int seq;
        final int timeSec;

        Held(OrderFileDto order, int seq) {
            this(order, seq, 0);
        }

        Held(OrderFileDto order, int seq, int timeSec) {
            this.order = order;
            this.seq = seq;
            this.timeSec = timeSec;
        }
    }

    public record StreakRow(String mmid, String contractCode, String symbolName, String metric,
                            LocalTime start, LocalTime end, int duration) {
    }

    public record AuditRow(String mmid, String contractCode, String obligation, LocalTime from, LocalTime to,
                           int seconds, String reason) {
    }

    private static final class Snap {
        final String side;
        final int ticks;
        final int volume;
        final boolean live;

        Snap(String side, int ticks, int volume, boolean live) {
            this.side = side;
            this.ticks = ticks;
            this.volume = volume;
            this.live = live;
        }
    }

    private static final class Seg {
        final int start;
        final int end;
        final boolean spread;
        final boolean depth;
        final String spreadReason;
        final String depthReason;
        final String changed;

        Seg(int start, boolean spread, boolean depth, String spreadReason, String depthReason, String changed) {
            this(start, 0, spread, depth, spreadReason, depthReason, changed);
        }

        Seg(int start, int end, boolean spread, boolean depth, String spreadReason, String depthReason, String changed) {
            this.start = start;
            this.end = end;
            this.spread = spread;
            this.depth = depth;
            this.spreadReason = spreadReason;
            this.depthReason = depthReason;
            this.changed = changed == null ? "" : changed;
        }
    }
}
