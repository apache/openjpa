/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.event;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public abstract class DynamicTCPRemoteCommitProvider extends TCPRemoteCommitProvider {

    private int _cacheDurationMillis = 30000;

    public DynamicTCPRemoteCommitProvider() throws UnknownHostException {
        super();
    }

    public int getCacheDurationMillis() {
        return _cacheDurationMillis;
    }

    public void setCacheDurationMillis(final int _cacheDurationMillis) {
        this._cacheDurationMillis = _cacheDurationMillis;
    }

    @Override
    public final void setAddresses(final String names) throws UnknownHostException {
        throw new UnknownHostException("Do not set Addresses on this instance; "
                + "did you expect " + TCPRemoteCommitProvider.class.getSimpleName() + " ?");
    }

    @Override
    public void endConfiguration() {
        TcpAddressesUpdater updater = new TcpAddressesUpdater();
        updater.run();

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(updater, 0, _cacheDurationMillis);

        super.endConfiguration();
    }

    protected abstract List<String> fetchDynamicAddresses();

    private class TcpAddressesUpdater extends TimerTask {

        @Override
        public void run() {
            List<String> dynamicAddresses = fetchDynamicAddresses();

            _addressesLock.lock();
            try {
                String localhostAddress = InetAddress.getLocalHost().getHostAddress();

                for (String dynamic : dynamicAddresses) {
                    InetAddress tmpAddress = InetAddress.getByName(dynamic);

                    if (localhostAddress.equals(dynamic)) {
                        // This string matches the hostname for for ourselves, we
                        // don't actually need to send ourselves messages.
                        if (log.isTraceEnabled()) {
                            log.trace(s_loc.get("tcp-address-asself", tmpAddress.getHostAddress() + ":" + _port));
                        }
                    } else {
                        HostAddress podAddress = new HostAddress(dynamic);
                        if (_addresses.contains(podAddress)) {
                            if (log.isTraceEnabled()) {
                                log.trace(s_loc.get("dyntcp-address-not-set",
                                        podAddress.getAddress().getHostAddress() + ":" + podAddress.getPort()));
                            }
                        } else {
                            _addresses.add(podAddress);

                            if (log.isTraceEnabled()) {
                                log.trace(s_loc.get("dyntcp-address-set",
                                        podAddress.getAddress().getHostAddress() + ":" + podAddress.getPort()));
                            }
                        }
                    }
                }

                List<HostAddress> toCloseAndRemove = _addresses.stream().
                        filter(address -> !dynamicAddresses.contains(address.getAddress().getHostAddress())).
                        collect(Collectors.toList());
                toCloseAndRemove.forEach(address -> {
                    address.close();
                    _addresses.remove(address);

                    if (log.isTraceEnabled()) {
                        log.trace(s_loc.get("tcp-address-unset",
                                address.getAddress().getHostAddress() + ":" + address.getPort()));
                    }
                });
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(s_loc.get("dyntcp-updater-error"), e);
                }
            } finally {
                _addressesLock.unlock();
            }
        }
    }
}
