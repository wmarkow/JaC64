package com.dreamfabric.jac64.emu.memory;

public interface KernalROMIf extends ROMIf {
    public final static int START_ADDRESS = 0xE000;
    public final static int END_ADDRESS = 0xFFFF;

    @Override
    public default int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public default int getEndAddress() {
        return END_ADDRESS;
    }
}
