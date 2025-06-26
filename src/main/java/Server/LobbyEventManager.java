package Server;

import catan.Catan;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LobbyEventManager {
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, StreamObserver<Catan.GameEvent>>> lobbyStreams = new ConcurrentHashMap<>();

    public void subscribe(UUID gameId, UUID userId, StreamObserver<Catan.GameEvent> obs) {
        lobbyStreams.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(userId, obs);
    }

    public void unsubscribe(UUID gameId, UUID userId) {
        ConcurrentMap<UUID, StreamObserver<Catan.GameEvent>> subs = lobbyStreams.get(gameId);
        if (subs != null) {
            subs.remove(userId);
        }
    }

    public void broadcast(UUID gameId, Catan.GameEvent event) {
        ConcurrentMap<UUID, StreamObserver<Catan.GameEvent>> subs = lobbyStreams.get(gameId);
        if (subs != null) {
            for (StreamObserver<Catan.GameEvent> obs : subs.values()) {
                try {
                    obs.onNext(event);
                } catch (Exception ignored) {}
            }
        }
    }
}
