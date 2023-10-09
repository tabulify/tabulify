package net.bytle.tower.util;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.AbstractSession;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import net.bytle.fs.Fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Add persistent session storage to {@link LocalSessionStore}
 */
public class PersistentLocalSessionStore implements SessionStore, LocalSessionStore, Handler<Long> {

  /**
   * Default of how often, in ms, to check for expired sessions
   */
  public static final long INTERVAL_5_SEC = 5000;
  public static final long INTERVAL_60_SEC = 60000;

  /**
   * Default name for map used to store sessions
   */
  private static final String DEFAULT_SESSION_MAP_NAME = "vertx-web.sessions";
  private static final Path SESSIONS_DIRECTORY = Paths.get(".", ".tower", "sessions");
  private static final String SESSION_FILE_EXTENSION = ".bin";
  public static final String SYNC_INTERVAL_CONF = "syncInterval";
  public static final String MAP_NAME_CONF = "mapName";
  private static PersistentLocalSessionStore store;


  private LocalMap<String, Session> localMap;
  /**
   * Get the session version on the file system
   */
  private HashMap<String, Integer> onDiskSessionDataCheckSum;
  private long syncInterval;
  private VertxContextPRNG random;

  private long timerID = -1;
  private boolean closed;

  private VertxInternal vertx;


  /**
   * @param vertx           - vertx instance
   * @param processInterval - How often, in ms, to process sessions (sync and delete)
   * @return the persistent store
   */
  public static PersistentLocalSessionStore create(Vertx vertx, long processInterval) {
    store = new PersistentLocalSessionStore();
    store.init(vertx, new JsonObject()
      .put(SYNC_INTERVAL_CONF, processInterval)
      .put(MAP_NAME_CONF, DEFAULT_SESSION_MAP_NAME));
    return store;
  }

  public static PersistentLocalSessionStore get() {
    return store;
  }

    @Override
  public Session createSession(long timeout) {
    return new SharedDataSessionImpl(random, timeout, DEFAULT_SESSIONID_LENGTH);
  }

  @Override
  public Session createSession(long timeout, int length) {
    return new SharedDataSessionImpl(random, timeout, length);
  }

  @Override
  public SessionStore init(Vertx vertx, JsonObject options) {
    // initialize a secure random
    this.random = VertxContextPRNG.current(vertx);
    this.vertx = (VertxInternal) vertx;
    this.syncInterval = options.getLong(SYNC_INTERVAL_CONF, INTERVAL_5_SEC);
    localMap = vertx.sharedData().getLocalMap(options.getString(MAP_NAME_CONF, DEFAULT_SESSION_MAP_NAME));
    onDiskSessionDataCheckSum = new HashMap<>();

    for (Path path : Fs.getChildrenFiles(SESSIONS_DIRECTORY)) {
      try {
        Buffer buffer = Buffer.buffer(Files.readAllBytes(path));
        SharedDataSessionImpl session = new SharedDataSessionImpl(random);
        session.readFromBuffer(0, buffer);
        localMap.put(session.id(), session);
        onDiskSessionDataCheckSum.put(session.id(), checksum(session));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    setTimer();

    return this;
  }

  @Override
  public long retryTimeout() {
    return 0;
  }

  @Override
  public Future<Session> get(String id) {
    final ContextInternal ctx = vertx.getOrCreateContext();
    return ctx.succeededFuture(localMap.get(id));
  }

  @Override
  public Future<Void> delete(String id) {
    final ContextInternal ctx = vertx.getOrCreateContext();
    localMap.remove(id);
    return ctx.succeededFuture();
  }

  @Override
  public Future<Void> put(Session session) {
    final ContextInternal ctx = vertx.getOrCreateContext();
    final AbstractSession oldSession = (AbstractSession) localMap.get(session.id());
    final AbstractSession newSession = (AbstractSession) session;

    if (oldSession != null) {
      // there was already some stored data in this case we need to validate versions
      if (oldSession.version() != newSession.version()) {
        return ctx.failedFuture("Session version mismatch");
      }
    }

    newSession.incrementVersion();
    localMap.put(session.id(), session);
    return ctx.succeededFuture();
  }

  @Override
  public Future<Void> clear() {
    final ContextInternal ctx = vertx.getOrCreateContext();
    localMap.clear();
    return ctx.succeededFuture();
  }

  @Override
  public Future<Integer> size() {
    final ContextInternal ctx = vertx.getOrCreateContext();
    return ctx.succeededFuture(localMap.size());
  }

  @Override
  public synchronized void close() {
    localMap.close();
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    closed = true;
  }

  /**
   * Handle the session timeout
   *
   * @param tid the event to handle
   */
  @Override
  public synchronized void handle(Long tid) {
    this.flush();
  }

  public PersistentLocalSessionStore flush() {
    long now = System.currentTimeMillis();

    Set<String> toRemove = new HashSet<>();
    Set<String> toHave = new HashSet<>();

    localMap.forEach((String id, Session session) -> {
      long idlePeriod = now - session.lastAccessed();
      long timeout = session.timeout();
      if (idlePeriod > timeout) {
        toRemove.add(id);
      } else {
        toHave.add(id);
      }
    });

    /**
     * To remove
     */
    for (String id : toRemove) {
      localMap.remove(id);
      Path sessionFile = SESSIONS_DIRECTORY.resolve(id + SESSION_FILE_EXTENSION);
      if (Files.exists(sessionFile)) {
        try {
          Files.delete(sessionFile);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    /**
     * To have
     */
    for (String id : toHave) {
      SharedDataSessionImpl session = (SharedDataSessionImpl) localMap.get(id);
      Path sessionFile = SESSIONS_DIRECTORY.resolve(id + SESSION_FILE_EXTENSION);
      if (!Files.exists(sessionFile)) {
        Fs.createFile(sessionFile);
        writeSessionData(session, sessionFile, checksum(session));
        continue;
      }

      /**
       * {@link Session#lastAccessed() Session Last Accessed} is not always used
       * (not use with Csrf for instance)
       * We therefore use the crc version
       */
      Integer diskVersion = onDiskSessionDataCheckSum.get(id);
      int actualVersion = checksum(session);
      if (diskVersion == null || diskVersion != actualVersion) {
        writeSessionData(session, sessionFile, actualVersion);
      }

    }

    if (!closed) {
      setTimer();
    }

    return this;
  }

  private void writeSessionData(Session session, Path sessionFile, int actualVersion) {
    try {
      if (session instanceof SharedDataSessionImpl) {
        Buffer buffer = Buffer.buffer();
        SharedDataSessionImpl sharedVersion = (SharedDataSessionImpl) session;
        sharedVersion.writeToBuffer(buffer);
        Files.write(sessionFile, buffer.getBytes());
        // keep track of the data version
        onDiskSessionDataCheckSum.put(session.id(), actualVersion);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void setTimer() {
    if (syncInterval != 0) {
      timerID = vertx.setTimer(syncInterval, this);
    }
  }

  protected int checksum(Session session) {
    if (session.isEmpty()) {
      return 0x0000;
    } else {
      int result = 1;

      for (Map.Entry<String, Object> kv : session.data().entrySet()) {
        String key = kv.getKey();
        result = 31 * result + key.hashCode();
        Object value = kv.getValue();
        if (value != null) {
          result = 31 * result + value.hashCode();
        }
      }
      return result;
    }
  }
}
