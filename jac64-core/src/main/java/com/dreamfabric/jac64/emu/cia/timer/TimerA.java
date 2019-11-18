package com.dreamfabric.jac64.emu.cia.timer;

import com.dreamfabric.jac64.emu.bus.ControlBus;

public class TimerA extends Timer {

    public TimerA(String id, boolean uo, Timer other, ControlBus controlBus) {
        super(id, uo, other, controlBus);
    }

    public void setCR(long cycles, int data) {
        super.setCR(cycles, data);

        setCountCycles((data & 0x20) == 0);
    }
}
