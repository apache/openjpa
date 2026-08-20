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
package org.apache.openjpa.persistence.event;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.openjpa.event.JMSRemoteCommitProvider;
import org.apache.openjpa.persistence.event.common.apps.RuntimeTest1;

/**
 * So named to prevent the autobuild from running this -- we don't
 * have a JMS provider up and running in the autobuild currently.
 */
public class JMSRemoteEventsTest extends RemoteEventBase {
	
	private static final Logger logger = Logger.getLogger(JMSRemoteEventsTest.class.getCanonicalName());

    public JMSRemoteEventsTest(String s) {
        super(s);
    }

    @Override
    public void setUp() {
        deleteAll(RuntimeTest1.class);
    }

    public void testJMSEvents() {
        doTest(JMSRemoteCommitProvider.class,
            "Topic=topic/KodoCommitProviderTopic",
            "Topic=topic/KodoCommitProviderTopic");
    }

    public static void main(String[] args)
        throws Exception {
        Context ctx = new InitialContext();
        TopicConnectionFactory tcf =
            (TopicConnectionFactory) ctx.lookup("java:/ConnectionFactory");
        Topic topic = (Topic) ctx.lookup("topic/KodoCommitProviderTopic");
        ctx.close();

        TopicConnection connection = tcf.createTopicConnection();

        // false == not transacted.
        TopicSession session = connection.createTopicSession
            (false, Session.AUTO_ACKNOWLEDGE);

        // create a subscriber.
        TopicSubscriber s = session.createSubscriber(topic, null,
            /* noLocal: */ false);
        s.setMessageListener(new DebugMessageListener());
        connection.start();
        System.out.println
            ("started listening on topic/KodoCommitProviderTopic");
    }

    private static class DebugMessageListener
        implements MessageListener {

        @Override
        public void onMessage(Message m) {
            try {
                if (m instanceof ObjectMessage) {
                    ObjectMessage om = (ObjectMessage) m;
                    logger.fine("received object: " + om.getObject());
                } else {
                	logger.fine("received bad message: " + m);
                }
            }
            catch (JMSException e) {
            	logger.fine("Exception while processing message");
                e.printStackTrace(System.err);
            }
        }
    }
}
