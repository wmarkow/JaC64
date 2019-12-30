package com.dreamfabric.jac64.emu.pla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.memory.BasicROM;
import com.dreamfabric.jac64.emu.memory.CharROM;
import com.dreamfabric.jac64.emu.memory.KernalROM;

public class PLA {

    private static Logger LOGGER = LoggerFactory.getLogger(PLA.class);

    private boolean exrom = true;
    private boolean game = true;
    private boolean charen = true;
    private boolean hiram = true;
    private boolean loram = true;

    private IO io = null;
    private BasicROM basicRom = null;
    private KernalROM kernalRom = null;
    private CharROM charRom = null;

    public PLA() {
        LOGGER.info("Test");
    }

    public void setCharenHiramLoram(int byteValue) {
        charen = (byteValue & 0b100) == 0b100 ? true : false;
        hiram = (byteValue & 0b010) == 0b010 ? true : false;
        loram = (byteValue & 0b001) == 0b001 ? true : false;

        // LOGGER.info(String.format("Setting CHAREN = %s, HIRAM = %s, LORAM = %s",
        // charen, hiram, loram));

        enableChips();
    }

    public void setIO(IO io) {
        this.io = io;
    }

    public void setBasicROM(BasicROM basicRom) {
        this.basicRom = basicRom;
    }

    public void setKernalROM(KernalROM kernalRom) {
        this.kernalRom = kernalRom;
    }

    public void setCharROM(CharROM charRom) {
        this.charRom = charRom;
    }

    private void enableChips() {
        io.setEnabled(false);
        basicRom.setEnabled(false);
        kernalRom.setEnabled(false);
        charRom.setEnabled(false);

        switch (getMode()) {
            case 0:
                break;
            case 1:
                charRom.setEnabled(true);
                break;
            case 2:
                kernalRom.setEnabled(true);
                charRom.setEnabled(true);
                break;
            case 3:
                basicRom.setEnabled(true);
                kernalRom.setEnabled(true);
                charRom.setEnabled(true);
                break;
            case 4:
                break;
            case 5:
                io.setEnabled(true);
                break;
            case 6:
                io.setEnabled(true);
                kernalRom.setEnabled(true);
                break;
            case 7:
                io.setEnabled(true);
                basicRom.setEnabled(true);
                kernalRom.setEnabled(true);
                break;
        }
    }

    private int getMode() {
        int mode = 0;
        if (charen) {
            mode += 4;
        }
        if (hiram) {
            mode += 2;
        }
        if (loram) {
            mode += 1;
        }

        return mode;
    }
}
