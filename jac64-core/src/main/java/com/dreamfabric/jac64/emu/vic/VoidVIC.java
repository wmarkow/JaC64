package com.dreamfabric.jac64.emu.vic;

import com.dreamfabric.jac64.emu.bus.AddressableVoid;

/***
 * Implementation of {@link VICIf} that doesn't generate any graphic.
 * 
 * @author Witold Markowski
 *
 */
public class VoidVIC extends AddressableVoid implements VICIf {

    @Override
    public void start(long currentCpuCycles) {
        // nothing to do here
    }

    @Override
    public void stop() {
        // nothing to do here
    }

    @Override
    public void reset() {
        // nothing to do here
    }

}
