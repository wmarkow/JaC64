package com.dreamfabric.jac64.emu.cia.timer;

import com.dreamfabric.jac64.emu.SimulableIf;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;
import com.dreamfabric.jac64.emu.scheduler.TimeEvent;

public class RealTimeClock implements SimulableIf {

    private static final int SAMPLE_RATE = 10;
    private int CPUFrq = 985248;

    private int tod10sec = 0;
    private int todsec = 0;
    private int todmin = 0;
    private int todhour = 0;

    private EventQueue scheduler;
    private int clocksPerSample = CPUFrq / SAMPLE_RATE;

    public RealTimeClock(EventQueue scheduler) {
        this.scheduler = scheduler;
    }

    TimeEvent updateEvent = new TimeEvent(0) {
        public void execute(long currentCpuCycles) {
            RealTimeClock.this.execute();

            scheduler.addEvent(this, currentCpuCycles + clocksPerSample);
        }
    };

    public int getTod10sec() {
        return tod10sec;
    }

    public void setTod10sec(int tod10sec) {
        this.tod10sec = tod10sec;
    }

    public int getTodsec() {
        return todsec;
    }

    public void setTodsec(int todsec) {
        this.todsec = todsec;
    }

    public int getTodmin() {
        return todmin;
    }

    public void setTodmin(int todmin) {
        this.todmin = todmin;
    }

    public int getTodhour() {
        return todhour;
    }

    public void setTodhour(int todhour) {
        this.todhour = todhour;
    }

    @Override
    public void start(long currentCpuCycles) {
        scheduler.addEvent(updateEvent, currentCpuCycles + clocksPerSample);
    }

    @Override
    public void stop() {
        scheduler.removeEvent(updateEvent);
    }

    @Override
    public void reset() {
        tod10sec = 0;
        todsec = 0;
        todmin = 0;
        todhour = 0;
    }

    private void execute() {
        int tmp = (tod10sec & 0x0f) + 1;
        tod10sec = tmp % 10;
        if (tmp > 9) {
            // Maxval == 0x59
            tmp = (todsec & 0x7f) + 1;
            if ((tmp & 0x0f) > 9)
                tmp += 0x06;
            if (tmp > 0x59)
                tmp = 0;
            todsec = tmp;
            // Wrapped seconds...
            // Minutes inc - max 0x59
            if (tmp == 0) {
                tmp = (todmin & 0x7f) + 1;
                if ((tmp & 0x0f) > 9)
                    tmp += 0x06;
                if (tmp > 0x59)
                    tmp = 0;
                todmin = tmp;

                // Hours, max 0x12
                if (tmp == 0) {
                    tmp = (todhour & 0x1f) + 1;
                    if ((tmp & 0x0f) > 9)
                        tmp += 0x06;

                    if (tmp > 0x11)
                        tmp = 0;
                    // how is hour? 1 - 12 or 0 - 11 ??
                    todhour = tmp;
                }
            }
        }

        // TODO: fix alarms and latches !!
        // Since this should continue run, just reschedule...
        // System.out.println("TOD ticked..." + (todhour>>4) + (todhour & 0xf) + ":" +
        // (todmin>>4) + (todmin&0xf) + ":" + (todsec>>4) + (todsec&0xf));
    }
}
