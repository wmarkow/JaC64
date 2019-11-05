package com.dreamfabric.jac64.emu.bus;

/***
 * A basic implementation of {@link AddressableIf} which actually doesn't
 * perform any write and read operations. This class is useful to implement a
 * 'fake' devices that exist in the system but do nothing (like i.e. a
 * {@link VoidSID} device that will not produce any sound).
 * 
 * @see {@link AddressableVoid#write}
 * @see {@link AddressableVoid#read}
 * 
 * @author Witold Markowski
 *
 */
public abstract class AddressableVoid implements AddressableIf {

    private boolean enabled = false;

    /***
     * Performs a 'fake write' operation but returns a correct value.
     * 
     * @return Returns true when 'write' succeeded. Returns false if the 'write'
     *         operation was not possible (device disabled or address mismatch).
     */
    @Override
    public boolean write(int address, int data, long currentCpuCycles) {
        if (!canWrite(address)) {
            return false;
        }

        return true;
    }

    /***
     * Performs a 'fake read' operation.
     * 
     * @return It returns null.
     */
    @Override
    public Integer read(int address, long currentCpuCycles) {
        // TODO: return null or maybe zero?
        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    protected boolean canRead(int address) {
        if (!isEnabled()) {
            return false;
        }

        if (address < getStartAddress()) {
            return false;
        }

        if (address > getEndAddress()) {
            return false;
        }

        return true;
    }

    protected boolean canWrite(int address) {
        if (!isWritable()) {
            return false;
        }

        if (!isEnabled()) {
            return false;
        }

        if (address < getStartAddress()) {
            return false;
        }

        if (address > getEndAddress()) {
            return false;
        }

        return true;
    }
}
