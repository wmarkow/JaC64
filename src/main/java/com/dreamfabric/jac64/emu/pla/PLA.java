package com.dreamfabric.jac64.emu.pla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PLA {

    private static Logger LOGGER = LoggerFactory.getLogger(PLA.class);

    private boolean exrom = true;
    private boolean game = true;
    private boolean charen = true;
    private boolean hiram = true;
    private boolean loram = true;

    public PLA() {
        LOGGER.info("Test");
    }

    public void setCharenHiramLoren(int byteValue) {
        charen = (byteValue & 0b100) == 0b100 ? true : false;
        hiram = (byteValue & 0b010) == 0b010 ? true : false;
        loram = (byteValue & 0b001) == 0b001 ? true : false;

        // LOGGER.info(String.format("Setting CHAREN = %s, HIRAM = %s, LORAM = %s",
        // charen, hiram, loram));
    }
}
