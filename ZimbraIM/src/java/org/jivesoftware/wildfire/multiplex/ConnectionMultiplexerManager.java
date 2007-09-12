/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package org.jivesoftware.wildfire.multiplex;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.*;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.event.SessionEventDispatcher;
import org.jivesoftware.wildfire.event.SessionEventListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A ConnectionMultiplexerManager is responsible for keeping track of the connected
 * Connection Managers and the sessions that were established with the Connection
 * Managers. Moreover, a ConnectionMultiplexerManager is able to create, get and close
 * client sessions based on Connection requests.
 *
 * @author Gaston Dombiak
 */
public class ConnectionMultiplexerManager implements SessionEventListener {

    private static final ConnectionMultiplexerManager instance = new ConnectionMultiplexerManager();

    /**
     * Pseudo-random number generator object for use with getMultiplexerSession(String).
     */
    private static Random randGen = new Random();

    static {
        // Add the unique instance of this class as a session listener. We need to react
        // when sessions are closed so we can clean up the registry of client sessions.
        SessionEventDispatcher.addListener(instance);
    }

    /**
     * Map that keeps track of connection managers and hosted connections.
     * Key: stream ID; Value: Domain of connection manager hosting connection
     */
    private Map<String, String> streamIDs = new ConcurrentHashMap<String, String>();
    /**
     * Map that keeps track of connection managers and hosted sessions.
     * Key: Domain of connection manager; Value: Map with Key: stream ID; Value: Client session
     */
    private Map<String, Map<String, ClientSession>> sessionsByManager =
            new ConcurrentHashMap<String, Map<String, ClientSession>>();

    private SessionManager sessionManager;

    /**
     * Returns the unique instance of this class.
     *
     * @return the unique instance of this class.
     */
    public static ConnectionMultiplexerManager getInstance() {
        return instance;
    }

    /**
     * Returns the default secret key that connection managers should present while trying to
     * establish a new connection.
     *
     * @return the default secret key that connection managers should present while trying to
     *         establish a new connection.
     */
    public static String getDefaultSecret() {
        return JiveGlobals.getProperty("xmpp.multiplex.defaultSecret");
    }

    /**
     * Sets the default secret key that connection managers should present while trying to
     * establish a new connection.
     *
     * @param defaultSecret the default secret key that connection managers should present
     *        while trying to establish a new connection.
     */
    public static void setDefaultSecret(String defaultSecret) {
        JiveGlobals.setProperty("xmpp.multiplex.defaultSecret", defaultSecret);
    }

    private ConnectionMultiplexerManager() {
        sessionManager = XMPPServer.getInstance().getSessionManager();
        // Start thread that will send heartbeats to Connection Managers every 30 seconds
        // to keep connections open.
        Thread hearbeatThread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(30000);
                        for (ConnectionMultiplexerSession session : sessionManager
                                .getConnectionMultiplexerSessions()) {
                            session.getConnection().deliverRawText(" ");
                        }
                    }
                    catch (InterruptedException e) {
                        // Do nothing
                    }
                    catch(Exception e) {
                        Log.error(e);
                    }
                }
            }
        };
        hearbeatThread.setDaemon(true);
        hearbeatThread.setPriority(Thread.NORM_PRIORITY);
        hearbeatThread.start();
    }

    /**
     * Creates a new client session that was established to the specified connection manager.
     * The new session will not be findable through its stream ID.
     *
     * @param connectionManagerDomain the connection manager that is handling the connection
     *        of the session.
     *  @param host the XMPP server host which the client is talking to      
     * @param streamID the stream ID created by the connection manager for the new session.
     */
    public void createClientSession(String connectionManagerDomain, String host, String streamID) {
        try {
            Connection connection = new ClientSessionConnection(connectionManagerDomain, host);
            ClientSession session = SessionManager.getInstance()
                    .createClientSession(connection, host, new BasicStreamID(streamID));
            // Register that this streamID belongs to the specified connection manager
            streamIDs.put(streamID, connectionManagerDomain);
            // Register which sessions are being hosted by the speicifed connection manager
            Map<String, ClientSession> sessions = sessionsByManager.get(connectionManagerDomain);
            if (sessions == null) {
                synchronized (connectionManagerDomain.intern()) {
                    sessions = sessionsByManager.get(connectionManagerDomain);
                    if (sessions == null) {
                        sessions = new ConcurrentHashMap<String, ClientSession>();
                        sessionsByManager.put(connectionManagerDomain, sessions);
                    }
                }
            }
            sessions.put(streamID, session);
        }
        catch (UnauthorizedException e) {
            Log.error("Error creating virtual client session", e);
        }
    }

    /**
     * Closes an existing client session that was established through a connection manager.
     *
     * @param connectionManagerDomain the connection manager that is handling the connection
     *        of the session.
     * @param streamID the stream ID created by the connection manager for the session.
     */
    public void closeClientSession(String connectionManagerDomain, String streamID) {
        Map<String, ClientSession> sessions = sessionsByManager.get(connectionManagerDomain);
        if (sessions != null) {
            Session session = sessions.remove(streamID);
            if (session != null) {
                // Close the session
                session.getConnection().close();
            }
        }
    }

    /**
     * A connection manager has become available. Clients can now connect to the server through
     * the connection manager.
     *
     * @param connectionManagerName the connection manager that has become available.
     */
    public void multiplexerAvailable(String connectionManagerName) {
        // Add a new entry in the list of available managers. Here is where we are going to store
        // which clients were connected through which connection manager
        Map<String, ClientSession> sessions = sessionsByManager.get(connectionManagerName);
        if (sessions == null) {
            synchronized (connectionManagerName.intern()) {
                sessions = sessionsByManager.get(connectionManagerName);
                if (sessions == null) {
                    sessions = new ConcurrentHashMap<String, ClientSession>();
                    sessionsByManager.put(connectionManagerName, sessions);
                }
            }
        }
    }

    /**
     * A connection manager has gone unavailable. Close client sessions that were established
     * to the specified connection manager.
     *
     * @param connectionManagerName the connection manager that is no longer available.
     */
    public void multiplexerUnavailable(String connectionManagerName) {
        // Remove the connection manager and the hosted sessions
        Map<String, ClientSession> sessions = sessionsByManager.remove(connectionManagerName);
        if (sessions != null) {
            for (String streamID : sessions.keySet()) {
                // Remove inverse track of connection manager hosting streamIDs
                streamIDs.remove(streamID);
                // Close the session
                sessions.get(streamID).getConnection().close();
            }
        }
    }

    /**
     * Returns the ClientSession with the specified stream ID that is being hosted by the
     * specified connection manager.
     *
     * @param connectionManagerDomain the connection manager that is handling the connection
     *        of the session.
     * @param streamID the stream ID created by the connection manager for the session.
     * @return the ClientSession with the specified stream ID.
     */
    public ClientSession getClientSession(String connectionManagerDomain, String streamID) {
        Map<String, ClientSession> sessions = sessionsByManager.get(connectionManagerDomain);
        if (sessions != null) {
            return sessions.get(streamID);
        }
        return null;
    }

    /**
     * Returns a {@link ConnectionMultiplexerSession} for the specified connection manager
     * domain or <tt>null</tt> if none was found. In case the connection manager has many
     * connections established with the server then one of them will be selected randomly.
     *
     * @param connectionManagerDomain the domain of the connection manager to get a session.
     * @return a session to the specified connection manager domain or null if none was found.
     */
    public ConnectionMultiplexerSession getMultiplexerSession(String connectionManagerDomain) {
        List<ConnectionMultiplexerSession> sessions =
                sessionManager.getConnectionMultiplexerSessions(connectionManagerDomain);
        if (sessions.isEmpty()) {
            return null;
        }
        else if (sessions.size() == 1) {
            return sessions.get(0);
        }
        else {
            // Pick a random session so we can distribute traffic evenly
            return sessions.get(randGen.nextInt(sessions.size()));
        }
    }

    /**
     * Returns the names of the connected connection managers to this server.
     *
     * @return the names of the connected connection managers to this server.
     */
    public Collection<String> getMultiplexers() {
        return sessionsByManager.keySet();
    }

    /**
     * Returns the number of connected clients to a specific connection manager.
     *
     * @param managerName the name of the connection manager.
     * @return the number of connected clients to a specific connection manager.
     */
    public int getNumConnectedClients(String managerName) {
        Map<String, ClientSession> clients = sessionsByManager.get(managerName);
        if (clients == null) {
            return 0;
        }
        else {
            return clients.size();
        }
    }

    public void anonymousSessionCreated(Session session) {
        // Do nothing.
    }

    public void anonymousSessionDestroyed(Session session) {
        removeSession(session);
    }

    public void sessionCreated(Session session) {
        // Do nothing.
    }

    public void sessionDestroyed(Session session) {
        removeSession(session);
    }

    private void removeSession(Session session) {
        // Remove trace indicating that a connection manager is hosting a connection
        String streamID = session.getStreamID().getID();
        String connectionManagerDomain = streamIDs.remove(streamID);
        // Remove trace indicating that a connection manager is hosting a session
        if (connectionManagerDomain != null) {
            Map<String, ClientSession> sessions = sessionsByManager.get(connectionManagerDomain);
            if (sessions != null) {
                sessions.remove(streamID);
            }
        }
    }

    /**
     * Simple implementation of the StreamID interface to hold the stream ID assigned by
     * the Connection Manager to the Session.
     */
    private class BasicStreamID implements StreamID {
        String id;

        public BasicStreamID(String id) {
            this.id = id;
        }

        public String getID() {
            return id;
        }

        public String toString() {
            return id;
        }

        public int hashCode() {
            return id.hashCode();
        }
    }
}
