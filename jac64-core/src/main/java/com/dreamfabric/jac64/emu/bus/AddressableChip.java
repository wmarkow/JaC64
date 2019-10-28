package com.dreamfabric.jac64.emu.bus;

/***
 * A basic implementation that performs a real write and read operations into an
 * internal byte array. This class may be used to implement a real emulated
 * devices.
 * 
 * @author Witold Markowski
 *
 */
public abstract class AddressableChip extends AddressableVoid {

    private byte memory[];

    public AddressableChip() {
        super();

        initMemory();
    }

    /***
     * Performs a write operation.
     * 
     * @return Returns true when write succeeded. Returns false if the write
     *         operation was not possible (device disabled or address mismatch).
     */
    @Override
    public boolean write(int address, byte data) {
        if (!canWriteOrRead(address)) {
            return false;
        }

        memory[address - getStartAddress()] = data;

        return true;
    }

    /***
     * Performs a read operation.
     * 
     * @return Returns not null value when read succeeded. Returns null if the read
     *         operation was not possible (device disabled or address mismatch).
     */
    @Override
    public Byte read(int address) {
        if (!canWriteOrRead(address)) {
            return null;
        }

        return memory[address - getStartAddress()];
    }

    protected void initMemory() {
        int length = getEndAddress() - getStartAddress();
        memory = new byte[length];
    }

}
