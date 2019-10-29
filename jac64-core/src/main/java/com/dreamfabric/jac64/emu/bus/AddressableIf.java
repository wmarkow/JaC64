package com.dreamfabric.jac64.emu.bus;

public interface AddressableIf {

    public boolean write(int address, int data);

    public Integer read(int address);

    public int getStartAddress();

    public int getEndAddress();

    public void setEnabled(boolean enabled);

    public boolean isEnabled();
}
