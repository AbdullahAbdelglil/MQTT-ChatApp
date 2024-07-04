package com.example.MQTT;

import jakarta.websocket.server.PathParam;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Queue;


@org.springframework.stereotype.Controller
@RequestMapping("/chat")
public class Controller {
    private Client client;
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    @GetMapping("/login")
    public ResponseEntity<String> loginToChat(@PathParam("username") String username) {
        try {
            client = new Client(username);
            return ResponseEntity.ok("Logged in as " + username);
        } catch (MqttException e) {
            logger.error("Failed to login", e);
            return ResponseEntity.status(500).body("Failed to login: " + e.getMessage());
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publish(@RequestBody RequestDTO req) {
        if (client == null) {
            return ResponseEntity.status(400).body("Not logged in");
        }
        try {
            client.publish(req.getMessage(), req.getTopic(), req.getQos());
            return ResponseEntity.ok("Message published to topic " + req.getTopic());
        } catch (Exception e) {
            logger.error("Failed to publish message", e);
            return ResponseEntity.status(500).body("Failed to publish message: " + e.getMessage());
        }
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestBody RequestDTO req) {
        if (client == null) {
            return ResponseEntity.status(400).body("Not logged in");
        }
        try {
            client.subscribe(req.getTopic(), req.getQos());
            return ResponseEntity.ok("Subscribed to topic " + req.getTopic());
        } catch (Exception e) {
            logger.error("Failed to subscribe to topic", e);
            return ResponseEntity.status(500).body("Failed to subscribe to topic: " + e.getMessage());
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<String> listMessages(@PathParam("topic") String topic) {
        if (client == null) {
            return ResponseEntity.status(400).body("Not logged in");
        }
        return ResponseEntity.status(200).body(client.listMessagesOfTopic(topic).toString());
    }

    @GetMapping("/leave")
    public ResponseEntity<String> closeConnection() {
        if (client == null) {
            return ResponseEntity.status(400).body("Not logged in");
        }
        try {
            client.disconnectClient();
            return ResponseEntity.ok("Disconnected from broker");
        } catch (Exception e) {
            logger.error("Failed to disconnect", e);
            return ResponseEntity.status(500).body("Failed to disconnect: " + e.getMessage());
        }
    }
}
