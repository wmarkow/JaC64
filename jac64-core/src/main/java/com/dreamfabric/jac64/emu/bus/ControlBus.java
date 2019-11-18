package com.dreamfabric.jac64.emu.bus;

import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.pla.PLA;

public class ControlBus {

    private PLA pla;
    private InterruptManager interruptManager;

    public ControlBus(PLA pla, InterruptManager interruptManager) {
        this.pla = pla;
        this.interruptManager = interruptManager;
    }

    public void setCharenHiramLoram(int byteValue) {
        pla.setCharenHiramLoram(byteValue);
    }

    public boolean setNMI(int nmi) {
        return interruptManager.setNMI(nmi);
    }

    public void clearNMI(int nmi) {
        interruptManager.clearNMI(nmi);
    }

    public boolean setIRQ(int irq) {
        return interruptManager.setIRQ(irq);
    }

    public void clearIRQ(int irq) {
        interruptManager.clearIRQ(irq);
    }
}
