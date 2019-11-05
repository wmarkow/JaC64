package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;

public class CIA2 extends CIA {

    public final static int START_ADDRESS = 0xDD00;
    public final static int END_ADDRESS = 0xDDFF;

    public CIA2(EventQueue scheduler, InterruptManager interruptManager) {
        super(scheduler, START_ADDRESS, interruptManager);
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
    protected void updateInterrupts() {
        if ((ciaie & ciaicrRead & 0x1f) != 0) {
            ciaicrRead |= 0x80;
            // Trigger the NMI immediately!!!
            interruptManager.setNMI(InterruptManager.CIA_TIMER_NMI);
        } else {
            interruptManager.clearNMI(InterruptManager.CIA_TIMER_NMI);
        }
    }
}