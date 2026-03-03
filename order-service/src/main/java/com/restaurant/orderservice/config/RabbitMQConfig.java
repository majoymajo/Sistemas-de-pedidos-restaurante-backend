package com.restaurant.orderservice.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Order Service.
 * Configures the topic exchange, queues, bindings, and message converter for order event publishing.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.order-placed}")
    private String orderPlacedRoutingKey;

    /**
     * Declares the topic exchange for order events.
     * Topic exchanges route messages to queues based on routing key patterns.
     * 
     * @return TopicExchange configured as durable
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * Configures the message converter to use Jackson for JSON serialization/deserialization.
     * This allows automatic conversion of Java objects to JSON when publishing messages.
     * Registers JavaTimeModule to support Java 8 date/time types like LocalDateTime.
     * 
     * @return MessageConverter configured for JSON
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
