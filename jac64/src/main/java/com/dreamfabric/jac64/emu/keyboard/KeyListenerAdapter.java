package com.dreamfabric.jac64.emu.keyboard;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cia.Key;
import com.dreamfabric.jac64.emu.cia.KeyListener;

/***
 * Provides a mapping from {@link java.awt.event.KeyListener} to
 * {@link com.dreamfabric.jac64.emu.cia.KeyListener}
 * 
 * @author Witold Markowski
 *
 */
public class KeyListenerAdapter implements java.awt.event.KeyListener {
    private static Logger LOGGER = LoggerFactory.getLogger(KeyListenerAdapter.class);

    private KeyListener keyListener;
    private Set<Integer> pressedKeys = new HashSet<Integer>();
    private Map<Integer, Key> keyMap = new HashMap<Integer, Key>();

    public KeyListenerAdapter(KeyListener keyListener) {
        this.keyListener = keyListener;
        prepareMap();
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (pressedKeys.contains(event.getExtendedKeyCode())) {
            return;
        }
        pressedKeys.add(event.getExtendedKeyCode());

        LOGGER.info(String.format("key pressed %s code = %s, extended code = %s", event.getKeyChar(),
                event.getKeyCode(), event.getExtendedKeyCode()));

        Key key = map(event);
        if (key == null) {
            return;
        }

        keyListener.keyPressed(key);
    }

    @Override
    public void keyReleased(KeyEvent event) {
        LOGGER.info(String.format("key released %s code = %s, extended code = %s", event.getKeyChar(),
                event.getKeyCode(), event.getExtendedKeyCode()));

        pressedKeys.remove(event.getExtendedKeyCode());

        Key key = map(event);
        if (key == null) {
            return;
        }

        keyListener.keyReleased(key);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private Key map(KeyEvent event) {

        // special case: shift key, distinguish between left and right
        if (event.getKeyCode() == KeyEvent.VK_SHIFT) {
            if (event.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT) {
                return Key.VK_LSHIFT;
            }
            return Key.VK_RSHIFT;
        }

        return keyMap.get(event.getKeyCode());
    }

    private void prepareMap() {
        // TODO: map those missing keys which are put with 0 key
        keyMap.put(0, Key.VK_STOP);
        keyMap.put(KeyEvent.VK_Q, Key.VK_Q);
        keyMap.put(0, Key.VK_COMMODORE);
        keyMap.put(KeyEvent.VK_SPACE, Key.VK_SPACE);
        keyMap.put(KeyEvent.VK_2, Key.VK_2);
        keyMap.put(KeyEvent.VK_CONTROL, Key.VK_CTRL);
        keyMap.put(0, Key.VK_ARROW_LEFT);
        keyMap.put(KeyEvent.VK_1, Key.VK_1);
        keyMap.put(KeyEvent.VK_SLASH, Key.VK_SLASH);
        keyMap.put(0, Key.VK_ARROW_TOP);
        keyMap.put(KeyEvent.VK_EQUALS, Key.VK_EQUALS);
        // keyMap.put(KeyEvent.VK_SHIFT, Key.RSHIFT); // a special SHIFT case
        keyMap.put(KeyEvent.VK_HOME, Key.VK_HOME);
        keyMap.put(KeyEvent.VK_SEMICOLON, Key.VK_SEMICOLON);
        keyMap.put(KeyEvent.VK_ASTERISK, Key.VK_ASTERISK);
        keyMap.put(0, Key.VK_POUND);
        keyMap.put(KeyEvent.VK_COMMA, Key.VK_COMMA);
        keyMap.put(0, Key.VK_AT);
        keyMap.put(KeyEvent.VK_COLON, Key.VK_COLON);
        keyMap.put(KeyEvent.VK_PERIOD, Key.VK_PERIOD);
        keyMap.put(KeyEvent.VK_MINUS, Key.VK_MINUS);
        keyMap.put(KeyEvent.VK_L, Key.VK_L);
        keyMap.put(KeyEvent.VK_P, Key.VK_P);
        keyMap.put(KeyEvent.VK_PLUS, Key.VK_PLUS);
        keyMap.put(KeyEvent.VK_N, Key.VK_N);
        keyMap.put(KeyEvent.VK_O, Key.VK_O);
        keyMap.put(KeyEvent.VK_K, Key.VK_K);
        keyMap.put(KeyEvent.VK_M, Key.VK_M);
        keyMap.put(KeyEvent.VK_0, Key.VK_0);
        keyMap.put(KeyEvent.VK_J, Key.VK_J);
        keyMap.put(KeyEvent.VK_I, Key.VK_I);
        keyMap.put(KeyEvent.VK_9, Key.VK_9);
        keyMap.put(KeyEvent.VK_V, Key.VK_V);
        keyMap.put(KeyEvent.VK_U, Key.VK_U);
        keyMap.put(KeyEvent.VK_H, Key.VK_H);
        keyMap.put(KeyEvent.VK_B, Key.VK_B);
        keyMap.put(KeyEvent.VK_8, Key.VK_8);
        keyMap.put(KeyEvent.VK_G, Key.VK_G);
        keyMap.put(KeyEvent.VK_Y, Key.VK_Y);
        keyMap.put(KeyEvent.VK_7, Key.VK_7);
        keyMap.put(KeyEvent.VK_X, Key.VK_X);
        keyMap.put(KeyEvent.VK_T, Key.VK_T);
        keyMap.put(KeyEvent.VK_F, Key.VK_F);
        keyMap.put(KeyEvent.VK_C, Key.VK_C);
        keyMap.put(KeyEvent.VK_6, Key.VK_6);
        keyMap.put(KeyEvent.VK_D, Key.VK_D);
        keyMap.put(KeyEvent.VK_R, Key.VK_R);
        keyMap.put(KeyEvent.VK_5, Key.VK_5);
        // keyMap.put(KeyEvent.VK_SHIFT, Key.LSHIFT); // a special SHIFT case
        keyMap.put(KeyEvent.VK_E, Key.VK_E);
        keyMap.put(KeyEvent.VK_S, Key.VK_S);
        keyMap.put(KeyEvent.VK_Z, Key.VK_Z);
        keyMap.put(KeyEvent.VK_4, Key.VK_4);
        keyMap.put(KeyEvent.VK_A, Key.VK_A);
        keyMap.put(KeyEvent.VK_W, Key.VK_W);
        keyMap.put(KeyEvent.VK_3, Key.VK_3);
        keyMap.put(KeyEvent.VK_DOWN, Key.VK_CRSR_DN);
        keyMap.put(KeyEvent.VK_F5, Key.VK_F5);
        keyMap.put(KeyEvent.VK_F3, Key.VK_F3);
        keyMap.put(KeyEvent.VK_F1, Key.VK_F1);
        keyMap.put(KeyEvent.VK_F7, Key.VK_F7);
        keyMap.put(KeyEvent.VK_RIGHT, Key.VK_CRSR_RT);
        keyMap.put(KeyEvent.VK_ENTER, Key.VK_RETURN);
        keyMap.put(KeyEvent.VK_BACK_SPACE, Key.VK_DELETE);
    }
}
