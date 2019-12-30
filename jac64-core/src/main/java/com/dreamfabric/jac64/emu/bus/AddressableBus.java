package com.dreamfabric.jac64.emu.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cia.CIA2;
import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.memory.BasicROM;
import com.dreamfabric.jac64.emu.memory.CharROM;
import com.dreamfabric.jac64.emu.memory.ColorRAM;
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
    private CIA2 cia2;

    // statistics
    private long readCount = 0;
    private long writeCount = 0;

    public AddressableBus(CIA2 cia2) {
        this.cia2 = cia2;
    }

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
        writeCount++;

        boolean result = false;

        // it makes no point to write to ROMs
        result = io.write(address, data, currentCpuCycles);
        if (result) {
            return result;
        }

        return ram.write(address, data, currentCpuCycles);
    }

    @Override
    public Integer read(int address, long currentCpuCycles) {
        readCount++;

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

        return ram.read(address, currentCpuCycles);
    }

    public int readVicExclusive(int addressSeenByVic) {
        int cia2PRA = cia2.getPRA();
        int vicBankBaseAddress = (~(cia2PRA) & 3) << 14;

        boolean va14 = ((vicBankBaseAddress >> 14) & 0x1) == 1 ? true : false;

        Integer result = vicReadFromCharacterROM(va14, addressSeenByVic);
        if (result != null) {
            return result;
        }

        // read from RAM
        int addressSeenByCPU = vicBankBaseAddress | addressSeenByVic;
        return ram.read(addressSeenByCPU, 0);
    }

    public Integer readVicExclusiveFromColorRAM(int addressSeenByVic) {
        ColorRAM colorRam = io.getColorRAM();

        int address = colorRam.getStartAddress() + addressSeenByVic;

        boolean enabled = colorRam.isEnabled();
        colorRam.setEnabled(true);
        Integer result = colorRam.read(address, 0);
        colorRam.setEnabled(enabled);

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

    @Override
    public void setEnabled(boolean enabled) {
        // nothing to do here
    }

    @Override
    public boolean isEnabled() {
        // always true
        return true;
    }

    public long getReadCount() {
        return readCount;
    }

    public long getWriteCount() {
        return writeCount;
    }

    public void dumpUsage(long currentCpuCycles) {
        LOGGER.info(String.format("Was %s TOTAL operations per second.",
                1000000 * (readCount + writeCount) / currentCpuCycles));
        LOGGER.info(String.format("Was %s   CIA operations per second.",
                1000000 * (io.getCia1().getReadCount() + io.getCia1().getWriteCount()) / currentCpuCycles));
    }

    /***
     * if (p6 || p7) == true then CharROM is enabled.
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
    private Integer vicReadFromCharacterROM(boolean va14, int addressSeenByVic) {
        int charROMAddress = -1;

        boolean n_va14 = !va14;

        if (n_va14 == false) {
            // n_va14 is 0 => p6 = false and p7 = false. CharROM is not enabled.
            return null;
        }

        if (addressSeenByVic >= 0x1000 && addressSeenByVic <= 0x1FFF) {
            // read from Character ROM
            charROMAddress = addressSeenByVic - 0x1000 + charRom.getStartAddress();
        }

        if (addressSeenByVic >= 0x9000 && addressSeenByVic <= 0x9FFF) {
            // read from Character ROM
            charROMAddress = addressSeenByVic - 0x9000 + charRom.getStartAddress();
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
