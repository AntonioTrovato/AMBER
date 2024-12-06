/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.runner;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.reconfigure.manager.DynamicHaltManager;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.TreeMultimap;
import org.openjdk.jmh.util.Utils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstract runner, the base class for Runner and ForkedRunner.
 */
abstract class BaseRunner {

    private long projectedTotalTime;
    private long projectedRunningTime;
    private long actualRunningTime;
    private long benchmarkStart;
    private ArrayList<Integer> actual_warmup_iterations;
    private ArrayList<Integer> actual_measurement_iterations;

    protected final Options options;
    protected final OutputFormat out;

    public BaseRunner(Options options, OutputFormat handler) {
        if (options == null) {
            throw new IllegalArgumentException("Options is null.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler is null.");
        }
        this.options = options;
        this.out = handler;
    }

    protected void runBenchmarksForked(ActionPlan actionPlan, IterationResultAcceptor acceptor) {
        actual_warmup_iterations = new ArrayList<>();
        actual_measurement_iterations = new ArrayList<>();
        for (Action action : actionPlan.getActions()) {
            BenchmarkParams params = action.getParams();
            ActionMode mode = action.getMode();

            doSingle(params, mode, acceptor);
        }
    }

    protected Multimap<BenchmarkParams, BenchmarkResult> runBenchmarksEmbedded(ActionPlan actionPlan) {
        Multimap<BenchmarkParams, BenchmarkResult> results = new TreeMultimap<>();

        for (Action action : actionPlan.getActions()) {
            BenchmarkParams params = action.getParams();
            ActionMode mode = action.getMode();

            long startTime = System.currentTimeMillis();

            out.startBenchmark(params);
            out.println("");
            etaBeforeBenchmark();
            out.println("# Fork: N/A, test runs in the host VM");
            out.println("# *** WARNING: Non-forked runs may silently omit JVM options, mess up profilers, disable compiler hints, etc. ***");
            out.println("# *** WARNING: Use non-forked runs only for debugging purposes, not for actual performance runs. ***");

            final List<IterationResult> res = new ArrayList<>();
            final List<BenchmarkResultMetaData> mds = new ArrayList<>();

            IterationResultAcceptor acceptor = new IterationResultAcceptor() {
                @Override
                public void accept(IterationResult iterationData) {
                    res.add(iterationData);
                }

                @Override
                public void acceptMeta(BenchmarkResultMetaData md) {
                    mds.add(md);
                }
            };

            doSingle(params, mode, acceptor);

            if (!res.isEmpty()) {
                BenchmarkResultMetaData md = mds.get(0);
                if (md != null) {
                    md.adjustStart(startTime);
                }

                BenchmarkResult br = new BenchmarkResult(params, res, md);
                results.put(params, br);
                out.endBenchmark(br);
            }

            etaAfterBenchmark(params);
        }
        return results;
    }

    private void doSingle(BenchmarkParams params, ActionMode mode, IterationResultAcceptor acceptor) {
        try {
            switch (mode) {
                case WARMUP: {
                    runBenchmark(params, null);
                    out.println("");
                    break;
                }
                case WARMUP_MEASUREMENT:
                case MEASUREMENT: {
                    runBenchmark(params, acceptor);
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown mode: " + mode);

            }
        } catch (BenchmarkException be) {
            out.println("<failure>");
            out.println("");
            for (Throwable cause : be.getSuppressed()) {
                out.println(Utils.throwableToString(cause));
            }
            out.println("");

            if (options.shouldFailOnError().orElse(Defaults.FAIL_ON_ERROR)) {
                throw be;
            }
        }
    }

    protected void etaAfterBenchmark(BenchmarkParams params) {
        long current = System.nanoTime();
        projectedRunningTime += estimateTimeSingleFork(params);
        actualRunningTime += (current - benchmarkStart);
        benchmarkStart = current;
    }

    protected void etaBeforeBenchmarks(Collection<ActionPlan> plans) {
        projectedTotalTime = 0;
        for (ActionPlan plan : plans) {
            for (Action act : plan.getActions()) {
                BenchmarkParams params = act.getParams();
                projectedTotalTime += (Math.max(1, params.getForks()) + params.getWarmupForks()) * estimateTimeSingleFork(params);
            }
        }
    }

    private long estimateTimeSingleFork(BenchmarkParams params) {
        IterationParams wp = params.getWarmup();
        IterationParams mp = params.getMeasurement();

        long estimatedTime;
        if (params.getMode() == Mode.SingleShotTime) {
            // No way to tell how long it will execute,
            // guess anything, and let ETA compensation to catch up.
            estimatedTime = (wp.getCount() + mp.getCount()) * TimeUnit.MILLISECONDS.toNanos(1);
        } else {
            estimatedTime =
                    (wp.getCount() * wp.getTime().convertTo(TimeUnit.NANOSECONDS) +
                     mp.getCount() * mp.getTime().convertTo(TimeUnit.NANOSECONDS));
        }
        return estimatedTime;
    }

    protected void etaBeforeBenchmark() {
        if (benchmarkStart == 0) {
            benchmarkStart = System.nanoTime();
        }

        long totalETA;
        double partsDone = 1.0D * projectedRunningTime / projectedTotalTime;
        if (partsDone != 0) {
            totalETA = (long) (actualRunningTime * (1.0D / partsDone - 1));
        } else {
            totalETA = projectedTotalTime;
        }

        out.println(String.format("# Run progress: %.2f%% complete, ETA %s", partsDone * 100, formatDuration(totalETA)));
    }

    protected void etaAfterBenchmarks() {
        out.println(String.format("# Run complete. Total time: %s", formatDuration(actualRunningTime)));
        out.println("");
    }

    private String formatDuration(long nanos) {
        long days = TimeUnit.NANOSECONDS.toDays(nanos);
        nanos -= days * TimeUnit.DAYS.toNanos(1);

        long hrs = TimeUnit.NANOSECONDS.toHours(nanos);
        nanos -= hrs * TimeUnit.HOURS.toNanos(1);

        long mins = TimeUnit.NANOSECONDS.toMinutes(nanos);
        nanos -= mins * TimeUnit.MINUTES.toNanos(1);

        long secs = TimeUnit.NANOSECONDS.toSeconds(nanos);

        return String.format("%s%02d:%02d:%02d", (days > 0) ? days + " days, " : "", hrs, mins, secs);
    }

    void runBenchmark(BenchmarkParams benchParams, IterationResultAcceptor acceptor) {
        BenchmarkHandler handler = null;
        try {
            handler = new BenchmarkHandler(out, options, benchParams);
            runBenchmark(benchParams, handler, acceptor);
        } catch (BenchmarkException be) {
            throw be;
        } catch (Throwable ex) {
            throw new BenchmarkException(ex);
        } finally {
            if (handler != null) {
                handler.shutdown();
            }
        }
    }

    private boolean incorrectIterationDuration(long iterationTime, TimeUnit iterationTimeUnit) {
        long durationInMillis = iterationTimeUnit.toMillis(iterationTime);
        return durationInMillis < 100;
    }

    private boolean dynamicHaltIsApplicable(BenchmarkParams benchParams) {
        return !benchParams.getDynamicHaltHost().isEmpty() && !benchParams.getDynamicHaltPort().isEmpty() && !benchParams.getDynamicHaltModel().isEmpty();
    }

    protected void runBenchmark(BenchmarkParams benchParams, BenchmarkHandler handler, IterationResultAcceptor acceptor) {
        long warmupTime = System.currentTimeMillis();

        long allWarmup = 0;
        long allMeasurement = 0;

        // create an instance of the dynamic halt manager
        DynamicHaltManager dinamicHalt = new DynamicHaltManager(benchParams);
        int maxDuration = 600;
        int currentIterations = 0;
        boolean thrpt = false; // if is throughput mode we must apply conversion to avg exec time
        boolean halted = false;

        // warmup
        IterationParams wp = benchParams.getWarmup();
        int wpIterations = wp.getCount();

        if (!dynamicHaltIsApplicable(benchParams)) {
            out.println("The Dynamic Halt is NOT Active");
        }else {
            wp.setTime(100,TimeUnit.MILLISECONDS);
            wpIterations = 500;
        }

        if(benchParams.getMode() == Mode.Throughput)
            thrpt = true;

        //System.out.println("Throughput Mode: " + thrpt);

        for (int i = 1; i <= wpIterations; i++) {
            currentIterations++;
            // will run system gc if we should
            if (runSystemGC()) {
                out.verbosePrintln("System.gc() executed");
            }

            out.iteration(benchParams, wp, i);
            boolean isFirstIteration = (i == 1);
            boolean isLastIteration = (benchParams.getMeasurement().getCount() == 0);
            IterationResult ir = handler.runIteration(benchParams, wp, isFirstIteration, isLastIteration);
            out.iterationResult(benchParams, wp, i, ir);

            /*System.out.println("Totale iterazioni eseguite fino ad ora: " + currentIterations);
            System.out.println("Iterazione di Warm-up numero: " + i);
            System.out.println("Risultati: " + ir.getPrimaryResult().getStatistics());*/

            allWarmup += ir.getMetadata().getAllOps();

            if (dynamicHaltIsApplicable(benchParams)) {
                dinamicHalt.addIteration(currentIterations, ir, thrpt);
                if (dinamicHalt.checkHaltCondition(benchParams.getDynamicHaltHost(), benchParams.getDynamicHaltPort(), benchParams.getDynamicHaltModel())) {
                    System.out.println("Halt eseguito");
                    halted = true;
                    break;
                }
                if (currentIterations >= 500)
                    break;
            }
        }

        //int actual_warmup_iterations = currentIterations;

        //int actual_measurement_iterations = 0;

        long measurementTime = 0;
        long stopTime = 0;
        List<IterationResult> default_measurements = new ArrayList<>();

        if (!halted) {
            measurementTime = System.currentTimeMillis();

            // measurement
            IterationParams mp = benchParams.getMeasurement();
            int mtIterations = mp.getCount();

            if (dynamicHaltIsApplicable(benchParams)) {
                mp.setTime(100,TimeUnit.MILLISECONDS);
                mtIterations = 100;
            }

            for (int i = 1; i <= mtIterations; i++) {
                currentIterations++;
                // will run system gc if we should
                if (runSystemGC()) {
                    out.verbosePrintln("System.gc() executed");
                }

                // run benchmark iteration
                out.iteration(benchParams, mp, i);

                boolean isFirstIteration = (benchParams.getWarmup().getCount() == 0) && (i == 1);
                boolean isLastIteration = (i == mp.getCount());
                IterationResult ir = handler.runIteration(benchParams, mp, isFirstIteration, isLastIteration);
                out.iterationResult(benchParams, mp, i, ir);

                /*System.out.println("Totale iterazioni eseguite fino ad ora: " + currentIterations);
                System.out.println("Iterazione di Misurazione numero: " + i);
                System.out.println("Risultati: " + ir.getPrimaryResult().getStatistics());*/

                allMeasurement += ir.getMetadata().getAllOps();

                //save the results in case we can't halt the executions
                default_measurements.add(ir);

                if (dynamicHaltIsApplicable(benchParams)) {
                    /*dinamicHalt.addIteration(currentIterations, ir, thrpt);
                    if (dinamicHalt.checkHaltCondition(benchParams.getDynamicHaltHost(), benchParams.getDynamicHaltPort(), benchParams.getDynamicHaltModel())) {
                        System.out.println("Halt eseguito");
                        halted = true;
                        break;
                    }*/
                    if (currentIterations >= maxDuration)
                        break;
                }

                /*if (acceptor != null) {
                    acceptor.accept(ir);
                }*/


            }

            //actual_measurement_iterations = currentIterations - actual_warmup_iterations;

            stopTime = System.currentTimeMillis();
        }

        /*System.out.println("Actual warm-up iterations: " + actual_warmup_iterations);
        System.out.println("Actual measurement iterations: " + actual_measurement_iterations);*/

        /*System.out.println("Misurazioni di default: ");
        for (IterationResult ir : default_measurements)
            System.out.println(ir.getPrimaryResult().getStatistics());
        System.out.println("Misurazioni che sono state registrate nell'ultima finestra");
        List<IterationResult> iterationResults1 = dinamicHalt.getLast100IterationResults();
        for (IterationResult ir : iterationResults1)
            System.out.println(ir.getPrimaryResult().getStatistics());

        System.out.println("Halted: " + halted);*/

        //if the benchmark was halted, consider the last 100 for evaluations
        //else, consider the measurements got from the standard measurements iterations
        if (halted && acceptor != null) {
            List<IterationResult> iterationResults = dinamicHalt.getLast100IterationResults();
            for (IterationResult ir : iterationResults)
                acceptor.accept(ir);
        }else if(!halted && acceptor != null)
            for (IterationResult ir : default_measurements)
                acceptor.accept(ir);

        // we use the second constructor because the actual number of iterations could be
        // different from the set one
        BenchmarkResultMetaData md = new BenchmarkResultMetaData(
                warmupTime, measurementTime, stopTime,
                allWarmup, allMeasurement);

        md.addWarmupIterations(currentIterations - 100);
        md.addMeasurementIterations(100);

        if (acceptor != null) {
            acceptor.acceptMeta(md);
        }
    }

    /**
     * Execute System.gc() if we the System.gc option is set.
     *
     * @return true if we did
     */
    public boolean runSystemGC() {
        if (options.shouldDoGC().orElse(Defaults.DO_GC)) {
            List<GarbageCollectorMXBean> enabledBeans = new ArrayList<>();

            long beforeGcCount = 0;
            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                long count = bean.getCollectionCount();
                if (count != -1) {
                    enabledBeans.add(bean);
                }
            }

            for (GarbageCollectorMXBean bean : enabledBeans) {
                beforeGcCount += bean.getCollectionCount();
            }

            // Run the GC twice, and force finalization before each GCs.
            System.runFinalization();
            System.gc();
            System.runFinalization();
            System.gc();

            // Now make sure GC actually happened. We have to wait for two things:
            //   a) That at least two collections happened, indicating GC work.
            //   b) That counter updates have not happened for a while, indicating GC work had ceased.
            //
            // Note there is an opportunity window for a concurrent GC to happen before the first
            // System.gc() call, which would get counted towards our GCs. This race is unresolvable
            // unless we have GC-specific information about the collection cycles, and verify those
            // were indeed GCs triggered by us.

            final int MAX_WAIT_MSEC = 20 * 1000;

            if (enabledBeans.isEmpty()) {
                out.println("WARNING: MXBeans can not report GC info. System.gc() invoked, pessimistically waiting " + MAX_WAIT_MSEC + " msecs");
                try {
                    TimeUnit.MILLISECONDS.sleep(MAX_WAIT_MSEC);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }

            boolean gcHappened = false;

            long start = System.nanoTime();
            while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < MAX_WAIT_MSEC) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                long afterGcCount = 0;
                for (GarbageCollectorMXBean bean : enabledBeans) {
                    afterGcCount += bean.getCollectionCount();
                }

                if (!gcHappened) {
                    if (afterGcCount - beforeGcCount >= 2) {
                        gcHappened = true;
                    }
                } else {
                    if (afterGcCount == beforeGcCount) {
                        // Stable!
                        return true;
                    }
                    beforeGcCount = afterGcCount;
                }
            }

            if (gcHappened) {
                out.println("WARNING: System.gc() was invoked but unable to wait while GC stopped, is GC too asynchronous?");
            } else {
                out.println("WARNING: System.gc() was invoked but couldn't detect a GC occurring, is System.gc() disabled?");
            }
            return false;
        }
        return false;
    }

}
