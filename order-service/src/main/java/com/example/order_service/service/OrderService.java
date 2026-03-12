package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.PaymentRequest;
import com.example.order_service.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final RestClient restClient;
    private final String inventoryServiceUrl;
    private final String paymentServiceUrl;

    public OrderService(
            RestClient.Builder restClientBuilder,
            @Value("${external.inventory-service.url}") String inventoryServiceUrl,
            @Value("${external.payment-service.url}") String paymentServiceUrl) {
        this.restClient = restClientBuilder.build();
        this.inventoryServiceUrl = inventoryServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    public String placeOrder(OrderRequest orderRequest) {
        String orderId = UUID.randomUUID().toString();
        log.info("Placing order {} for product {}", orderId, orderRequest.productId());

        // Check Inventory (Synchronous HTTP GET call)
        Boolean inStock = restClient.get()
                .uri(inventoryServiceUrl + "/api/inventory/" + orderRequest.productId() + "?quantity=" + orderRequest.quantity())
                .retrieve()
                .body(Boolean.class);

        if (Boolean.FALSE.equals(inStock)) {
            throw new RuntimeException("Product is out of stock!");
        }

        // Process Payment (Synchronous HTTP POST call)
        PaymentRequest paymentRequest = new PaymentRequest(orderId, orderRequest.price() * orderRequest.quantity());
        PaymentResponse paymentResponse = restClient.post()
                .uri(paymentServiceUrl + "/api/payment")
                .body(paymentRequest)
                .retrieve()
                .body(PaymentResponse.class);

        if (paymentResponse == null || !"APPROVED".equals(paymentResponse.status())) {
            throw new RuntimeException("Payment failed!");
        }

        log.info("Order {} successfully placed with Payment ID {}", orderId, paymentResponse.paymentId());
        return "Order Placed Successfully. Order ID: " + orderId;
    }
}