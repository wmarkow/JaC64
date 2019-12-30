package com.dreamfabric.jac64.emu.cia.keyboard;

public enum Joy2Key {
    VK_FIRE(0x10), VK_RIGHT(0x08), VK_LEFT(0x04), VK_DOWN(0x02), VK_UP(0x01);

    private int portAValue;

    private Joy2Key(int portAValue) {
        this.portAValue = portAValue;
    }

    public int getPortAValue() {
        return portAValue;
    }
}
