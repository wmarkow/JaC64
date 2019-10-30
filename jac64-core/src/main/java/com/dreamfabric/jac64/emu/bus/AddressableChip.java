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

    private int memory[];

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
    public boolean write(int address, int data) {
        if (!canWriteOrRead(address)) {
            return false;
        }

        write0(address, data);

        return true;
    }

    /***
     * Performs a read operation.
     * 
     * @return Returns not null value when read succeeded. Returns null if the read
     *         operation was not possible (device disabled or address mismatch).
     */
    @Override
    public Integer read(int address) {
        if (!canWriteOrRead(address)) {
            return null;
        }

        return memory[address - getStartAddress()];
    }

    protected void initMemory() {
        int length = getEndAddress() - getStartAddress();
        memory = new int[length];
    }

    protected void write0(int address, int data) {
        if (data < 0) {
            throw new IllegalArgumentException(String.format("Can't write: negative value %s", data));
        }

        if (data > 255) {
            throw new IllegalArgumentException(String.format("Can't write: value bigger than 255 %s", data));
        }

        if (address < getStartAddress()) {
            throw new IllegalArgumentException(
                    String.format("Can't write: address to small %s < %s", address, getStartAddress()));
        }

        if (address > getEndAddress()) {
            throw new IllegalArgumentException(
                    String.format("Can't write: address to big %s > %s", address, getEndAddress()));
        }
        memory[address - getStartAddress()] = data;
    }

    protected Integer read0(int address) {
        if (address < getStartAddress()) {
            throw new IllegalArgumentException(
                    String.format("Can't write: address to small %s < %s", address, getStartAddress()));
        }

        if (address > getEndAddress()) {
            throw new IllegalArgumentException(
                    String.format("Can't write: address to big %s > %s", address, getEndAddress()));
        }

        return memory[address - getStartAddress()];
    }
}
