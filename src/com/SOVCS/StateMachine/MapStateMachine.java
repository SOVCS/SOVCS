package com.SOVCS.StateMachine;

import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapStateMachine extends StateMachine implements Snapshottable, SessionListener {
    private Map<Object, Object> map = new HashMap<>();
    private Set<ServerSession> sessions = new HashSet<>();

    public void put(Commit<PutCommand> commit) {
        try {
            map.put(commit.operation().key(), commit.operation().value());
        } finally {
            commit.close();
        }
    }

    public Object get(Commit<GetQuery> commit) {
        try {
            return map.get(commit.operation().key());
        } finally {
            commit.close();
        }
    }

    @Override
    public void register(ServerSession session) {
        this.sessions.add(session);
    }

    @Override
    public void unregister(ServerSession session) {
    }

    @Override
    public void expire(ServerSession session) {
    }

    @Override
    public void close(ServerSession session) {
        this.sessions.remove(session);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeObject(map);
    }

    @Override
    public void install(SnapshotReader reader) {
        map = reader.readObject();
    }
}