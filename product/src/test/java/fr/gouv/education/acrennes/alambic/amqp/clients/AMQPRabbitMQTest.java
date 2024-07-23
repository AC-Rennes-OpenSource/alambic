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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import fr.gouv.education.acrennes.alambic.amqp.broker.EmbeddedBroker;
import org.junit.*;

import java.io.IOException;

public class AMQPRabbitMQTest {

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
     * - RabbitMQ client
     * */
    @Test
    public void test() throws Exception {
        final String EXCHANGE_NAME = "exchange";
        final String QUEUE_NAME = "queue";
        final String ROUTING_KEY = "jms/queue";
        final String MESSAGE_BODY = "Hello all folks!";
        final String BROKER_URL = "amqp://guest:guest@/default?brokerlist='tcp://localhost:5672'";

        Connection connection = null;
        Channel channel = null;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(BROKER_URL);

            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "direct", false);
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

            // Post message
            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, MESSAGE_BODY.getBytes());

            // Get message
            GetResponse response = channel.basicGet(QUEUE_NAME, false);
            if (null != response) {
                byte[] body = response.getBody();
                Assert.assertEquals(MESSAGE_BODY, new String(body));

                // explicit acknowledge receipt of the message
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            }

            response = channel.basicGet(QUEUE_NAME, false);
            if (null != response) {
                byte[] body = response.getBody();
                Assert.assertEquals(MESSAGE_BODY, new String(body));

                // explicit acknowledge receipt of the message
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (null != channel) {
                channel.close();
            }
        }
    }

}