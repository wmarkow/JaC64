/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64.emu.chip;

import com.dreamfabric.jac64.Observer;
import com.dreamfabric.jac64.emu.cpu.MOS6510Core;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;

/**
 * ExtChip - used for implementing HW Chips connected to the CPU. handles
 * IRQs/NMIs for all the implemented CPU/IO chips and defines some APIs that is
 * called by CPU
 *
 *
 * Created: Tue Aug 02 08:58:12 2005
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public abstract class ExtChip {

    private Observer observer;

    public void init() {
    }

    public abstract void reset();

    public abstract void stop();

    public abstract int performRead(int address, long cycles);

    public abstract void performWrite(int address, int data, long cycles);

    public abstract void clock(long cycles);

    public void setObserver(Observer o) {
        observer = o;
    }

    public void update(Object source, Object data) {
        if (observer != null) {
            observer.update(source, data);
        }
    }
}