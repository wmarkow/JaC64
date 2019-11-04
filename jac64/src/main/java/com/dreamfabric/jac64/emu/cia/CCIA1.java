package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.EventQueue;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.vic.C64Screen;

public class CCIA1 extends CIA {

    public CCIA1(EventQueue scheduler, InterruptManager interruptManager) {
        super(scheduler, C64Screen.IO_OFFSET + 0xdc00, interruptManager);
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
