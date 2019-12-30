package com.dreamfabric.jac64.emu.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cia.CIA1;
import com.dreamfabric.jac64.emu.cia.keyboard.Joy1Key;
import com.dreamfabric.jac64.emu.cia.keyboard.Joy1KeyListener;
import com.dreamfabric.jac64.emu.cia.keyboard.Joy2Key;
import com.dreamfabric.jac64.emu.cia.keyboard.Joy2KeyListener;
import com.dreamfabric.jac64.emu.cia.keyboard.Key;
import com.dreamfabric.jac64.emu.cia.keyboard.KeyListener;
import com.dreamfabric.jac64.emu.cpu.MOS6510Core;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.pla.PLA;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;
import com.dreamfabric.jac64.emu.scheduler.TimeEvent;
import com.dreamfabric.jac64.emu.vic.VICIf;

public class ControlBus implements KeyListener, Joy1KeyListener, Joy2KeyListener {
    private static Logger LOGGER = LoggerFactory.getLogger(ControlBus.class);

    private PLA pla;
    private InterruptManager interruptManager;
    private MOS6510Core cpu;
    private EventQueue scheduler;
    private VICIf vic;
    private CIA1 cia1;

    public ControlBus(PLA pla, InterruptManager interruptManager, MOS6510Core cpu, EventQueue scheduler, VICIf vic) {
        this.pla = pla;
        this.interruptManager = interruptManager;
        this.cpu = cpu;
        this.scheduler = scheduler;
        this.vic = vic;
    }

    public void setCIA1(CIA1 cia1) {
        this.cia1 = cia1;
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

    public void executeFromEventQueue(long currentCpuCycles) {
        while (currentCpuCycles >= scheduler.nextTime) {
            TimeEvent t = scheduler.popFirst();
            if (t != null) {
                LOGGER.debug("Executing event: " + t.getShort());
                // Give it the actual time also!!!
                t.execute(currentCpuCycles);
            } else {
                LOGGER.debug("Nothing to execute...");
                return;
            }
        }
    }

    public void addEvent(TimeEvent event, long cpuCyclesTime) {
        scheduler.addEvent(event, cpuCyclesTime);
    }

    public boolean removeEvent(TimeEvent event) {
        return scheduler.removeEvent(event);
    }

    public void clock(long currentCpuCycles) {
        vic.clock(currentCpuCycles);
    }

    @Override
    public void joy2KeyPressed(Joy2Key key) {
        cia1.getJoy2KeyListener().joy2KeyPressed(key);
    }

    @Override
    public void joy2KeyReleased(Joy2Key key) {
        cia1.getJoy2KeyListener().joy2KeyReleased(key);
    }

    @Override
    public void joy1KeyPressed(Joy1Key key) {
        cia1.getJoy1KeyListener().joy1KeyPressed(key);
    }

    @Override
    public void joy1KeyReleased(Joy1Key key) {
        cia1.getJoy1KeyListener().joy1KeyReleased(key);
    }

    @Override
    public void keyPressed(Key key) {
        cia1.getKeyListener().keyPressed(key);
    }

    @Override
    public void keyReleased(Key key) {
        cia1.getKeyListener().keyReleased(key);
    }
}
