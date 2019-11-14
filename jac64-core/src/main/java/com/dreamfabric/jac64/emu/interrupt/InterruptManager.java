package com.dreamfabric.jac64.emu.interrupt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cpu.MOS6510Core;

public class InterruptManager {
    private static Logger LOGGER = LoggerFactory.getLogger(InterruptManager.class);

    // C64 specific names - but... basically just numbers
    public static final int VIC_IRQ = 1;
    public static final int CIA_TIMER_IRQ = 2;
    public static final int KEYBOARD_NMI = 1;
    public static final int CIA_TIMER_NMI = 2;

    private int nmiFlags;
    private int irqFlags;
    private int oldIrqFlags;
    private int oldNmiFlags;
    private MOS6510Core cpu;

    public InterruptManager(MOS6510Core cpu) {
        this.cpu = cpu;
    }

    public void reset() {
        nmiFlags = 0;
        irqFlags = 0;
        oldIrqFlags = 0;
        oldNmiFlags = 0;
        cpu.setIRQLow(false);
        cpu.setNMILow(false);
        LOGGER.info("ExtChip: Resetting IRQ flags!");
    }

    public boolean setIRQ(int irq) {
        boolean val = (irqFlags & irq) == 0;
        irqFlags |= irq;
        if (irqFlags != oldIrqFlags) {
            if (irqFlags != 0) {
                LOGGER.debug("ExtChips: Setting IRQ! " + irq + " => " + irqFlags + " at " + cpu.currentCpuCycles);
            }
            cpu.setIRQLow(irqFlags != 0);
            oldIrqFlags = irqFlags;
        }
        return val;
    }

    public void clearIRQ(int irq) {
        irqFlags &= ~irq;
        if (irqFlags != oldIrqFlags) {
            if (oldIrqFlags != 0) {
                LOGGER.debug("Clearing IRQ! " + irq + " => " + irqFlags + " at " + cpu.currentCpuCycles);
            }
            cpu.setIRQLow(irqFlags != 0);
            oldIrqFlags = irqFlags;
        }
    }

    public boolean setNMI(int nmi) {
        boolean val = (nmiFlags & nmi) == 0;
        nmiFlags |= nmi;
        if (nmiFlags != oldNmiFlags) {
            LOGGER.info("Setting NMI! " + nmi + " => " + nmiFlags + " at " + cpu.currentCpuCycles);
            cpu.setNMILow(nmiFlags != 0);
            oldNmiFlags = nmiFlags;
        }
        return val;
    }

    public void clearNMI(int nmi) {
        nmiFlags &= ~nmi;
        if (nmiFlags != oldNmiFlags) {
            if (oldNmiFlags != 0) {
                LOGGER.debug("Clearing NMI! " + nmi + " => " + nmiFlags + " at " + cpu.currentCpuCycles);
            }
            cpu.setNMILow(nmiFlags != 0);
            oldNmiFlags = nmiFlags;
        }
    }

    public int getNMIFlags() {
        return this.nmiFlags;
    }

    public int getIRQFlags() {
        return this.irqFlags;
    }
}
