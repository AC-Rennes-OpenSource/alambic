/*******************************************************************************
 * Copyright (C) 2019-2020 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.gouv.education.acrennes.alambic.amqp.clients;

import java.io.IOException;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.education.acrennes.alambic.amqp.broker.EmbeddedBroker;
import junit.framework.Assert;

public class AMQPQpidTest {

	private static final String INITIAL_BROKER_CONNEXION_CONFIG_PATH = "data/qpid/qpid-broker.properties";

	private static EmbeddedBroker brokerStarter;

	@BeforeClass
	public static void startup() throws Exception {
		brokerStarter = new EmbeddedBroker();
		brokerStarter.startBroker();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		brokerStarter.stopBroker();
	}

	@Before
	public void setUp() throws IOException {
	}

	/**
	 * - QPID embedded Broker
	 * - QPID client + Java JMS
	 * */
	@Test
	public void test() throws Exception {
		final String MESSAGE_BODY = "Hello all folks!";
		
		Connection connection = null;
		Context context = null;

		try {
			Properties properties = new Properties();
			properties.load(AMQPQpidTest.class.getClassLoader().getResourceAsStream(INITIAL_BROKER_CONNEXION_CONFIG_PATH));
			context = new InitialContext(properties);

			ConnectionFactory connFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
			connection = connFactory.createConnection();
			connection.start();

			Session session=connection.createSession(false,Session.CLIENT_ACKNOWLEDGE);
			Destination destination = (Destination) context.lookup("topicExchange");

			MessageProducer messageProducer = session.createProducer(destination);
			MessageConsumer messageConsumer = session.createConsumer(destination);

			TextMessage message = session.createTextMessage(MESSAGE_BODY);
			messageProducer.send(message);

			message = (TextMessage)messageConsumer.receive();
			Assert.assertEquals(MESSAGE_BODY, message.getText());
			
			// explicit acknowledge receipt of the message
			message.acknowledge();			
			connection.stop();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (null != context) {
				context.close();
			}
		}
	}

}