package com.dreamfabric.jac64.emu.cia.timer;

import com.dreamfabric.jac64.emu.bus.ControlBus;

public class TimerB extends Timer {

    public TimerB(String id, boolean uo, Timer other, ControlBus controlBus) {
        super(id, uo, other, controlBus);
    }

    public void setCR(long cycles, int data) {
        super.setCR(cycles, data);

        setCountCycles((data & 0x60) == 0);
        setCountUnderflow((data & 0x60) == 0x40);
    }
}
