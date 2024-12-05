package org.openjdk.jmh.it.params.jmh_generated;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
public class EmptyTrailingStringParamTest_jmhType_B2 extends EmptyTrailingStringParamTest_jmhType_B1 {
    public volatile int setupTrialMutex;
    public volatile int tearTrialMutex;
    public final static AtomicIntegerFieldUpdater<EmptyTrailingStringParamTest_jmhType_B2> setupTrialMutexUpdater = AtomicIntegerFieldUpdater.newUpdater(EmptyTrailingStringParamTest_jmhType_B2.class, "setupTrialMutex");
    public final static AtomicIntegerFieldUpdater<EmptyTrailingStringParamTest_jmhType_B2> tearTrialMutexUpdater = AtomicIntegerFieldUpdater.newUpdater(EmptyTrailingStringParamTest_jmhType_B2.class, "tearTrialMutex");

    public volatile int setupIterationMutex;
    public volatile int tearIterationMutex;
    public final static AtomicIntegerFieldUpdater<EmptyTrailingStringParamTest_jmhType_B2> setupIterationMutexUpdater = AtomicIntegerFieldUpdater.newUpdater(EmptyTrailingStringParamTest_jmhType_B2.class, "setupIterationMutex");
    public final static AtomicIntegerFieldUpdater<EmptyTrailingStringParamTest_jmhType_B2> tearIterationMutexUpdater = AtomicIntegerFieldUpdater.newUpdater(EmptyTrailingStringParamTest_jmhType_B2.class, "tearIterationMutex");

    public volatile int setupInvocationMutex;
    public volatile int tearInvocationMutex;
    public final static AtomicIntegerFieldUpdater<EmptyTrailingStringParamTest_jmhType_B2> setupInvocationMutexUpdater = AtomicIntegerFieldUpdater.newUpdater(EmptyTrailingStringParamTest_jmhType_B2.class, "setupInvocationMutex");
    public final static AtomicIntegerFieldUpdater<EmptyTrailingStringParamTest_jmhType_B2> tearInvocationMutexUpdater = AtomicIntegerFieldUpdater.newUpdater(EmptyTrailingStringParamTest_jmhType_B2.class, "tearInvocationMutex");

}
