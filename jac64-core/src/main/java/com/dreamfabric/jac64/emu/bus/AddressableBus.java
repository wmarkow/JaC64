package com.dreamfabric.jac64.emu.bus;

import java.util.ArrayList;
import java.util.List;

public class AddressableBus implements AddressableIf {

    private final static int START_ADDRESS = 0x0000;
    private final static int END_ADDRESS = 0xFFFF;

    private List<AddressableIf> addressables = new ArrayList<AddressableIf>();

    public void addAddressable(AddressableIf addressable) {
        addressables.add(addressable);
    }

    @Override
    public boolean write(int address, int data) {
        boolean result = false;

        for (AddressableIf addressable : addressables) {
            if (addressable.write(address, data)) {
                result = true;
            }
        }

        return result;
    }

    @Override
    public Integer read(int address) {
        for (AddressableIf addressable : addressables) {
            Integer result = addressable.read(address);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // nothing to do here
    }

    @Override
    public boolean isEnabled() {
        // always true
        return true;
    }

}
