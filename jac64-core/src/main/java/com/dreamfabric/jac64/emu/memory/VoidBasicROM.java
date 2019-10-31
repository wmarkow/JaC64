package com.dreamfabric.jac64.emu.memory;

public class VoidBasicROM extends VoidMemory {

    public final static int START_ADDRESS = 0xA000;
    public final static int END_ADDRESS = 0xBFFF;

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }
}
