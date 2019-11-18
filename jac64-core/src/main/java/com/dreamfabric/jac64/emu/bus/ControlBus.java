package com.dreamfabric.jac64.emu.bus;

import com.dreamfabric.jac64.emu.cpu.MOS6510Core;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.pla.PLA;

public class ControlBus {

    private PLA pla;
    private InterruptManager interruptManager;
    private MOS6510Core cpu;

    public ControlBus(PLA pla, InterruptManager interruptManager, MOS6510Core cpu) {
        this.pla = pla;
        this.interruptManager = interruptManager;
        this.cpu = cpu;
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

    public void setCpuBALowUntil(long cpuCycles) {
        cpu.baLowUntil = cpuCycles;
    }
}
