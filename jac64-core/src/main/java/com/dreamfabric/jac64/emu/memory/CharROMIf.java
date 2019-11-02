package com.dreamfabric.jac64.emu.memory;

public interface CharROMIf extends ROMIf {
    public final static int START_ADDRESS = 0xD000;
    public final static int END_ADDRESS = 0xDFFF;

    @Override
    public default int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public default int getEndAddress() {
        return END_ADDRESS;
    }
}
