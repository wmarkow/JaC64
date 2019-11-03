package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.EventQueue;
import com.dreamfabric.jac64.emu.TimeEvent;

//A timer class for handling the Timer state machines
// The CIATimer is inspired by the CIA Timer State Machine in the
// Frodo emulator
public class CIATimer {

    public static final boolean TIMER_DEBUG = false; // true;

    // The states of the timer
    static final int STOP = 0;
    private static final int WAIT = 1;
    private static final int LOAD_STOP = 2;
    private static final int LOAD_COUNT = 3;
    private static final int LOAD_WAIT_COUNT = 5;
    private static final int COUNT = 6;
    private static final int COUNT_STOP = 7;

    // If timer is connected...
    CIATimer otherTimer;

    int state = STOP;
    // The latch for this timer
    int latch = 0;
    int timer = 0;

    // When is an update needed for this timer?
    long nextUpdate = 0;
    long nextZero = 0;
    long lastLatch = 0;

    boolean interruptNext = false;
    boolean underflow = false;
    boolean flipflop = false;

    boolean countCycles = false;
    boolean countUnderflow = false;

    private EventQueue scheduler;
    private CIA cia;

    TimeEvent updateEvent = new TimeEvent(0) {
        public void execute(long cycles) {
            doUpdate(cycles);
            if (state != STOP) {
                // System.out.println(ciaID() + " Adding Timer update event at " + cycles +
                // " with time: " + nextUpdate + " state: " + state);
                scheduler.addEvent(this, nextUpdate);
            }
        }
    };

    int writeCR = -1;
    int cr = 0;
    String id;
    int iflag;
    boolean updateOther;

    CIATimer(String id, int flag, boolean uo, CIATimer other, EventQueue scheduler, CIA cia) {
        this.id = id;
        this.otherTimer = other;
        this.iflag = flag;
        updateOther = uo;
        this.scheduler = scheduler;
        this.cia = cia;
    }

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

    private void loadTimer(long cycles) {
        timer = latch;
        nextZero = cycles + latch;

        if (TIMER_DEBUG && cia.offset == 0x10d00) {
            System.out.println(cia.ciaID() + ": " + id + " - timer loaded at " + cycles + " with: " + latch + " diff "
                    + (cycles - lastLatch));
            lastLatch = cycles;
        }
    }

    private void triggerInterrupt(long cycles) {
        if (TIMER_DEBUG && cia.offset == 0x10d00)
            System.out.println(cia.ciaID() + ": " + id + " - trigger interrupt at: " + cycles + " nextZero: " + nextZero
                    + " nextUpdate: " + nextUpdate);
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

    void writeCR(long cycles, int data) {
        writeCR = data;
        if (nextUpdate > cycles + 1 || !updateEvent.isScheduled()) {
            nextUpdate = cycles + 1;
            scheduler.addEvent(updateEvent, nextUpdate);
        }
    }

    public void doUpdate(long cycles) {
        if (nextUpdate == 0) {
            nextUpdate = cycles;
            nextZero = cycles;
        }
        if (cycles == nextUpdate) {
            // If the call is on "spot" then just do it!
            update(cycles);
        } else {
            // As long as cycles is larger than next update, just continue
            // call it!
            while (cycles >= nextUpdate) {
                System.out.println(cia.ciaID() + ": " + id + " ** update at: " + cycles + " expected: " + nextUpdate
                        + " state: " + state);
                update(nextUpdate);
            }
        }
    }

    // maybe only update when "needed" and when registers read???
    // NEED TO CHECK IF WE ARE CALLED WHEN EXPECTED!!!
    // No - since BA Low will cause no updates of IO units..??
    public void update(long cycles) {
        // set a default
        underflow = false;
        nextUpdate = cycles + 1;
        if (interruptNext) {
            cia.ciaicrRead |= iflag;
            interruptNext = false;
            // Trigg the stuff...
            cia.updateInterrupts();
        }
        // Update timer...
        getTimer(cycles);
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
                loadTimer(cycles);
                // Stop timer!
                state = STOP;
                break;
            case LOAD_COUNT:
                loadTimer(cycles);
                state = COUNT;
                break;
            case LOAD_WAIT_COUNT:
                if (nextZero == cycles + 1) {
                    triggerInterrupt(cycles);
                }
                state = WAIT;
                loadTimer(cycles);
                break;
            case COUNT_STOP:
                if (!countUnderflow) {
                    timer = (int) (cycles - nextZero);
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
                        triggerInterrupt(cycles);
                    }
                } else {
                    // TODO: check this!!!
                    if (cycles >= nextZero && state != STOP) {
                        state = LOAD_COUNT;
                        triggerInterrupt(cycles);
                    } else {
                        // We got here too early... what now?
                        nextUpdate = nextZero;
                    }
                }
                break;
        }

        // Delayed write of CR - is that only for timers?
        if (writeCR != -1) {
            delayedWrite(cycles);
            writeCR = -1;
        }
    }

    void delayedWrite(long cycles) {
        nextUpdate = cycles + 1;
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
                        loadTimer(cycles);
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