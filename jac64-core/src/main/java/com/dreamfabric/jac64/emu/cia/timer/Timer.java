package com.dreamfabric.jac64.emu.cia.timer;

import com.dreamfabric.jac64.emu.SimulableIf;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;
import com.dreamfabric.jac64.emu.scheduler.TimeEvent;

//A timer class for handling the Timer state machines
// The CIATimer is inspired by the CIA Timer State Machine in the
// Frodo emulator
public class Timer implements SimulableIf {

    // The states of the timer
    private static final int STOP = 0;
    private static final int WAIT = 1;
    private static final int LOAD_STOP = 2;
    private static final int LOAD_COUNT = 3;
    private static final int LOAD_WAIT_COUNT = 5;
    private static final int COUNT = 6;
    private static final int COUNT_STOP = 7;

    // If timer is connected...
    Timer otherTimer;

    int state = STOP;
    // The latch for this timer
    private int latch = 0;
    int timer = 0;

    // When is an update needed for this timer?
    long nextUpdate = 0;
    long nextZero = 0;
    long lastLatch = 0;

    boolean interruptNext = false;
    boolean underflow = false;
    boolean flipflop = false;

    private boolean countCycles = false;
    private boolean countUnderflow = false;

    private EventQueue scheduler;

    TimeEvent updateEvent = new TimeEvent(0) {
        public void execute(long currentCpuCycles) {
            doUpdate(currentCpuCycles);
            if (state != STOP) {
                // System.out.println(ciaID() + " Adding Timer update event at " + cycles +
                // " with time: " + nextUpdate + " state: " + state);
                scheduler.addEvent(this, nextUpdate);
            }
        }
    };

    private int writeCR = -1;
    private int cr = 0;
    String id;
    int iflag;
    boolean updateOther;
    private TimerListenerIf timerListener;

    public Timer(String id, boolean uo, Timer other, EventQueue scheduler) {
        this.id = id;
        this.otherTimer = other;
        updateOther = uo;
        this.scheduler = scheduler;
    }

    public void setTimerListener(TimerListenerIf timerListener) {
        this.timerListener = timerListener;
    }

    @Override
    public void start(long currentCpuCycles) {
        // it looks like the timer is already started (automagically?)
    }

    @Override
    public void stop() {
    }

    @Override
    public void reset() {
        latch = 0xffff;
        timer = 0xffff;
        countUnderflow = false;
        flipflop = false;
        state = STOP;
        nextZero = 0;
        nextUpdate = 0;
        writeCR = -1;
        scheduler.removeEvent(updateEvent);
    }

    public int getTimer(long cycles) {
        if (state != COUNT)
            return timer;
        int t = (int) (nextZero - cycles);
        if (t < 0) {
            // Unexpected underflow...
            t = 0;
        }
        timer = t;
        return t;
    }

    public int getTimerLowByteValue(long currentCpuCycles) {
        return getTimer(currentCpuCycles) & 0xff;
    }

    public int getTimerHighByteValue(long currentCpuCycles) {
        return getTimer(currentCpuCycles) >> 8;
    }

    public void setLatchLowByte(int value) {
        latch = (latch & 0xff00) | value;
    }

    public void setLatchHighByte(int value) {
        latch = (latch & 0xff) | (value << 8);
        if (state == Timer.STOP) {
            timer = latch;
        }
    }

    public int getLatch() {
        return latch;
    }

    private void loadTimer(long currentCpuCycles) {
        timer = latch;
        nextZero = currentCpuCycles + latch;
    }

    private void triggerInterrupt(long currentCpuCycles) {
        interruptNext = true;
        underflow = true;
        flipflop = !flipflop;
        // One shot?
        if ((cr & 8) != 0) {
            cr &= 0xfe;
            writeCR &= 0xfe;
            state = LOAD_STOP;
        } else {
            state = LOAD_COUNT;
        } // TODO: fix update of tb.
          // if (updateOther) {
          // otherTimer.update(cycles);
          // }
    }

    public void setCR(long currentCpuCycles, int data) {
        writeCR = data;
        if (nextUpdate > currentCpuCycles + 1 || !updateEvent.isScheduled()) {
            nextUpdate = currentCpuCycles + 1;
            scheduler.addEvent(updateEvent, nextUpdate);
        }
    }

    public int getCR() {
        return cr;
    }

    public void doUpdate(long currentCpuCycles) {
        if (nextUpdate == 0) {
            nextUpdate = currentCpuCycles;
            nextZero = currentCpuCycles;
        }
        if (currentCpuCycles == nextUpdate) {
            // If the call is on "spot" then just do it!
            update(currentCpuCycles);
        } else {
            // As long as cycles is larger than next update, just continue
            // call it!
            while (currentCpuCycles >= nextUpdate) {
                update(nextUpdate);
            }
        }
    }

    public void setCountCycles(boolean countCycles) {
        this.countCycles = countCycles;
    }

    public void setCountUnderflow(boolean countUnderflow) {
        this.countUnderflow = countUnderflow;
    }

    // maybe only update when "needed" and when registers read???
    // NEED TO CHECK IF WE ARE CALLED WHEN EXPECTED!!!
    // No - since BA Low will cause no updates of IO units..??
    public void update(long currentCpuCycles) {
        // set a default
        underflow = false;
        nextUpdate = currentCpuCycles + 1;
        if (interruptNext) {
            interruptNext = false;
            timerListener.onTimerUnderflow();
        }
        // Update timer...
        getTimer(currentCpuCycles);
        // Timer state machine!
        switch (state) {
            case STOP:
                // Nothing...
                break;
            case WAIT:
                // Go to count next time!
                state = COUNT;
                break;
            case LOAD_STOP:
                loadTimer(currentCpuCycles);
                // Stop timer!
                state = STOP;
                break;
            case LOAD_COUNT:
                loadTimer(currentCpuCycles);
                state = COUNT;
                break;
            case LOAD_WAIT_COUNT:
                if (nextZero == currentCpuCycles + 1) {
                    triggerInterrupt(currentCpuCycles);
                }
                state = WAIT;
                loadTimer(currentCpuCycles);
                break;
            case COUNT_STOP:
                if (!countUnderflow) {
                    timer = (int) (currentCpuCycles - nextZero);
                    if (timer < 0)
                        timer = 0;
                }
                state = STOP;
                break;
            case COUNT:
                // perform count - this assumes that we are counting
                // cycles!!!
                if (countUnderflow) {
                    if (otherTimer.underflow)
                        timer--;
                    if (timer <= 0) {
                        triggerInterrupt(currentCpuCycles);
                    }
                } else {
                    // TODO: check this!!!
                    if (currentCpuCycles >= nextZero && state != STOP) {
                        state = LOAD_COUNT;
                        triggerInterrupt(currentCpuCycles);
                    } else {
                        // We got here too early... what now?
                        nextUpdate = nextZero;
                    }
                }
                break;
        }

        // Delayed write of CR - is that only for timers?
        if (writeCR != -1) {
            delayedWrite(currentCpuCycles);
            writeCR = -1;
        }
    }

    void delayedWrite(long currentCpuCycles) {
        nextUpdate = currentCpuCycles + 1;
        switch (state) {
            case STOP:
            case LOAD_STOP:
                if ((writeCR & 1) > 0) {
                    // Start timer
                    if ((writeCR & 0x10) > 0) {
                        // force load
                        state = LOAD_WAIT_COUNT;
                    } else {
                        // Just count!
                        loadTimer(currentCpuCycles);
                        state = WAIT;
                    }
                } else {
                    if ((writeCR & 0x10) > 0) {
                        state = LOAD_STOP;
                    }
                }
                break;
            case COUNT:
                if ((writeCR & 1) > 0) {
                    if ((writeCR & 0x10) > 0) {
                        // force load
                        state = LOAD_WAIT_COUNT;
                    } // Otherwise just continue counting!
                } else {
                    if ((writeCR & 0x10) > 0) {
                        state = LOAD_STOP;
                    } else {
                        state = COUNT_STOP;
                    }
                }
                break;
            case LOAD_COUNT:
            case WAIT:
                if ((writeCR & 1) > 0) {
                    // Start timer
                    if ((writeCR & 8) > 0) {
                        // one-shot
                        writeCR = writeCR & 0xfe;
                        state = STOP;
                    } else if ((writeCR & 0x10) > 0) {
                        state = LOAD_WAIT_COUNT;
                    } // Otherwise just continue counting!
                } else {
                    state = COUNT_STOP;
                }
                break;
        }
        cr = writeCR & 0xef;
    }
}