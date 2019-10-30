package com.dreamfabric.jac64.emu.keyboard;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cia.Joy1Key;
import com.dreamfabric.jac64.emu.cia.Joy1KeyListener;

public class Joy1KeyListenerAdapter implements java.awt.event.KeyListener {
    private static Logger LOGGER = LoggerFactory.getLogger(Joy1KeyListenerAdapter.class);

    private Joy1KeyListener joy1KeyListener;
    private Set<Integer> pressedKeys = new HashSet<Integer>();
    private Map<Integer, Joy1Key> keyMap = new HashMap<Integer, Joy1Key>();

    public Joy1KeyListenerAdapter(Joy1KeyListener joy1KeyListener) {
        this.joy1KeyListener = joy1KeyListener;
        prepareMap();
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (pressedKeys.contains(event.getExtendedKeyCode())) {
            return;
        }
        pressedKeys.add(event.getExtendedKeyCode());

        LOGGER.info(String.format("Joy1 key pressed %s code = %s, extended code = %s", event.getKeyChar(),
                event.getKeyCode(), event.getExtendedKeyCode()));

        Joy1Key key = map(event);
        if (key == null) {
            return;
        }

        joy1KeyListener.joy1KeyPressed(key);
    }

    @Override
    public void keyReleased(KeyEvent event) {
        LOGGER.info(String.format("Joy1 key released %s code = %s, extended code = %s", event.getKeyChar(),
                event.getKeyCode(), event.getExtendedKeyCode()));

        pressedKeys.remove(event.getExtendedKeyCode());

        Joy1Key key = map(event);
        if (key == null) {
            return;
        }

        joy1KeyListener.joy1KeyReleased(key);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private Joy1Key map(KeyEvent event) {
        return keyMap.get(event.getKeyCode());
    }

    private void prepareMap() {
        keyMap.put(KeyEvent.VK_NUMPAD5, Joy1Key.VK_DOWN);
        keyMap.put(KeyEvent.VK_NUMPAD0, Joy1Key.VK_FIRE);
        keyMap.put(KeyEvent.VK_NUMPAD4, Joy1Key.VK_LEFT);
        keyMap.put(KeyEvent.VK_NUMPAD6, Joy1Key.VK_RIGHT);
        keyMap.put(KeyEvent.VK_NUMPAD8, Joy1Key.VK_UP);
    }
}
