package com.dreamfabric.jac64.emu.sid;

import com.dreamfabric.jac64.emu.bus.AddressableVoid;

/***
 * Implementation of {@link SIDIf} that doesn't generate any sound.
 * 
 * @author Witold Markowski
 *
 */
public class VoidSID extends AddressableVoid implements SIDIf {

    @Override
    public void start() {
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
