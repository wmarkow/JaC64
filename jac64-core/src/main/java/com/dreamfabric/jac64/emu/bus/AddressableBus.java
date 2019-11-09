package com.dreamfabric.jac64.emu.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.memory.BasicROM;
import com.dreamfabric.jac64.emu.memory.CharROM;
import com.dreamfabric.jac64.emu.memory.KernalROM;
import com.dreamfabric.jac64.emu.memory.RAM;

public class AddressableBus implements AddressableIf {
    private static Logger LOGGER = LoggerFactory.getLogger(AddressableBus.class);

    private final static int START_ADDRESS = 0x0000;
    private final static int END_ADDRESS = 0xFFFF;

    private BasicROM basicRom;
    private KernalROM kernalRom;
    private CharROM charRom;
    private IO io;
    private RAM ram;

    public void setBasicRom(BasicROM basicRom) {
        this.basicRom = basicRom;
    }

    public void setKernalRom(KernalROM kernalRom) {
        this.kernalRom = kernalRom;
    }

    public void setCharRom(CharROM charRom) {
        this.charRom = charRom;
    }

    public void setIO(IO io) {
        this.io = io;
    }

    public void setRAM(RAM ram) {
        this.ram = ram;
    }

    public RAM getRAM() {
        return ram;
    }

    @Override
    public boolean write(int address, int data, long currentCpuCycles) {
        boolean result = false;

        // it makes no point to write to ROMs
        result = io.write(address, data, currentCpuCycles);
        if (result) {
            return result;
        }

        return result;
    }

    @Override
    public Integer read(int address, long currentCpuCycles) {
        Integer result = null;

        result = basicRom.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        result = kernalRom.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        result = charRom.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        result = io.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        return null;
    }

    public int readVicExclusive(int cia2PRA, int addressSeenByVic) {
        int vicBankBaseAddress = (~(cia2PRA) & 3) << 14;

        int addressSeenByCPU = vicBankBaseAddress | addressSeenByVic;

        LOGGER.info(String.format("VIC exclusive read: vic address = 0x%05X ---> CPU address = 0x%05X",
                addressSeenByVic, addressSeenByCPU));

        Integer result = vicReadFromCharacterROM(vicBankBaseAddress, addressSeenByVic);
        if (result != null) {
            return result;
        }

        // read from RAM
        return ram.read(addressSeenByCPU, 0);
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

    /***
     * <p>
     * p6 <= n_va14 and not va13 and va12 and n_aec and n_game ; if true then
     * address 0x1000
     * </p>
     * <p>
     * p7 <= n_va14 and not va13 and va12 and n_aec and not n_exrom and not n_game
     * ;if true then address 0x9000
     * </p>
     * 
     * @param vicBankBaseAddress
     * @param addressSeenByVic
     * @return
     */
    private Integer vicReadFromCharacterROM(int vicBankBaseAddress, int addressSeenByVic) {
        int charROMAddress = -1;

        if ((addressSeenByVic & 0x1000) == 0x1000) {
            // read from Character ROM
            charROMAddress = vicBankBaseAddress | (addressSeenByVic - 0x1000);
        }

        if ((addressSeenByVic & 0x9000) == 0x9000) {
            // read from Character ROM
            charROMAddress = vicBankBaseAddress | (addressSeenByVic - 0x9000);
        }

        if (charROMAddress == -1) {
            return null;
        }

        boolean enabled = charRom.isEnabled();
        charRom.setEnabled(true);
        Integer result = charRom.read(charROMAddress, 0);
        charRom.setEnabled(enabled);

        return result;
    }
}
