package com.dreamfabric.jac64.emu.cia.timer;

import com.dreamfabric.jac64.emu.scheduler.EventQueue;

public class TimerA extends Timer {

    public TimerA(String id, boolean uo, Timer other, EventQueue scheduler) {
        super(id, uo, other, scheduler);
    }

    public void setCR(long cycles, int data) {
        super.setCR(cycles, data);

        setCountCycles((data & 0x20) == 0);
    }
}
