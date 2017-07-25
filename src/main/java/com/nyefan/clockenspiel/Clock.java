package com.nyefan.clockenspiel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author nyefan
 */
public class Clock {

    private static final Logger LOGGER = LoggerFactory.getLogger(Clock.class);

    public final boolean PRINT_INTERMEDIATE;

    private final int      THREAD_POOL_SIZE;
    private final long     TIMEOUT_LENGTH;
    private final TimeUnit TIMEOUT_UNIT;

    private final int ITERATIONS_PER_CYCLE;
    private final int MIN_CYCLES;
    private final int STAT_CYCLES;
    private final int STAT_VERIFY_CYCLES;
    private final int MAX_CYCLES;

    private Clock(boolean printIntermediate,
                  int threadPoolSize,
                  long timeoutLength,
                  TimeUnit timeoutUnit,
                  int iterationsPerCycle,
                  int minCycles,
                  int statCycles,
                  int statVerifyCycles,
                  int maxCycles) {
        PRINT_INTERMEDIATE = printIntermediate;
        THREAD_POOL_SIZE = threadPoolSize;
        TIMEOUT_LENGTH = timeoutLength;
        TIMEOUT_UNIT = timeoutUnit;
        ITERATIONS_PER_CYCLE = iterationsPerCycle;
        MIN_CYCLES = minCycles;
        STAT_CYCLES = statCycles;
        STAT_VERIFY_CYCLES = statVerifyCycles;
        MAX_CYCLES = maxCycles;
    }

    public <T> List<Duration> time(Callable<Callable<T>> generator, String testName) throws Exception {
        List<Duration> results = new LinkedList<>();

        if (PRINT_INTERMEDIATE) {
            LOGGER.info(attachMetadata(testName));
        }

        while (!varianceIsAcceptable(results)) {
            Callable<T> task     = generator.call();
            Duration    duration = time(generateTasks(task));

            results.add(duration);

            if (PRINT_INTERMEDIATE) {
                printDuration(duration);
            }
        }

        if (!PRINT_INTERMEDIATE) {
            LOGGER.info(attachMetadata(testName));
            printDuration(results);
        }

        return results;
    }

    public <T> List<Duration> time(Callable<Callable<T>> generator) throws Exception {
        List<Duration> results = new LinkedList<>();

        while (!varianceIsAcceptable(results)) {
            results.add(time(generateTasks(generator.call())));
        }

        return results;
    }

    public <T> Duration timeGeneratedFunctions(Callable<T> task) throws InterruptedException {
        return time(generateTasks(task));
    }

    public <T> Duration time(List<Callable<T>> tasks) throws InterruptedException {
        Instant         start;
        Instant         end;
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        start = Instant.now();
        executorService.invokeAll(tasks, TIMEOUT_LENGTH, TIMEOUT_UNIT);
        end = Instant.now();

        return Duration.between(start, end);
    }

    public <T> List<Callable<T>> generateTasks(Callable<T> task) {
        List<Callable<T>> tasks = new ArrayList<>(ITERATIONS_PER_CYCLE);

        for (int i = 0; i < ITERATIONS_PER_CYCLE; i++) {
            tasks.add(task);
        }

        return tasks;
    }

    public boolean varianceIsAcceptable(List<Duration> results) {
        if (results.size() < MIN_CYCLES || results.size() >= MAX_CYCLES) {
            return false;
        } else {
            List<Double> statList = results.subList(Math.max(results.size() - STAT_CYCLES, 0), results.size())
                    .stream()
                    .map(duration -> Long.valueOf(duration.getSeconds()).doubleValue() + Integer.valueOf(duration.getNano()).doubleValue())
                    .collect(Collectors.toList());
            double mean = statList.stream()
                    .mapToDouble(Double::valueOf)
                    .average()
                    .getAsDouble();
            double variance = statList.stream()
                    .reduce(0d, (u, v) -> u + (v - mean) * (v + mean))
                    / (double) (statList.size() - 1);
            double stdev = Math.sqrt(variance);

            double min = mean - stdev / 2;
            double max = mean + stdev / 2;

            List<Double> statVerifyList = statList.subList(Math.max(statList.size() - STAT_VERIFY_CYCLES, 0), statList.size());
            return statVerifyList.stream().anyMatch(duration -> duration < min || duration > max);
        }
    }

    public String attachMetadata(String testName) {
        return String.format("%s - %d iterations per cycle, %d threads, %d %s timeout per cycle",
                             testName, ITERATIONS_PER_CYCLE, THREAD_POOL_SIZE, TIMEOUT_LENGTH, TIMEOUT_UNIT.toString().toLowerCase());
    }

    public static void printDuration(List<Duration> results) {
        results.stream()
                .forEach(Clock::printDuration);
    }

    public static void printDuration(Duration duration) {
        double d = Long.valueOf(duration.getSeconds()).doubleValue() + Integer.valueOf(duration.getNano()).doubleValue() / 1000000000d;
        LOGGER.info(String.format("%.3f seconds", d));
    }

    public static class ClockBuilder {

        private boolean printIntermediate = true;

        private int      threadPoolSize = 1;
        private long     timeoutLength  = 1L;
        private TimeUnit timeoutUnit    = TimeUnit.MINUTES;

        private int iterationsPerCycle = 10_000;
        private int minCycles          = 25;
        private int statCycles         = 20;
        private int statVerifyCycles   = 5;
        private int maxCycles          = 100;

        public ClockBuilder() {}

        public Clock build() {
            return new Clock(printIntermediate,
                             threadPoolSize,
                             timeoutLength,
                             timeoutUnit,
                             iterationsPerCycle,
                             minCycles,
                             statCycles,
                             statVerifyCycles,
                             maxCycles);
        }

        public ClockBuilder setPrintIntermediate(boolean p) {
            printIntermediate = p;
            return this;
        }

        public ClockBuilder setThreadPoolSize(int s) {
            threadPoolSize = s;
            return this;
        }

        public ClockBuilder setTimeoutLength(long l) {
            timeoutLength = l;
            return this;
        }

        public ClockBuilder setTimeoutUnit(TimeUnit u) {
            timeoutUnit = u;
            return this;
        }

        public ClockBuilder setIterationsPerCycle(int i) {
            iterationsPerCycle = i;
            return this;
        }

        public ClockBuilder setMinCycles(int c) {
            minCycles = c;
            return this;
        }

        public ClockBuilder setStatCycles(int c) {
            statCycles = c;
            return this;
        }

        public ClockBuilder setStatVerifyCycles(int c) {
            statVerifyCycles = c;
            return this;
        }

        public ClockBuilder setMaxCycles(int c) {
            maxCycles = c;
            return this;
        }
    }
}
