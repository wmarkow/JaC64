package com.dreamfabric.jac64.emu.bus;

public interface AddressableIf {

    public boolean write(int address, int data, long currentCpuCycles);

    public Integer read(int address, long currentCpuCycles);

    public int getStartAddress();

    public int getEndAddress();

    public void setEnabled(boolean enabled);

    public boolean isEnabled();

    public default boolean isWritable() {
        return true;
    }
}
