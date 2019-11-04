package com.dreamfabric.jac64.emu.cia;

import com.dreamfabric.jac64.emu.EventQueue;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.vic.C64Screen;

public class CCIA1 extends CIA {

    public CCIA1(EventQueue scheduler, InterruptManager interruptManager) {
        super(scheduler, C64Screen.IO_OFFSET + 0xdc00, interruptManager);
    }
}
