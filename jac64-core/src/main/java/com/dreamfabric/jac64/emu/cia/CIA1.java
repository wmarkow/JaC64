package com.dreamfabric.jac64.emu.cia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cia.keyboard.Joy1KeyListener;
import com.dreamfabric.jac64.emu.cia.keyboard.Joy2KeyListener;
import com.dreamfabric.jac64.emu.cia.keyboard.KeyListener;
import com.dreamfabric.jac64.emu.cia.keyboard.Keyboard;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;

public class CIA1 extends CIA {
    private static Logger LOGGER = LoggerFactory.getLogger(CIA1.class);

    public final static int START_ADDRESS = 0xDC00;
    public final static int END_ADDRESS = 0xDCFF;

    private Keyboard keyboard;

    public CIA1(EventQueue scheduler, InterruptManager interruptManager) {
        super(scheduler, START_ADDRESS, interruptManager);

        write0(DDRA + START_ADDRESS, 0x00); // input
        write0(PRA + START_ADDRESS, 0xFF); // HIGH
        write0(DDRB + START_ADDRESS, 0x00); // input
        write0(PRB + START_ADDRESS, 0xFF); // HIGH

        keyboard = new Keyboard();
    }

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
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
                keyboard.setPRA(data);
                break;
            case PRB:
                keyboard.setPRB(data);
                break;
            case DDRA:
                keyboard.setDDRA(data);
                break;
            case DDRB:
                keyboard.setDDRB(data);
                break;
            default:
                // already handled by super
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
                return keyboard.getPRAPin();
            case PRB:
                return keyboard.getPRBPin();
            case DDRA:
                return result;
            case DDRB:
                return result;
            default:
                // already handled by super
        }

        return result;
    }

    public KeyListener getKeyListener() {
        return keyboard;
    }

    public Joy1KeyListener getJoy1KeyListener() {
        return keyboard;
    }

    public Joy2KeyListener getJoy2KeyListener() {
        return keyboard;
    }

    @Override
    protected void updateInterrupts() {
        if ((ciaie & ciaicrRead & 0x1f) != 0) {
            ciaicrRead |= 0x80;
            // Trigger the IRQ immediately!!!

            // cpu.log("CIA 1 *** TRIGGERING CIA TIMER!!!: " +
            // ciaie + " " + chips.getIRQFlags() + " " + cpu.getIRQLow());
            interruptManager.setIRQ(InterruptManager.CIA_TIMER_IRQ);
        } else {
            // System.out.println("*** CLEARING CIA TIMER!!!");
            interruptManager.clearIRQ(InterruptManager.CIA_TIMER_IRQ);
        }
    }
}
