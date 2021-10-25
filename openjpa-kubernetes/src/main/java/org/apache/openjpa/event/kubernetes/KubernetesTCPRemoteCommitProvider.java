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
package org.apache.openjpa.event.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.openjpa.event.DynamicTCPRemoteCommitProvider;
import org.apache.openjpa.lib.util.Localizer;

public class KubernetesTCPRemoteCommitProvider extends DynamicTCPRemoteCommitProvider {

    private static final Localizer s_loc = Localizer.forPackage(KubernetesTCPRemoteCommitProvider.class);

    private String _namespace = "<namespace>";

    private String _label = "<label>";

    public KubernetesTCPRemoteCommitProvider() throws UnknownHostException {
        super();
    }

    public String getNamespace() {
        return _namespace;
    }

    public void setNamespace(final String namespace) {
        this._namespace = namespace;
    }

    public String getLabel() {
        return _label;
    }

    public void setLabel(final String label) {
        this._label = label;
    }

    protected KubernetesClient kubernetesClient() throws KubernetesClientException {
        return new DefaultKubernetesClient();
    }

    @Override
    protected List<String> fetchDynamicAddresses() {
        List<String> podIPs = new ArrayList<>();

        try (KubernetesClient client = kubernetesClient()) {
            podIPs.addAll(client.pods().inNamespace(_namespace).withLabel(_label).list().
                    getItems().stream().
                    filter(Readiness::isPodReady).
                    map(pod -> pod.getStatus().getPodIP()).
                    collect(Collectors.toList()));

            if (log.isTraceEnabled()) {
                log.trace(s_loc.get("kubernetestcp-pods", podIPs));
            }
        } catch (KubernetesClientException e) {
            if (log.isFatalEnabled()) {
                log.fatal(s_loc.get("kubernetestcp-error"), e);
            }
        }

        return podIPs;
    }
}
