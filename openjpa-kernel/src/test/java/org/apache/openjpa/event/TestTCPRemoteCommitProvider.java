/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.openjpa.lib.log.LogFactoryImpl;
import org.apache.openjpa.util.UserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

class TestTCPRemoteCommitProvider {
    /**
     * An address that is guaranteed not to belong to this host:
     * 192.0.2.0/24 is TEST-NET-1 (RFC 5737) and can never be a
     * local interface address.
     */
    private static final String NON_LOCAL_ADDRESS = "192.0.2.1";

    /**
     * .invalid is a reserved TLD (RFC 2606), so this name can never
     * be resolved via DNS.
     */
    private static final String UNRESOLVABLE_HOST = "nonexistent.invalid";

    private static InetAddress findLocalAddress() throws SocketException {
        // Prefer a non-loopback address so the test doesn't depend on
        // how the loopback interface is configured.
        Optional<InetAddress> address = NetworkInterface.networkInterfaces()
            .flatMap(NetworkInterface::inetAddresses)
            .filter(addr -> !addr.isLoopbackAddress())
            .findFirst();
        if (address.isEmpty()) {
            Optional<InetAddress> fallback = NetworkInterface.networkInterfaces()
                .flatMap(NetworkInterface::inetAddresses)
                .filter(InetAddress::isLoopbackAddress)
                .findFirst();
            address = address.or(() -> fallback);
        }
        return address.orElseThrow(() -> new AssertionError("No local addresses found on this host"));
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target instanceof TCPRemoteCommitProvider
            ? TCPRemoteCommitProvider.class.getDeclaredField(fieldName)
            : target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void setBindAddressValidLocalAddress() throws Exception {
        TCPRemoteCommitProvider provider = new TCPRemoteCommitProvider();
        InetAddress localAddress = findLocalAddress();

        provider.setBindAddress(localAddress.getHostAddress());

        assertEquals(localAddress, getPrivateField(provider, "_bindAddress"));
        assertArrayEquals(localAddress.getAddress(), (byte[]) getPrivateField(provider, "_localhost"));
    }

    @Test
    void setBindAddressLocalHostname() throws Exception {
        TCPRemoteCommitProvider provider = new TCPRemoteCommitProvider();

        // "localhost" always resolves to a loopback address, which is one
        // of this host's interface addresses.
        provider.setBindAddress("localhost");

        Object bindAddress = getPrivateField(provider, "_bindAddress");
        assertNotNull(bindAddress);
        // The resolved address must be one of this host's addresses.
        boolean isLocal = NetworkInterface.networkInterfaces()
            .flatMap(NetworkInterface::inetAddresses)
            .anyMatch(bindAddress::equals);
        assertTrue(isLocal, "Bind address " + bindAddress + " is not a local address");
    }

    @Test
    void setBindAddressNonLocalAddressThrows() throws Exception {
        TCPRemoteCommitProvider provider = new TCPRemoteCommitProvider();

        try {
            provider.setBindAddress(NON_LOCAL_ADDRESS);
            fail("Expected UserException for a non-local bind address");
        } catch (UserException ue) {
            assertTrue(ue.getMessage().contains(NON_LOCAL_ADDRESS), "Unexpected message: " + ue.getMessage());
            // Nothing must have been set.
            assertNull(getPrivateField(provider, "_bindAddress"));
        }
    }

    @Test
    void setBindAddressUnresolvableHostThrows() throws Exception {
        TCPRemoteCommitProvider provider = null;
        try {
            provider = new TCPRemoteCommitProvider();
        } catch (UnknownHostException uhe) {
            fail("Could not construct TCPRemoteCommitProvider: " + uhe);
        }

        try {
            provider.setBindAddress(UNRESOLVABLE_HOST);
            fail("Expected UnknownHostException for an unresolvable host name");
        } catch (UnknownHostException uhe) {
            assertTrue(uhe.getMessage().contains(UNRESOLVABLE_HOST), "Unexpected message: " + uhe.getMessage());
            assertNull(getPrivateField(provider, "_bindAddress"));
        }
    }

    /**
     * Creates a provider with its log set. Normally OpenJPA performs the
     * configuration (and sets the log) before the provider is used.
     */
    private static TCPRemoteCommitProvider createProvider() throws UnknownHostException {
        TCPRemoteCommitProvider provider = new TCPRemoteCommitProvider();
        provider.log = new LogFactoryImpl().getLog(TestTCPRemoteCommitProvider.class.getName());
        return provider;
    }

    /**
     * Returns the configured peer addresses as a list of "host:port"
     * strings, in the order they were set.
     */
    private static List<String> addressStrings(TCPRemoteCommitProvider provider) {
        return provider._addresses.stream()
            .map(TCPRemoteCommitProvider.HostAddress::toString)
            .toList();
    }

    @Test
    void setAddressesMultiplePeers() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        provider.setAddresses("192.0.2.1:5671;198.51.100.2:5672");

        assertEquals(List.of("192.0.2.1:5671", "198.51.100.2:5672"), addressStrings(provider));
    }

    @Test
    void setAddressesWithoutPortUsesDefaultPort() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        provider.setAddresses(NON_LOCAL_ADDRESS);

        assertEquals(List.of(NON_LOCAL_ADDRESS + ":5636"), addressStrings(provider));
    }

    @Test
    void setAddressesExcludesSelf() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        // 127.0.0.1 is a loopback address of this host and the port matches
        // the provider's default port, so it must be excluded.
        provider.setAddresses("127.0.0.1:5636;" + NON_LOCAL_ADDRESS + ":5636");

        assertEquals(List.of(NON_LOCAL_ADDRESS + ":5636"), addressStrings(provider));
    }

    @Test
    void setAddressesLocalAddressOtherPortIsIncluded() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        // Same host as above, but a different port, so it must be kept.
        provider.setAddresses("127.0.0.1:5671");

        assertEquals(List.of("127.0.0.1:5671"), addressStrings(provider));
    }

    @Test
    void setAddressesReplacesPreviousAddresses() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        provider.setAddresses(NON_LOCAL_ADDRESS + ":5671");
        provider.setAddresses("198.51.100.9:5673");

        assertEquals(List.of("198.51.100.9:5673"), addressStrings(provider));
    }

    @Test
    void setAddressesUnresolvableHostThrows() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        try {
            provider.setAddresses(UNRESOLVABLE_HOST + ":5671");
            fail("Expected UnknownHostException for an unresolvable host name");
        } catch (UnknownHostException uhe) {
            assertTrue(uhe.getMessage().contains(UNRESOLVABLE_HOST), "Unexpected message: " + uhe.getMessage());
        }
    }

    @Test
    void acceptsConnectionFromThisHost() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        assertTrue(provider.acceptsConnectionFrom(InetAddress.getLoopbackAddress()));
        assertTrue(provider.acceptsConnectionFrom(findLocalAddress()));
    }

    @Test
    void acceptsConnectionOnlyFromPeers() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();
        provider.setAddresses(NON_LOCAL_ADDRESS + ":5671");

        // the port of the peer is not relevant, peers connect from arbitrary ports
        assertTrue(provider.acceptsConnectionFrom(InetAddress.getByName(NON_LOCAL_ADDRESS)));
        assertFalse(provider.acceptsConnectionFrom(InetAddress.getByName("198.51.100.2")));
    }

    @Test
    void acceptsConnectionFromAnyHostWhenNotRestricted() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();
        provider.setRestrictToPeers(false);

        assertTrue(provider.acceptsConnectionFrom(InetAddress.getByName("198.51.100.2")));
    }

    @Test
    void acceptsConnectionFollowsAddressUpdates() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();
        InetAddress first = InetAddress.getByName(NON_LOCAL_ADDRESS);
        InetAddress second = InetAddress.getByName("198.51.100.2");

        provider.setAddresses(NON_LOCAL_ADDRESS + ":5671");
        assertTrue(provider.acceptsConnectionFrom(first));
        assertFalse(provider.acceptsConnectionFrom(second));

        provider.setAddresses("198.51.100.2:5671");
        assertFalse(provider.acceptsConnectionFrom(first));
        assertTrue(provider.acceptsConnectionFrom(second));
    }

    @Test
    @Timeout(value = 10, threadMode = ThreadMode.SEPARATE_THREAD)
    void acceptsConnectionDoesNotWaitForSending() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();
        provider.setAddresses(NON_LOCAL_ADDRESS + ":5671");

        // sending to the peers holds the lock of the addresses, possibly
        // for a long time while connecting to a peer that is not reachable
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread sender = new Thread(() -> {
            provider._addressesLock.lock();
            try {
                locked.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                provider._addressesLock.unlock();
            }
        });
        sender.start();
        locked.await();
        try {
            assertTrue(provider.acceptsConnectionFrom(InetAddress.getByName(NON_LOCAL_ADDRESS)));
            assertFalse(provider.acceptsConnectionFrom(InetAddress.getByName("198.51.100.2")));
        } finally {
            release.countDown();
            sender.join();
        }
    }

    /**
     * Provider that treats connections from this host as coming from a
     * host that is not a peer, as no other host is available in a test.
     */
    private static class NonPeerProvider extends TCPRemoteCommitProvider {
        private NonPeerProvider() throws UnknownHostException {
            super();
        }

        @Override
        protected boolean acceptsConnectionFrom(InetAddress address) {
            return !isRestrictToPeers();
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    private static TCPRemoteCommitProvider startProvider(TCPRemoteCommitProvider provider) throws IOException {
        provider.log = new LogFactoryImpl().getLog(TestTCPRemoteCommitProvider.class.getName());
        provider.setNumBroadcastThreads(0);
        provider.setPort(freePort());
        provider.endConfiguration();
        return provider;
    }

    private static Socket connect(TCPRemoteCommitProvider provider) throws IOException {
        return new Socket(InetAddress.getLoopbackAddress(), provider.getPort());
    }

    /**
     * Whether the remote end closed the connection within a few seconds.
     */
    private static boolean isClosedByRemote(Socket s) throws IOException {
        s.setSoTimeout(5000);
        try (InputStream in = s.getInputStream()) {
            return in.read() == -1;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (SocketException e) {
            // connection reset
            return true;
        }
    }

    /**
     * Whether the remote end keeps the connection open.
     */
    private static boolean isKeptOpen(Socket s) throws IOException {
        s.setSoTimeout(300);
        try {
            return s.getInputStream().read() != -1;
        } catch (SocketTimeoutException e) {
            return true;
        } catch (SocketException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<Thread> receiverThreads(TCPRemoteCommitProvider provider) throws Exception {
        Object listener = getPrivateField(provider, "_listener");
        return (Set<Thread>) getPrivateField(listener, "_receiverThreads");
    }

    private static void awaitReceivers(TCPRemoteCommitProvider provider, int expected) throws Exception {
        long end = System.currentTimeMillis() + 5000;
        while (receiverThreads(provider).size() != expected && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        assertEquals(expected, receiverThreads(provider).size());
    }

    @Test
    void connectionFromNonPeerIsRejected() throws Exception {
        TCPRemoteCommitProvider provider = startProvider(new NonPeerProvider());
        try (Socket s = connect(provider)) {
            assertTrue(isClosedByRemote(s));
            assertEquals(0, receiverThreads(provider).size());
        } finally {
            provider.close();
        }
    }

    @Test
    void connectionFromNonPeerIsAcceptedWhenNotRestricted() throws Exception {
        TCPRemoteCommitProvider provider = new NonPeerProvider();
        provider.setRestrictToPeers(false);
        startProvider(provider);
        try (Socket s = connect(provider)) {
            assertTrue(isKeptOpen(s));
            awaitReceivers(provider, 1);
        } finally {
            provider.close();
        }
    }

    @Test
    void maxReceiveConnectionsIsEnforcedAndFinishedReceiversAreRemoved() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();
        provider.setMaxReceiveConnections(2);
        startProvider(provider);
        List<Socket> sockets = new ArrayList<>();
        try {
            sockets.add(connect(provider));
            sockets.add(connect(provider));
            awaitReceivers(provider, 2);
            assertTrue(isKeptOpen(sockets.get(0)));
            assertTrue(isKeptOpen(sockets.get(1)));

            // over the limit
            try (Socket s = connect(provider)) {
                assertTrue(isClosedByRemote(s));
            }
            assertEquals(2, receiverThreads(provider).size());

            // a finished receiver removes itself, which makes room for a new connection
            sockets.remove(0).close();
            awaitReceivers(provider, 1);
            sockets.add(connect(provider));
            awaitReceivers(provider, 2);
            assertTrue(isKeptOpen(sockets.get(1)));
        } finally {
            for (Socket s : sockets) {
                s.close();
            }
            provider.close();
        }
    }

    /**
     * Dynamic provider that treats connections from this host as coming from
     * a host that only becomes a peer once a refresh of the addresses finds it.
     */
    private static class DynamicNonPeerProvider extends DynamicTCPRemoteCommitProvider {
        private final List<Thread> _fetchThreads = new CopyOnWriteArrayList<>();
        private volatile boolean _peerAppears;
        private volatile boolean _isPeer;

        private DynamicNonPeerProvider() throws UnknownHostException {
            super();
            setCacheDurationMillis(3600000);
        }

        @Override
        protected List<String> fetchDynamicAddresses() {
            _fetchThreads.add(Thread.currentThread());
            if (_peerAppears) {
                _isPeer = true;
            }
            return List.of();
        }

        @Override
        protected boolean acceptsConnectionFrom(InetAddress address) {
            return !isRestrictToPeers() || _isPeer;
        }
    }

    private static DynamicNonPeerProvider startDynamicProvider() throws Exception {
        DynamicNonPeerProvider provider = new DynamicNonPeerProvider();
        startProvider(provider);
        // the initial refresh and the first run of the periodic refresh
        long end = System.currentTimeMillis() + 5000;
        while (provider._fetchThreads.size() < 2 && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        assertEquals(2, provider._fetchThreads.size());
        return provider;
    }

    private static void assertNotRefreshedOnAcceptThread(TCPRemoteCommitProvider provider, List<Thread> fetchThreads)
        throws Exception {
        Object listener = getPrivateField(provider, "_listener");
        Thread acceptThread = (Thread) getPrivateField(listener, "_acceptThread");
        assertFalse(fetchThreads.contains(acceptThread));
    }

    @Test
    void dynamicProviderRefreshesAddressesBeforeRejectingUnknownHost() throws Exception {
        DynamicNonPeerProvider provider = startDynamicProvider();
        try (Socket s = connect(provider)) {
            assertTrue(isClosedByRemote(s));
            assertEquals(3, provider._fetchThreads.size());
            assertNotRefreshedOnAcceptThread(provider, provider._fetchThreads);
            awaitReceivers(provider, 0);
        } finally {
            provider.close();
        }
    }

    @Test
    void dynamicProviderAcceptsPeerFoundByRefresh() throws Exception {
        DynamicNonPeerProvider provider = startDynamicProvider();
        provider._peerAppears = true;
        try (Socket s = connect(provider)) {
            assertTrue(isKeptOpen(s));
            awaitReceivers(provider, 1);
            assertEquals(3, provider._fetchThreads.size());
            assertNotRefreshedOnAcceptThread(provider, provider._fetchThreads);
        } finally {
            provider.close();
        }
    }

    @Test
    void dynamicProviderDoesNotRefreshWhenNotRestricted() throws Exception {
        DynamicNonPeerProvider provider = new DynamicNonPeerProvider();
        provider.setRestrictToPeers(false);
        assertFalse(provider.refreshesAddressesOnDemand());
    }
}
