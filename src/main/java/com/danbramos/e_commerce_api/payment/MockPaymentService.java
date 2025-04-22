package com.danbramos.e_commerce_api.payment;

import com.danbramos.e_commerce_api.order.Order;
import com.danbramos.e_commerce_api.order.OrderService;
import com.danbramos.e_commerce_api.payment.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A mock service to simulate asynchronous payment processing.
 * It introduces a random delay and randomly determines payment success or failure.
 * It notifies the OrderService upon completion.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MockPaymentService {

    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    /**
     * Simulates processing a payment for an order asynchronously.
     * Waits for a short period, then randomly succeeds or fails based on simple logic.
     * Notifies the OrderService upon completion via callback methods.
     *
     * @param order The order for which payment is being processed.
     */
    @Async
    public void processPayment(Order order) {
        if (order == null || order.getId() == null) {
            log.error("MockPaymentService received null order or order with null ID. Cannot process payment.");
            return;
        }
        PaymentDetails paymentDetails = order.getPaymentDetails();
        if (paymentDetails == null) {
            log.error("MockPaymentService received null payment details. Cannot process payment.");
            return;
        }

        Long orderId = order.getId();
        BigDecimal amount = order.getTotalAmount();

        log.info("Processing payment for Order ID: {} - Amount: {}", orderId, amount);

        try {
            int delaySeconds = random.nextInt(3) + 2;
            log.debug("Order ID: {} - Simulating payment processing delay of {} seconds.", orderId, delaySeconds);
            TimeUnit.SECONDS.sleep(delaySeconds);

            boolean paymentSuccess = true;
            if (amount != null && amount.compareTo(new BigDecimal("500")) > 0) {
                paymentSuccess = random.nextInt(10) < 7;
            } else {
                paymentSuccess = random.nextInt(10) < 9;
            }

            if (paymentSuccess) {
                log.info("Payment SUCCESSFUL for Order ID: {}", orderId);
                String mockTransactionId = "MOCK_TXN_" + System.currentTimeMillis() + "_" + orderId;
                eventPublisher.publishEvent(new PaymentSuccessEvent(orderId, mockTransactionId));
            } else {
                paymentDetails.markAsFailed();
                log.warn("Payment FAILED for Order ID: {}", orderId);
            }
        } catch (Exception e) {
            log.error("Unexpected error during mock payment processing/callback for Order ID: {}: {}", orderId, e.getMessage(), e);
            paymentDetails.markAsFailed();
        }
    }
}