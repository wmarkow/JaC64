package com.dreamfabric.jac64.emu.memory;

public class CharROM extends ROM {
    public final static int START_ADDRESS = 0xD000;
    public final static int END_ADDRESS = 0xDFFF;

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }
}
