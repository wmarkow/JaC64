package com.dreamfabric.jac64.emu.interrupt;

import java.util.Hashtable;

import com.dreamfabric.jac64.emu.cpu.MOS6510Core;

public class InterruptManager {
    public static final boolean DEBUG_INTERRUPS = false;

    // C64 specific names - but... basically just numbers
    public static final int VIC_IRQ = 1;
    public static final int CIA_TIMER_IRQ = 2;
    public static final int KEYBOARD_NMI = 1;
    public static final int CIA_TIMER_NMI = 2;

    // One InterruptManager per named CPU.
    private static Hashtable<String, InterruptManager> managers = new Hashtable<String, InterruptManager>();

    private int nmiFlags;
    private int irqFlags;
    private int oldIrqFlags;
    private int oldNmiFlags;
    private MOS6510Core cpu;

    public InterruptManager(MOS6510Core cpu) {
        this.cpu = cpu;
    }

    public static InterruptManager getInterruptManager(MOS6510Core cpu) {
        if (!managers.contains(cpu.getName())) {
            managers.put(cpu.getName(), new InterruptManager(cpu));
        }

        return managers.get(cpu.getName());
    }
    
    public void reset() {
        nmiFlags = 0;
        irqFlags = 0;
        oldIrqFlags = 0;
        oldNmiFlags = 0;
        cpu.setIRQLow(false);
        cpu.setNMILow(false);
        cpu.log("ExtChip: Resetting IRQ flags!");
    }

    public boolean setIRQ(int irq) {
        boolean val = (irqFlags & irq) == 0;
        irqFlags |= irq;
        if (irqFlags != oldIrqFlags) {
            if (DEBUG_INTERRUPS && irqFlags != 0 && cpu.isDebug()) {
                cpu.log("ExtChips: Setting IRQ! " + irq + " => " + irqFlags + " at " + cpu.cycles);
            }
            cpu.setIRQLow(irqFlags != 0);
            oldIrqFlags = irqFlags;
        }
        return val;
    }

    public void clearIRQ(int irq) {
        irqFlags &= ~irq;
        if (irqFlags != oldIrqFlags) {
            if (DEBUG_INTERRUPS && oldIrqFlags != 0 && cpu.isDebug()) {
                System.out.println("Clearing IRQ! " + irq + " => " + irqFlags + " at " + cpu.cycles);
            }
            cpu.setIRQLow(irqFlags != 0);
            oldIrqFlags = irqFlags;
        }
    }

    public boolean setNMI(int nmi) {
        boolean val = (nmiFlags & nmi) == 0;
        nmiFlags |= nmi;
        if (nmiFlags != oldNmiFlags) {
            if (DEBUG_INTERRUPS && cpu.isDebug())
                System.out.println("Setting NMI! " + nmi + " => " + nmiFlags + " at " + cpu.cycles);
            cpu.setNMILow(nmiFlags != 0);
            oldNmiFlags = nmiFlags;
        }
        return val;
    }

    public void clearNMI(int nmi) {
        nmiFlags &= ~nmi;
        if (nmiFlags != oldNmiFlags) {
            if (DEBUG_INTERRUPS && oldNmiFlags != 0 && cpu.isDebug()) {
                System.out.println("Clearing NMI! " + nmi + " => " + nmiFlags + " at " + cpu.cycles);
            }
            cpu.setNMILow(nmiFlags != 0);
            oldNmiFlags = nmiFlags;
        }
    }

    public int getNmiFlags() {
        return this.nmiFlags;
    }

    public int getIrqFlags() {
        return this.irqFlags;
    }
}
