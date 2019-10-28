package com.dreamfabric.jac64.emu.sid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.bus.AbstractAddressable;

public class VoidSID extends AbstractAddressable implements SIDIf {

    private static Logger LOGGER = LoggerFactory.getLogger(VoidSID.class);

    public final static int START_ADDRESS = 0xD400;
    public final static int END_ADDRESS = 0xD41F;

    private long lastUpdate = 0;

    @Override
    public boolean write(int address, byte data) {
        boolean result = super.write(address, data);

        if (result) {
            long now = System.nanoTime();
            long delta = now - lastUpdate;

            lastUpdate = now;

//            LOGGER.info(String.format("write success. Delta ns = %s", delta));
        }

        return result;
    }

    @Override
    public Byte read(int address) {
        Byte result = super.read(address);

        if (result != null) {
//            LOGGER.info("read success");
        }

        return result;
    }

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }
}
