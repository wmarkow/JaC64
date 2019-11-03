/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */
package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.Hex;
import com.dreamfabric.jac64.emu.chip.ExtChip;
import com.dreamfabric.jac64.emu.cpu.MOS6510Core;

/**
 * CIA emulation for JaC64.
 *
 * Created: Sat Jul 30 05:38:32 2005
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public class CIA {
    public static final boolean WRITE_DEBUG = false; // true;

    public static final int PRA = 0x00;
    public static final int PRB = 0x01;
    public static final int DDRA = 0x02;
    public static final int DDRB = 0x03;
    public static final int TIMALO = 0x04;
    public static final int TIMAHI = 0x05;
    public static final int TIMBLO = 0x06;
    public static final int TIMBHI = 0x07;
    public static final int TODTEN = 0x08;
    public static final int TODSEC = 0x09;
    public static final int TODMIN = 0x0a;
    public static final int TODHRS = 0x0b;
    public static final int SDR = 0x0c;
    public static final int ICR = 0x0d;
    public static final int CRA = 0x0e;
    public static final int CRB = 0x0f;

    private CIATimer timerA;
    private CIATimer timerB;
    private RealTimeClock rtc;

    int pra = 0;
    int prb = 0;
    int ddra = 0;
    int ddrb = 0;
    int sdr;

    // For the CPU to read (contains status)
    int ciaicrRead;
    int ciaie = 0; // interrupt enable

    // B-Div is set if mode (bit 5,6 of CIACRB) = 10
    public static final int TIMER_B_DIV_MASK = 0x60;
    public static final int TIMER_B_DIV_VAL = 0x40;

    public long nextCIAUpdate = 0;

    int offset;

    public int serialFake = 0;
    private ExtChip chips;

    /**
     * Creates a new <code>CIA</code> instance.
     *
     */
    public CIA(MOS6510Core cpu, int offset, ExtChip chips) {
        this.offset = offset;
        this.chips = chips;
        timerA = new CIATimer("TimerA", 1, true, null, cpu.getScheduler(), this);
        timerB = new CIATimer("TimerB", 2, false, timerA, cpu.getScheduler(), this);
        timerA.otherTimer = timerB;
        rtc = new RealTimeClock(cpu.getScheduler(), cpu.cycles);
    }

    public void reset() {
        ciaicrRead = 0;
        ciaie = 0;

        timerA.reset();
        timerB.reset();

        updateInterrupts();
    }

    public String ciaID() {
        return offset == 0x10c00 ? "CIA 1" : "CIA 2";
    }

    public int performRead(int address, long cycles) {
        address -= offset;
        switch (address) {
            case DDRA:
                return ddra;
            case DDRB:
                return ddrb;
            case PRA:
                return (pra | ~ddra) & 0xff;
            case PRB:
                int data = (prb | ~ddrb) & 0xff;
                if ((timerA.getCR() & 0x02) > 0) {
                    data &= 0xbf;
                    if ((timerA.getCR() & 0x04) > 0) {
                        data |= timerA.flipflop ? 0x40 : 0;
                    } else {
                        data |= timerA.underflow ? 0x40 : 0;
                    }
                }
                if ((timerB.getCR() & 0x02) > 0) {
                    data &= 0x7f;
                    if ((timerB.getCR() & 0x04) > 0) {
                        data |= timerB.flipflop ? 0x80 : 0;
                    } else {
                        data |= timerB.underflow ? 0x80 : 0;
                    }
                }
                return data;
            case TIMALO:
                return timerA.getTimerLowByteValue(cycles);
            case TIMAHI:
                return timerA.getTimerHighByteValue(cycles);
            case TIMBLO:
                return timerB.getTimerLowByteValue(cycles);
            case TIMBHI:
                return timerB.getTimerHighByteValue(cycles);
            case TODTEN:
                return rtc.getTod10sec();
            case TODSEC:
                return rtc.getTodsec();
            case TODMIN:
                return rtc.getTodmin();
            case TODHRS:
                return rtc.getTodhour();
            case SDR:
                return sdr;
            case CRA:
                return timerA.getCR();
            case CRB:
                return timerB.getCR();
            case ICR:
                // Clear interrupt register (flags)!
                if (CIATimer.TIMER_DEBUG && offset == 0x10d00)
                    println("clear Interrupt register, was: " + Hex.hex2(ciaicrRead));
                int val = ciaicrRead;
                ciaicrRead = 0;

                // Latch off the IRQ/NMI immediately!!!
                updateInterrupts();

                return val;
            default:
                return 0xff;
        }
    }

    public void performWrite(int address, int data, long cycles) {
        address -= offset;

        if (WRITE_DEBUG)
            println(ciaID() + " Write to :" + Integer.toString(address, 16) + " = " + Integer.toString(data, 16));

        switch (address) {
            // monitor.println("Set Keyboard:" + data);
            case DDRA:
                ddra = data;
                break;
            case DDRB:
                ddrb = data;
                break;
            case PRA:
                pra = data;
                break;
            case PRB:
                prb = data;
                break;
            case TIMALO:
                // Update TimerA latch low byte value
                timerA.setLatchLowByte(data);
                break;
            case TIMAHI:
                // Update TimerA latch high byte value
                timerA.setLatchHighByte(data);
                break;
            case TIMBLO:
                // Update TimerB latch low byte value
                timerB.setLatchLowByte(data);
                break;
            case TIMBHI:
                // Update TimerA latch high byte value
                timerB.setLatchHighByte(data);
                break;
            case TODTEN:
                rtc.setTod10sec(data);
                break;
            case TODSEC:
                rtc.setTodsec(data);
                break;
            case TODMIN:
                rtc.setTodmin(data);
                break;
            case TODHRS:
                rtc.setTodhour(data);
                break;
            case ICR: // dc0d - CIAICR - CIA Interrupt Control Register
                boolean val = (data & 0x80) != 0;
                if (val) {
                    // Set the 1 bits if val = 1
                    ciaie |= data & 0x7f;
                } else {
                    // Clear the 1 bits
                    ciaie &= ~data;
                }
                // Trigger interrupts if needed...
                updateInterrupts();
                System.out.println(ciaID() + " ====> IE = " + ciaie);
                break;
            case CRA:
                timerA.setCR(cycles, data);
                timerA.setCountCycles((data & 0x20) == 0);
                break;

            case CRB:
                timerB.setCR(cycles, data);
                timerB.setCountCycles((data & 0x60) == 0);
                timerB.setCountUnderflow((data & 0x60) == 0x40);
                break;
        }
    }

    void updateInterrupts() {
        if ((ciaie & ciaicrRead & 0x1f) != 0) {
            ciaicrRead |= 0x80;
            // Trigger the IRQ/NMI immediately!!!
            if (offset == 0x10c00) {
                // cpu.log("CIA 1 *** TRIGGERING CIA TIMER!!!: " +
                // ciaie + " " + chips.getIRQFlags() + " " + cpu.getIRQLow());
                chips.setIRQ(ExtChip.CIA_TIMER_IRQ);
            } else {
                chips.setNMI(ExtChip.CIA_TIMER_NMI);
            }
        } else {
            if (offset == 0x10c00) {
                // System.out.println("*** CLEARING CIA TIMER!!!");
                chips.clearIRQ(ExtChip.CIA_TIMER_IRQ);
            } else {
                chips.clearNMI(ExtChip.CIA_TIMER_NMI);
            }
        }
    }

    private void println(String s) {
        System.out.println(ciaID() + ": " + s);
    }

    public void printStatus() {
        System.out.println("--------------------------");
        println(" status");
        System.out.println("Timer A state: " + timerA.state);
        System.out.println("Timer A next trigger: " + timerA.nextZero);
        System.out.println("CIA CRA: " + Hex.hex2(timerA.getCR()) + " => "
                + (((timerA.getCR() & 0x08) == 0) ? "cont" : "one-shot"));
        // System.out.println("Timer A Interrupt: " + timerATrigg);
        System.out.println("Timer A Latch: " + timerA.getLatch());
        System.out.println("Timer B state: " + timerB.state);
        System.out.println("Timer B next trigger: " + timerB.nextZero);
        System.out.println("CIA CRB: " + Hex.hex2(timerA.getCR()) + " => "
                + (((timerB.getCR() & 0x08) == 0) ? "cont" : "one-shot"));
        // System.out.println("Timer B Interrupt: " + timerBTrigg);
        System.out.println("Timer B Latch: " + timerB.getLatch());
        System.out.println("--------------------------");
    }
}
