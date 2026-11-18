package com.parkease.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:parkease.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queues.booking:notification.booking.queue}")
    private String bookingQueueName;

    @Value("${app.rabbitmq.queues.payment:notification.payment.queue}")
    private String paymentQueueName;

    @Value("${app.rabbitmq.routing-keys.booking:booking.*}")
    private String bookingRoutingKey;

    @Value("${app.rabbitmq.routing-keys.payment:payment.*}")
    private String paymentRoutingKey;

    @Bean
    public TopicExchange parkeaseExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue bookingQueue() {
        return QueueBuilder.durable(bookingQueueName).build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(paymentQueueName).build();
    }

    @Bean
    public Binding bookingBinding(Queue bookingQueue, TopicExchange parkeaseExchange) {
        return BindingBuilder.bind(bookingQueue)
                .to(parkeaseExchange)
                .with(bookingRoutingKey);
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange parkeaseExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(parkeaseExchange)
                .with(paymentRoutingKey);
    }

    // Modern, non-deprecated replacement in Spring Boot 4.x / Spring AMQP 4.x
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
