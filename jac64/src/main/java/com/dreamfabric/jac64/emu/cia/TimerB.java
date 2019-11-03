package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.EventQueue;

public class TimerB extends Timer {

    public TimerB(String id, int flag, boolean uo, Timer other, EventQueue scheduler, CIA cia) {
        super(id, flag, uo, other, scheduler, cia);
    }

    public void setCR(long cycles, int data) {
        super.setCR(cycles, data);

        setCountCycles((data & 0x60) == 0);
        setCountUnderflow((data & 0x60) == 0x40);
    }
}
