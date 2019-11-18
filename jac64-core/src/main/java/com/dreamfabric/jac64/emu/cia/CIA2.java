package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.bus.ControlBus;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;

public class CIA2 extends CIA {

    public final static int START_ADDRESS = 0xDD00;
    public final static int END_ADDRESS = 0xDDFF;

    public CIA2(ControlBus controlBus) {
        super(START_ADDRESS, controlBus);
    }

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }

    public int getPRA() {
        return read0(PRA + getStartAddress());
    }

    @Override
    protected void updateInterrupts() {
        if ((ciaie & ciaicrRead & 0x1f) != 0) {
            ciaicrRead |= 0x80;
            // Trigger the NMI immediately!!!
            controlBus.setNMI(InterruptManager.CIA_TIMER_NMI);
        } else {
            controlBus.clearNMI(InterruptManager.CIA_TIMER_NMI);
        }
    }
}
