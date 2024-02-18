package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.AbstractSession;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.collections.MapDb;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Add persistent session storage to {@link LocalSessionStore}
 * <p>
 * This is not a cookie store. Cookie store does not work well with CSRF
 * because on post, the session cookie `cs-session-id` is not sent back with new value
 * A lot of problem with this way of working because the data is the session id
 * CookieSessionStore sessionStore = CookieSessionStore.create(towerDomain.getVertx(), secret);
 * <p>
 * This is a {@link io.vertx.ext.web.sstore.LocalSessionStore} that
 * was adapted to persist the session in MapDb
 */
public class PersistentLocalSessionStore implements SessionStore, LocalSessionStore, Handler<Long> {

  static final Logger LOGGER  = LogManager.getLogger(PersistentLocalSessionStore.class);
  /**
   * Default of how often, in ms, to check for expired sessions
   */
  public static final long INTERVAL_5_SEC = 5000;
  public static final long INTERVAL_60_SEC = 60000;

  public static final String SYNC_INTERVAL_CONF = "syncInterval";

  private final HTreeMap<String, Session> sessionMap;
  private final VertxContextPRNG random;
  private final HttpServer httpServer;
  private final MapDb mapDb;

  private long timerID = -1;


  /**
   *
   */
  public PersistentLocalSessionStore(HttpServer httpServer) {
    Server server = httpServer.getServer();

    // initialize a secure random
    this.random = VertxContextPRNG.current(server.getVertx());

    // How often, in ms, to process sessions (sync and delete)
    long defaultSyncInterval = PersistentLocalSessionStore.INTERVAL_60_SEC;
    if (JavaEnvs.IS_DEV) {
      defaultSyncInterval = PersistentLocalSessionStore.INTERVAL_5_SEC;
    }
    long syncInterval = server.getConfigAccessor().getLong(SYNC_INTERVAL_CONF, defaultSyncInterval);

    // localMap = vertx.sharedData().getLocalMap(options.getString(MAP_NAME_CONF, DEFAULT_SESSION_MAP_NAME));
    this.mapDb = server.getMapDb();
    this.sessionMap = server.getMapDb()
      .hashMap("sessions", Serializer.STRING, new Serializer<Session>() {
        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull Session session) throws IOException {
          if (!(session instanceof SharedDataSessionImpl)) {
            /**
             * See {@link PersistentLocalSessionStore#createSession(long)}
             */
            throw new InternalException("The session should be a shared data session");
          }
          Buffer buffer = Buffer.buffer();
          SharedDataSessionImpl sharedVersion = (SharedDataSessionImpl) session;
          sharedVersion.writeToBuffer(buffer);
          out.write(buffer.getBytes());
        }

        @Override
        public Session deserialize(@NotNull DataInput2 input, int available) throws IOException {
          byte[] bytes = new byte[available];
          input.readFully(bytes);
          Buffer buffer = Buffer.buffer(bytes);
          SharedDataSessionImpl session = new SharedDataSessionImpl(random);
          session.readFromBuffer(0, buffer);
          return session;
        }

      })
      .createOrOpen();

    this.httpServer = httpServer;
    if (syncInterval != 0) {
      timerID = server.getVertx().setTimer(syncInterval, this);
    }
    // run once to see if the store is still correct
    // may block if there is a problem
    this.purgeOldSession();

  }


  /**
   * @return the persistent store
   */
  public static PersistentLocalSessionStore create(HttpServer httpServer) {

    return new PersistentLocalSessionStore(httpServer);
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
    // ???
    return this;
  }

  @Override
  public long retryTimeout() {
    return 0;
  }

  @Override
  public Future<Session> get(String id) {
    return Future.succeededFuture(sessionMap.get(id));
  }

  @Override
  public Future<Void> delete(String id) {
    sessionMap.remove(id);
    mapDb.commit();
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> put(Session session) {

    final AbstractSession oldSession = (AbstractSession) sessionMap.get(session.id());
    final AbstractSession newSession = (AbstractSession) session;

    if (oldSession != null) {
      // there was already some stored data in this case we need to validate versions
      if (oldSession.version() != newSession.version()) {
        return Future.failedFuture("Session version mismatch");
      }
    }

    newSession.incrementVersion();
    sessionMap.put(session.id(), session);
    mapDb.commit();
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> clear() {

    sessionMap.clear();
    return Future.succeededFuture();
  }

  @Override
  public Future<Integer> size() {

    return Future.succeededFuture(sessionMap.size());
  }

  @Override
  public synchronized void close() {
    sessionMap.close();
    if (timerID != -1) {
      this.httpServer.getServer().getVertx().cancelTimer(timerID);
    }
  }

  /**
   * Handle the session timeout
   *
   * @param tid the event to handle
   */
  @Override
  public synchronized void handle(Long tid) {
    this.purgeOldSession();
  }


  private PersistentLocalSessionStore purgeOldSession() {

    long now = System.currentTimeMillis();

    AtomicBoolean deleted = new AtomicBoolean(false);
    for(Map.Entry<String,Session> entry:sessionMap.entrySet()){
      String id = entry.getKey();
      Session session = entry.getValue();
      long idlePeriod = now - session.lastAccessed();
      long timeout = session.timeout();
      if(!session.id().equals(id)){
        // Serialization/Deserialization problem?
        LOGGER.error("The id of the session ("+session.id()+") is not the same than the id of the map ("+id+"). Sessions was deleted");
        this.sessionMap.remove(id);
        continue;
      }
      if (idlePeriod > timeout) {
        deleted.set(true);
        this.sessionMap.remove(id);
      }

    }

    if(deleted.get()){
      this.mapDb.commit();
    }

    return this;

  }


}
