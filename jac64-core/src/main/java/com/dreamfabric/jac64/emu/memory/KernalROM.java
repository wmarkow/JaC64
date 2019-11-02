package com.dreamfabric.jac64.emu.memory;

public class KernalROM extends ROM {
    public final static int START_ADDRESS = 0xE000;
    public final static int END_ADDRESS = 0xFFFF;

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }
}
