package com.atomicpay.backend.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.atomicpay.backend.config.RabbitMQConfig;
import com.atomicpay.backend.model.PaymentMessage;

@Service
public class PaymentProducer {

    private final RabbitTemplate rabbitTemplate;

    public PaymentProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPayment(PaymentMessage message) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_ROUTING_KEY,
                message
        );

        System.out.println(
                "Payment message sent to RabbitMQ: "
                + message.getOrderId()
        );
    }
}