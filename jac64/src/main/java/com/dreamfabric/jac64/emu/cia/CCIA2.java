package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.EventQueue;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.vic.C64Screen;

public class CCIA2 extends CIA {

    public CCIA2(EventQueue scheduler, InterruptManager interruptManager) {
        super(scheduler, C64Screen.IO_OFFSET + 0xdd00, interruptManager);
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
