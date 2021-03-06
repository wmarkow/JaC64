package com.dreamfabric.jac64.emu.keyboard;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cia.keyboard.Joy2Key;
import com.dreamfabric.jac64.emu.cia.keyboard.Joy2KeyListener;

public class Joy2KeyListenerAdapter implements java.awt.event.KeyListener {
    private static Logger LOGGER = LoggerFactory.getLogger(Joy2KeyListenerAdapter.class);

    private Joy2KeyListener joy2KeyListener;
    private Set<Integer> pressedKeys = new HashSet<Integer>();
    private Map<Integer, Joy2Key> keyMap = new HashMap<Integer, Joy2Key>();

    public Joy2KeyListenerAdapter(Joy2KeyListener joy2KeyListener) {
        this.joy2KeyListener = joy2KeyListener;
        prepareMap();
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (pressedKeys.contains(event.getExtendedKeyCode())) {
            return;
        }
        pressedKeys.add(event.getExtendedKeyCode());

        LOGGER.info(String.format("Joy2 key pressed %s code = %s, extended code = %s", event.getKeyChar(),
                event.getKeyCode(), event.getExtendedKeyCode()));

        Joy2Key key = map(event);
        if (key == null) {
            return;
        }

        joy2KeyListener.joy2KeyPressed(key);
    }

    @Override
    public void keyReleased(KeyEvent event) {
        LOGGER.info(String.format("Joy2 key released %s code = %s, extended code = %s", event.getKeyChar(),
                event.getKeyCode(), event.getExtendedKeyCode()));

        pressedKeys.remove(event.getExtendedKeyCode());

        Joy2Key key = map(event);
        if (key == null) {
            return;
        }

        joy2KeyListener.joy2KeyReleased(key);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private Joy2Key map(KeyEvent event) {
        if (KeyEvent.VK_CONTROL == event.getKeyCode() && KeyEvent.KEY_LOCATION_LEFT == event.getKeyLocation()) {
            // left control pressed but we want right one
            return null;
        }

        return keyMap.get(event.getKeyCode());
    }

    private void prepareMap() {
        keyMap.put(KeyEvent.VK_DOWN, Joy2Key.VK_DOWN);
        keyMap.put(KeyEvent.VK_CONTROL, Joy2Key.VK_FIRE);
        keyMap.put(KeyEvent.VK_LEFT, Joy2Key.VK_LEFT);
        keyMap.put(KeyEvent.VK_RIGHT, Joy2Key.VK_RIGHT);
        keyMap.put(KeyEvent.VK_UP, Joy2Key.VK_UP);
    }
}
