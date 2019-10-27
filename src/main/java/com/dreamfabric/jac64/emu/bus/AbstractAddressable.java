package com.dreamfabric.jac64.emu.bus;

public abstract class AbstractAddressable implements AddressableIf {

    private boolean enabled = false;
    private byte memory[];

    public AbstractAddressable() {
        initMemory();
    }

    @Override
    public boolean write(int address, byte data) {
        if (!isEnabled()) {
            return false;
        }

        if (address < getStartAddress()) {
            return false;
        }

        if (address > getEndAddress()) {
            return false;
        }

        memory[address - getStartAddress()] = data;

        return true;
    }

    @Override
    public Byte read(int address) {
        if (!isEnabled()) {
            return null;
        }

        if (address < getStartAddress()) {
            return null;
        }

        if (address > getEndAddress()) {
            return null;
        }

        return memory[address - getStartAddress()];
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    protected void initMemory() {
        int length = getEndAddress() - getStartAddress();
        memory = new byte[length];
    }
}
