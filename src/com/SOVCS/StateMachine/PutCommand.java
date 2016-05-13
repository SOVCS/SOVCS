package com.SOVCS.StateMachine;

import io.atomix.copycat.Command;

import java.io.IOException;
import java.io.Serializable;

public class PutCommand implements Command<Object>, Serializable {
    private final int key;
    private final FileState value;

    public PutCommand(int key, FileState value) {
        this.key = key;
        this.value = value;
    }

    public Object key() {
        return key;
    }

    public Object value() {
        return value;
    }
}
