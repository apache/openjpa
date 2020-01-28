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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.openjpa.event.TCPRemoteCommitProvider;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.log.SLF4JLogFactory;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class KubernetesTCPRemoteCommitProviderTest {

    private static final String NAMESPACE = "ns1";

    private static final String LABEL = "testKey";

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    private Pod pod1;

    private Pod pod2;

    private Pod pod3;

    private Pod pod4;

    @Before
    public void setupKubernetes() {
        KubernetesClient client = server.getClient();

        pod1 = new PodBuilder().
                withNewMetadata().
                withName("pod1").
                addToLabels(LABEL, "value1").
                endMetadata().
                withStatus(new PodStatusBuilder().withPodIP("1.1.1.1").build()).
                build();
        client.pods().inNamespace(NAMESPACE).create(pod1);

        pod2 = new PodBuilder().
                withNewMetadata().
                withName("pod2").
                addToLabels(LABEL, "value2").
                endMetadata().
                withStatus(new PodStatusBuilder().withPodIP("2.2.2.2").build()).
                build();
        client.pods().inNamespace(NAMESPACE).create(pod2);

        pod3 = new PodBuilder().
                withNewMetadata().
                withName("pod3").
                endMetadata().
                withStatus(new PodStatusBuilder().withPodIP("3.3.3.3").build()).
                build();
        client.pods().inNamespace("ns2").create(pod3);

        pod4 = new PodBuilder().
                withNewMetadata().
                withName("pod4").
                addToLabels("other", "value1").
                endMetadata().
                withStatus(new PodStatusBuilder().withPodIP("4.4.4.4").build()).
                build();
        client.pods().inNamespace(NAMESPACE).create(pod4);

        PodList podList = client.pods().inNamespace(NAMESPACE).withLabel(LABEL).list();
        assertNotNull(podList);
        assertEquals(2, podList.getItems().size());
        assertTrue(podList.getItems().contains(pod1));
        assertTrue(podList.getItems().contains(pod2));
    }

    @SuppressWarnings("unchecked")
    private List<String> getAddresses(final KubernetesTCPRemoteCommitProvider rcp)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Field _addresses = TCPRemoteCommitProvider.class.getDeclaredField("_addresses");
        _addresses.setAccessible(true);

        return new ArrayList<>((List<Object>) _addresses.get(rcp)).stream().
                map(ReflectionToStringBuilder::toString).
                map(address -> StringUtils.substringAfter(address, "_address=/")).
                map(address -> StringUtils.substringBefore(address, ",")).
                collect(Collectors.toList());
    }

    @Test
    public void addresses() throws UnknownHostException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException, InterruptedException {

        // prepare KubernetesTCPRemoteCommitProvider instance, inject mocked Kubernetes client
        KubernetesTCPRemoteCommitProvider rcp = new KubernetesTCPRemoteCommitProvider() {

            @Override
            protected KubernetesClient kubernetesClient() throws KubernetesClientException {
                return server.getClient();
            }
        };
        rcp.setNamespace(NAMESPACE);
        rcp.setLabel(LABEL);
        rcp.setCacheDurationMillis(500);

        // mock OpenJPA configuration
        Configuration conf = context.mock(Configuration.class);
        context.checking(new Expectations() {

            {
                oneOf(conf).getLog(with(any(String.class)));
                will(returnValue(new SLF4JLogFactory().getLog("")));
            }
        });
        rcp.setConfiguration(conf);

        // finalize
        rcp.endConfiguration();

        // expect to find remote addresses of matching pods
        List<String> addresses = getAddresses(rcp);
        assertEquals(2, addresses.size());
        assertTrue(addresses.contains(pod1.getStatus().getPodIP()));
        assertTrue(addresses.contains(pod2.getStatus().getPodIP()));

        Thread.sleep(500);

        addresses = getAddresses(rcp);
        assertEquals(2, addresses.size());
        assertTrue(addresses.contains(pod1.getStatus().getPodIP()));
        assertTrue(addresses.contains(pod2.getStatus().getPodIP()));
    }
}
