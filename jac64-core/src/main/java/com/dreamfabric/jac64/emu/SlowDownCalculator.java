package com.dreamfabric.jac64.emu;

public class SlowDownCalculator {

    private long requiredCyclesPerSecond;
    private long loopStartNanos;
    private long loopStartCycles;
    private double oneCycleInNanos;

    public SlowDownCalculator(int requiredCyclesPerSecond) {
        this.requiredCyclesPerSecond = requiredCyclesPerSecond;

        this.oneCycleInNanos = 1e9 / requiredCyclesPerSecond;
    }

    public void markLoopStart(long loopStartNanos, long loopStartCycles) {
        this.loopStartNanos = loopStartNanos;
        this.loopStartCycles = loopStartCycles;
    }

    public long calculateWaitInNanos(long markLoopEndNanos, long loopEndCycles) {
        long durationNanos = markLoopEndNanos - loopStartNanos;
        long deltaCycles = loopEndCycles - loopStartCycles;

        double curCyclesPerSecond = 1e9 * deltaCycles / durationNanos;

        if (curCyclesPerSecond > requiredCyclesPerSecond) {
            // too fast. The host machine is to fast. Need to slow down.
            double delayInNanos = deltaCycles * oneCycleInNanos;
            
            return (long)delayInNanos;
        }
        
        // to slow. Is the host machine to slow? No delay required.
        return 0;

    }
}
