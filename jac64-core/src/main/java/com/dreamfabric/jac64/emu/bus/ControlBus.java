package com.dreamfabric.jac64.emu.bus;

import com.dreamfabric.jac64.emu.pla.PLA;

public class ControlBus {

    private PLA pla;

    public ControlBus(PLA pla) {
        this.pla = pla;
    }

    public void setCharenHiramLoram(int byteValue) {
        pla.setCharenHiramLoram(byteValue);
    }
}
