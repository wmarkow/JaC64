package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.EventQueue;
import com.dreamfabric.jac64.emu.TimeEvent;

public class RealTimeClock {

    private int tod10sec = 0;
    private int todsec = 0;
    private int todmin = 0;
    private int todhour = 0;

    private EventQueue scheduler;

    public RealTimeClock(EventQueue scheduler, long cycles) {
        this.scheduler = scheduler;

        todEvent.setTime(cycles + 10000);
        scheduler.addEvent(todEvent);
    }

    public void reset() {
        tod10sec = 0;
        todsec = 0;
        todmin = 0;
        todhour = 0;
    }

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

    public TimeEvent getTodEvent() {
        return todEvent;
    }

    public void setTodEvent(TimeEvent todEvent) {
        this.todEvent = todEvent;
    }

    private TimeEvent todEvent = new TimeEvent(0) {
        public void execute(long cycle) {
            time = time + 100000; // Approx a tenth of a second...
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
            scheduler.addEvent(this);
        }
    };
}
