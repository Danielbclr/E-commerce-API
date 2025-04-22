package com.danbramos.e_commerce_api.order;

import com.danbramos.e_commerce_api.common.Address;
import com.danbramos.e_commerce_api.payment.PaymentDetails;
import com.danbramos.e_commerce_api.payment.PaymentMethod;
import com.danbramos.e_commerce_api.payment.event.PaymentSuccessEvent;
import com.danbramos.e_commerce_api.user.User;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(User user, Address shippingAddress, PaymentMethod paymentMethod, Address billingAddress);
    public void handleOnSuccessfulPayment(PaymentSuccessEvent event);
    Optional<Order> findOrderByIdAndUser(Long orderId, User user);
    List<Order> findAllOrdersByUser(User user);
}
