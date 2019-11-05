/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */
package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.SimulableIf;
import com.dreamfabric.jac64.emu.bus.AddressableChip;
import com.dreamfabric.jac64.emu.cia.keyboard.Keyboard;
import com.dreamfabric.jac64.emu.cia.timer.RealTimeClock;
import com.dreamfabric.jac64.emu.cia.timer.Timer;
import com.dreamfabric.jac64.emu.cia.timer.TimerA;
import com.dreamfabric.jac64.emu.cia.timer.TimerB;
import com.dreamfabric.jac64.emu.cia.timer.TimerListenerIf;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;

/**
 * CIA emulation for JaC64.
 *
 * Created: Sat Jul 30 05:38:32 2005
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public abstract class CIA extends AddressableChip implements SimulableIf {
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

    private final static int INTERRUPT_ORIGIN_TIMER_A_UNDERFLOW_VALUE = 1;
    private final static int INTERRUPT_ORIGIN_TIMER_B_UNDERFLOW_VALUE = 2;

    private Timer timerA;
    private Timer timerB;
    private RealTimeClock rtc;

    int pra = 0;
    int prb = 0;
    int ddra = 0;
    int ddrb = 0;
    int sdr;

    // For the CPU to read (contains status)
    protected int ciaicrRead;
    int ciaie = 0; // interrupt enable

    // B-Div is set if mode (bit 5,6 of CIACRB) = 10
    public static final int TIMER_B_DIV_MASK = 0x60;
    public static final int TIMER_B_DIV_VAL = 0x40;

    public long nextCIAUpdate = 0;

    private int startAddress;

    public int serialFake = 0;
    protected InterruptManager interruptManager;

    /**
     * Creates a new <code>CIA</code> instance.
     *
     */
    public CIA(EventQueue scheduler, int startAddress, InterruptManager interruptManager) {
        super();
        this.startAddress = startAddress;

        this.interruptManager = interruptManager;
        timerA = new TimerA("TimerA", true, null, scheduler);
        timerB = new TimerB("TimerB", false, timerA, scheduler);
        timerA.setOtherTimer(timerB);
        rtc = new RealTimeClock(scheduler);

        timerA.setTimerListener(new TimerListenerIf() {

            @Override
            public void onTimerUnderflow() {
                ciaicrRead |= INTERRUPT_ORIGIN_TIMER_A_UNDERFLOW_VALUE;
                updateInterrupts();
            }
        });

        timerB.setTimerListener(new TimerListenerIf() {

            @Override
            public void onTimerUnderflow() {
                ciaicrRead |= INTERRUPT_ORIGIN_TIMER_B_UNDERFLOW_VALUE;
                updateInterrupts();
            }
        });
    }

    @Override
    public void start(long currentCpuCycles) {
        rtc.start(currentCpuCycles);
        timerA.start(currentCpuCycles);
        timerB.start(currentCpuCycles);
    }

    @Override
    public void stop() {
    }

    @Override
    public void reset() {
        ciaicrRead = 0;
        ciaie = 0;

        timerA.reset();
        timerB.reset();

        updateInterrupts();
    }

    private String ciaID() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean write(int address, int data, long currentCpuCycles) {
        boolean result = super.write(address, data, currentCpuCycles);

        if (result == false) {
            return false;
        }

        int localAddress = address - getStartAddress();
        switch (localAddress) {
            case PRA:
                break;
            case PRB:
                break;
            case DDRA:
                break;
            case DDRB:
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
                timerA.setCR(currentCpuCycles, data);
                break;
            case CRB:
                timerB.setCR(currentCpuCycles, data);
                break;
        }

        return true;
    }

    @Override
    public Integer read(int address, long currentCpuCycles) {
        Integer result = super.read(address, currentCpuCycles);
        if (result == null) {
            return null;
        }

        int localAddress = address - getStartAddress();
        switch (localAddress) {
            case PRA:
                return result;
            case PRB:
                return result;
            case DDRA:
                return result;
            case DDRB:
                return result;
            case TIMALO:
                return timerA.getTimerLowByteValue(currentCpuCycles);
            case TIMAHI:
                return timerA.getTimerHighByteValue(currentCpuCycles);
            case TIMBLO:
                return timerB.getTimerLowByteValue(currentCpuCycles);
            case TIMBHI:
                return timerB.getTimerHighByteValue(currentCpuCycles);
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
                int val = ciaicrRead;
                ciaicrRead = 0;

                // Latch off the IRQ/NMI immediately!!!
                updateInterrupts();

                return val;
            default:
                return 0xff;
        }
    }

    protected abstract void updateInterrupts();

    private void println(String s) {
        System.out.println(ciaID() + ": " + s);
    }

    public void printStatus() {
        // System.out.println("--------------------------");
        // println(" status");
        // System.out.println("Timer A state: " + timerA.state);
        // System.out.println("Timer A next trigger: " + timerA.nextZero);
        // System.out.println("CIA CRA: " + Hex.hex2(timerA.getCR()) + " => "
        // + (((timerA.getCR() & 0x08) == 0) ? "cont" : "one-shot"));
        // // System.out.println("Timer A Interrupt: " + timerATrigg);
        // System.out.println("Timer A Latch: " + timerA.getLatch());
        // System.out.println("Timer B state: " + timerB.state);
        // System.out.println("Timer B next trigger: " + timerB.nextZero);
        // System.out.println("CIA CRB: " + Hex.hex2(timerA.getCR()) + " => "
        // + (((timerB.getCR() & 0x08) == 0) ? "cont" : "one-shot"));
        // // System.out.println("Timer B Interrupt: " + timerBTrigg);
        // System.out.println("Timer B Latch: " + timerB.getLatch());
        // System.out.println("--------------------------");
    }
}
