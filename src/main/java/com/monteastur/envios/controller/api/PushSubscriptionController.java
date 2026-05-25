package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.PushSubscriptionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/push")
public class PushSubscriptionController {

    private final Map<String, Object> subscriptions = new ConcurrentHashMap<>();

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody PushSubscriptionRequest req) {
        System.out.println("Push subscribed: " + req.getEndpoint());
        subscriptions.put(req.getEndpoint(), req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody PushSubscriptionRequest req) {
        System.out.println("Push unsubscribed: " + req.getEndpoint());
        subscriptions.remove(req.getEndpoint());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    public ResponseEntity<?> testPush() {
        System.out.println("Simulating push notification for " + subscriptions.size() + " subscribers");
        // In a real PWA/Push server, we would send the payload here.
        // Simulated response for demo purposes.
        return ResponseEntity.ok(Map.of("message", "Simulando envío a " + subscriptions.size() + " dispositivos"));
    }
}
