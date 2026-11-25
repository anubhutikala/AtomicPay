package com.atomicpay.backend.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.atomicpay.backend.config.RabbitMQConfig;
import com.atomicpay.backend.model.PaymentMessage;

@Component
public class PaymentConsumer {

    private final PaymentService paymentService;
    private final WorkerMonitor workerMonitor;

    public PaymentConsumer(
            PaymentService paymentService,
            WorkerMonitor workerMonitor) {

        this.paymentService = paymentService;
        this.workerMonitor = workerMonitor;
    }

    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_QUEUE,
            messageConverter = "#{jsonMessageConverter}"
    )
    public void receivePayment(PaymentMessage message) {

        String workerName = Thread.currentThread().getName();

        workerMonitor.paymentStarted(workerName);

        try {
            System.out.println(
                    workerName + " received payment: "
                    + message.getOrderId());

            PaymentResult result = paymentService.processPayment(
                    message.getOrderId(),
                    message.getSenderId(),
                    message.getReceiverId(),
                    message.getAmount(),
                    workerName
            );

            if (result.isSuccess()) {

                workerMonitor.paymentSucceeded();

                System.out.println(
                        workerName + " completed payment: "
                        + message.getOrderId());

            } else {

                workerMonitor.paymentFailed();

                System.out.println(
                        workerName + " payment failed: "
                        + result.getMessage());
            }

        } catch (RuntimeException e) {

            workerMonitor.paymentFailed();

            System.err.println(
                    "\n========== PAYMENT PROCESSING ERROR ==========");

            System.err.println("Worker: " + workerName);
            System.err.println("Order ID: " + message.getOrderId());
            System.err.println("Sender: " + message.getSenderId());
            System.err.println("Receiver: " + message.getReceiverId());
            System.err.println("Amount: " + message.getAmount());

            System.err.println("\nException details:");
            e.printStackTrace();

            System.err.println(
                    "================================================\n");

            // Allow RabbitMQ retry and dead-letter handling.
            throw e;

        } finally {

            workerMonitor.paymentFinished();
        }
    }
}