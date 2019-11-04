package com.dreamfabric.jac64.emu;

public abstract class TimeEvent {
    // For linking events...
    TimeEvent nextEvent;
    TimeEvent prevEvent;
    boolean scheduled = false;

    String name;

    protected long cpuCyclesTime;

    public TimeEvent(long cpuCyclesTime) {
        this.cpuCyclesTime = cpuCyclesTime;
    }

    public TimeEvent(long cpuCyclesTime, String name) {
        this.cpuCyclesTime = cpuCyclesTime;
        this.name = name;
    }

    public final long getTime() {
        return cpuCyclesTime;
    }

    public void setTime(long cpuCyclesTime) {
        this.cpuCyclesTime = cpuCyclesTime;
    }

    public abstract void execute(long currentCpuCycles);

    public String getShort() {
        return "" + cpuCyclesTime + (name != null ? ": " + name : "");
    }

    public boolean isScheduled() {
        return scheduled;
    }
} // TimeEvent
