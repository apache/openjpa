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

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;

import org.apache.openjpa.lib.log.LogFactoryImpl;
import org.apache.openjpa.util.UserException;
import org.junit.Assert;
import org.junit.Test;

public class TestTCPRemoteCommitProvider {
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
        Field field = TCPRemoteCommitProvider.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    public void testSetBindAddressValidLocalAddress() throws Exception {
        TCPRemoteCommitProvider provider = new TCPRemoteCommitProvider();
        InetAddress localAddress = findLocalAddress();

        provider.setBindAddress(localAddress.getHostAddress());

        Assert.assertEquals(localAddress, getPrivateField(provider, "_bindAddress"));
        Assert.assertArrayEquals(localAddress.getAddress(), (byte[]) getPrivateField(provider, "_localhost"));
    }

    @Test
    public void testSetBindAddressLocalHostname() throws Exception {
        TCPRemoteCommitProvider provider = new TCPRemoteCommitProvider();

        // "localhost" always resolves to a loopback address, which is one
        // of this host's interface addresses.
        provider.setBindAddress("localhost");

        Object bindAddress = getPrivateField(provider, "_bindAddress");
        Assert.assertNotNull(bindAddress);
        // The resolved address must be one of this host's addresses.
        boolean isLocal = NetworkInterface.networkInterfaces()
            .flatMap(NetworkInterface::inetAddresses)
            .anyMatch(bindAddress::equals);
        Assert.assertTrue("Bind address " + bindAddress + " is not a local address", isLocal);
    }

    @Test
    public void testSetBindAddressNonLocalAddressThrows() throws Exception {
        TCPRemoteCommitProvider provider = new TCPRemoteCommitProvider();

        try {
            provider.setBindAddress(NON_LOCAL_ADDRESS);
            Assert.fail("Expected UserException for a non-local bind address");
        } catch (UserException ue) {
            Assert.assertTrue("Unexpected message: " + ue.getMessage(), ue.getMessage().contains(NON_LOCAL_ADDRESS));
            // Nothing must have been set.
            Assert.assertNull(getPrivateField(provider, "_bindAddress"));
        }
    }

    @Test
    public void testSetBindAddressUnresolvableHostThrows() throws Exception {
        TCPRemoteCommitProvider provider = null;
        try {
            provider = new TCPRemoteCommitProvider();
        } catch (UnknownHostException uhe) {
            Assert.fail("Could not construct TCPRemoteCommitProvider: " + uhe);
        }

        try {
            provider.setBindAddress(UNRESOLVABLE_HOST);
            Assert.fail("Expected UnknownHostException for an unresolvable host name");
        } catch (UnknownHostException uhe) {
            Assert.assertTrue("Unexpected message: " + uhe.getMessage(), uhe.getMessage().contains(UNRESOLVABLE_HOST));
            Assert.assertNull(getPrivateField(provider, "_bindAddress"));
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
    public void testSetAddressesMultiplePeers() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        provider.setAddresses("192.0.2.1:5671;198.51.100.2:5672");

        Assert.assertEquals(List.of("192.0.2.1:5671", "198.51.100.2:5672"), addressStrings(provider));
    }

    @Test
    public void testSetAddressesWithoutPortUsesDefaultPort() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        provider.setAddresses(NON_LOCAL_ADDRESS);

        Assert.assertEquals(List.of(NON_LOCAL_ADDRESS + ":5636"), addressStrings(provider));
    }

    @Test
    public void testSetAddressesExcludesSelf() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        // 127.0.0.1 is a loopback address of this host and the port matches
        // the provider's default port, so it must be excluded.
        provider.setAddresses("127.0.0.1:5636;" + NON_LOCAL_ADDRESS + ":5636");

        Assert.assertEquals(List.of(NON_LOCAL_ADDRESS + ":5636"), addressStrings(provider));
    }

    @Test
    public void testSetAddressesLocalAddressOtherPortIsIncluded() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        // Same host as above, but a different port, so it must be kept.
        provider.setAddresses("127.0.0.1:5671");

        Assert.assertEquals(List.of("127.0.0.1:5671"), addressStrings(provider));
    }

    @Test
    public void testSetAddressesReplacesPreviousAddresses() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        provider.setAddresses(NON_LOCAL_ADDRESS + ":5671");
        provider.setAddresses("198.51.100.9:5673");

        Assert.assertEquals(List.of("198.51.100.9:5673"), addressStrings(provider));
    }

    @Test
    public void testSetAddressesUnresolvableHostThrows() throws Exception {
        TCPRemoteCommitProvider provider = createProvider();

        try {
            provider.setAddresses(UNRESOLVABLE_HOST + ":5671");
            Assert.fail("Expected UnknownHostException for an unresolvable host name");
        } catch (UnknownHostException uhe) {
            Assert.assertTrue("Unexpected message: " + uhe.getMessage(), uhe.getMessage().contains(UNRESOLVABLE_HOST));
        }
    }
}
