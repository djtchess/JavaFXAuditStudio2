// Fixture de test — ne pas committer de code reel metier
package com.example.sample;

import java.util.List;

/**
 * Exemple de controller avec des identifiants metier, secrets et donnees pour tester
 * le pipeline de sanitisation complet.
 * Author: john.doe@example.com
 */
public class UserAccountService {

    private static final String API_KEY = "my-super-secret-api-key-12345";
    private static final String DB_PASSWORD = "password=changeme123";

    private final OrderRepository orderRepository;
    private final PaymentManager paymentManager;

    public UserAccountService(OrderRepository orderRepository, PaymentManager paymentManager) {
        this.orderRepository = orderRepository;
        this.paymentManager = paymentManager;
    }

    // TODO: remove this before prod — contact admin@company.internal
    public List<String> processUserOrders(String userId) {
        // temporary workaround — see JIRA ticket PROJ-1234
        String internalUrl = "http://192.168.1.100:8080/api/orders";
        return orderRepository.findByUserId(userId);
    }

    public void processPayment(String cardNumber) {
        // card number format: 4111111111111111
        paymentManager.charge(cardNumber, 9876543210L);
    }
}
