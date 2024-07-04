package com.example.MQTT;

import org.eclipse.paho.client.mqttv3.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private final String mqttBrokerUrl =  Constants.MQTT_BROKER_URL;
    private final IMqttClient client;

    //TODO: list to save all received messages
    private Map<String, Queue<MqttMessage>> messages = new HashMap<>();

    public Client(String username) throws MqttException {
        client = new MqttClient(mqttBrokerUrl, username);
        connectClient();
        setClientCallback();
    }

    private void connectClient() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        try {
            client.connect(options);
            logger.info("Connected to MQTT broker: {}", mqttBrokerUrl);
        } catch (MqttException e) {
            logger.error("Failed to connect to MQTT broker: {}", mqttBrokerUrl, e);
            throw e;
        }
    }

    private void setClientCallback() {
        client.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                logger.info("Message arrived. Topic: {}, Qos: {}, Message: {}",
                        topic, message.getQos(), new String(message.getPayload()));
                saveMessage(topic, message);
            }

            @Override
            public void connectionLost(Throwable cause) {
                logger.warn("Connection lost: {}", cause.getMessage());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    MqttMessage message = token.getMessage();
                    if(message == null) {
                        logger.info("Delivery complete, but the message is null (likely due to QoS) ");
                    } else {
                        logger.info("Delivery complete: {}", token.getMessage());
                    }
                } catch (MqttException e) {
                    logger.error("Error in deliveryComplete callback", e);
                }
            }
        });
    }

    public void publish(String message, String topic, int qos) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            client.publish(topic, mqttMessage);
            logger.info("Published message to topic {}: {}, Qos {}", topic, message, qos);
        } catch (MqttException e) {
            logger.error("Failed to publish message to topic {}: {}", topic, message, e);
        }
    }

    public void subscribe(String topic, int qos) {
        try {
            client.subscribe(topic, qos);
            logger.info("Subscribed to topic: {}", topic);
        } catch (MqttException e) {
            logger.error("Failed to subscribe to topic: {}", topic, e);
        }
    }

    public void saveMessage(String topic, MqttMessage message) {
        Queue<MqttMessage> messagesQueue = messages.getOrDefault(topic, new ArrayDeque<>());
        messagesQueue.add(message);
        messages.put(topic, messagesQueue);
    }

    public Queue<MqttMessage> listMessagesOfTopic(String topic) {
        return messages.getOrDefault(topic, null);
    }

    public void disconnectClient() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                logger.info("Disconnected from MQTT broker: {}", mqttBrokerUrl);
            }
        } catch (MqttException e) {
            logger.error("Failed to disconnect from MQTT broker: {}", mqttBrokerUrl, e);
        }
    }
}

