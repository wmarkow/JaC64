package com.dreamfabric.jac64.emu.memory;

public interface BasicROMIf extends ROMIf {

    public final static int START_ADDRESS = 0xA000;
    public final static int END_ADDRESS = 0xBFFF;

    @Override
    public default int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public default int getEndAddress() {
        return END_ADDRESS;
    }

    public default boolean isWritable() {
        return false;
    }
}
