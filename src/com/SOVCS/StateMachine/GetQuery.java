package com.SOVCS.StateMachine;

import io.atomix.copycat.Query;

import java.io.Serializable;

public class GetQuery implements Query<FileState>, Serializable {
    private final int key;

    public GetQuery(int key) {
        this.key = key;
    }

    public int key() {
        return key;
    }
}