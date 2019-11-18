package com.dreamfabric.jac64.emu.cpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.bus.ControlBus;
import com.dreamfabric.jac64.emu.vic.C64Screen;

public class C64Cpu extends MOS6510Core {
    private static Logger LOGGER = LoggerFactory.getLogger(C64Cpu.class);

    private ControlBus controlBus;
    private AddressableBus addressableBus;
    private C64Screen c64screen = null;

    public void setControlBus(ControlBus controlBus) {
        this.controlBus = controlBus;
    }

    public void setAddressableBus(AddressableBus addressableBus) {
        this.addressableBus = addressableBus;
    }

    public void setC64Screen(C64Screen c64screen) {
        this.c64screen = c64screen;
    }

    public void reset() {
        c64screen.reset();
        // this will ensure the correct PLA state
        writeByte(1, 0x7);
    }

    public void runBasic() {
        setMemory(631, (int) 'R');
        setMemory(632, (int) 'U');
        setMemory(633, (int) 'N');
        setMemory(634, 13);// enter
        setMemory(198, 4); // length
    }

    // Reads the memory with all respect to all flags...
    @Override
    protected final int fetchByte(int adr) {
        /* a cycles passes for this read */
        currentCpuCycles++;

        /* Chips work first, then CPU */
        executeFromEventQueue(currentCpuCycles);
        while (baLowUntil > currentCpuCycles) {
            currentCpuCycles++;
            executeFromEventQueue(currentCpuCycles);
        }

        Integer result = addressableBus.read(adr, currentCpuCycles);
        if (result != null) {
            return (int) result;
        }

        throw new IllegalArgumentException("Read operation should be handled by addressable bus!");
    }

    // A byte is written directly to memory or to ioChips
    @Override
    protected final void writeByte(int adr, int data) {
        currentCpuCycles++;

        executeFromEventQueue(currentCpuCycles);

        if (adr == 0x01) {
            // setting CHAREN, HIRAM and LORAM of PLA
            controlBus.setCharenHiramLoram(data);
        }

        if (addressableBus.write(adr, data, currentCpuCycles)) {
            return;
        }

        throw new IllegalArgumentException("Write operation should be handled by addressable bus!");
    }

    protected void unknownInstruction(int pc, int op) {
        switch (op) {
            case MOS6510Ops.SLEEP:
                currentCpuCycles += 100;
                break;
            case MOS6510Ops.LOAD_FILE:
                if (getACC() == 0)
                    LOGGER.info(
                            "**** LOAD FILE! ***** PC = " + Integer.toString(pc, 16) + " => wmarkow unknown rindex");
                else
                    LOGGER.info("**** VERIFY!    ***** PC = " + pc + " => wmarkow unknown rindex");
                int len;
                int mptr = getMemory(0xbb) + (getMemory(0xbc) << 8);
                LOGGER.info("Filename len:" + (len = getMemory(0xb7)));
                String name = "";
                for (int i = 0; i < len; i++)
                    name += (char) getMemory(mptr++);
                name += '\n';
                int sec = getMemory(0xb9);
                LOGGER.info("name = " + name);
                LOGGER.info("Sec Address: " + sec);

                pc--;
                break;
        }
    }

    private void executeFromEventQueue(long currentCpuCycles) {
        c64screen.clock(currentCpuCycles);
        controlBus.executeFromEventQueue(currentCpuCycles);
    }

    private int getMemory(int address) {
        return addressableBus.getRAM().read0(address);
    }

    private void setMemory(int address, int data) {
        addressableBus.getRAM().write0(address, data);
    }
}
