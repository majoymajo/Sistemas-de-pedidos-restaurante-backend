package com.restaurant.kitchenworker.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Kitchen Worker.
 * Configures the topic exchange, queues with Dead Letter Queue support, bindings, 
 * and message converter for order event consumption.
 * 
 * This configuration ensures reliable message processing with automatic retry and 
 * dead letter handling for failed messages.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.routing-key.order-placed}")
    private String orderPlacedRoutingKey;

    @Value("${rabbitmq.dlq.name}")
    private String dlqName;

    @Value("${rabbitmq.dlq.exchange}")
    private String dlxName;

    @Value("${rabbitmq.dlq.routing-key}")
    private String dlqRoutingKey;

    /**
     * Declares the topic exchange for order events.
     * Topic exchanges route messages to queues based on routing key patterns.
     * This exchange is shared between order-service (producer) and kitchen-worker (consumer).
     * 
     * @return TopicExchange configured as durable
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * Declares the Dead Letter Exchange for failed messages.
     * Messages that fail processing after all retry attempts will be routed to this exchange.
     * 
     * @return TopicExchange configured as durable for dead letter handling
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlxName, true, false);
    }

    /**
     * Declares the main queue for order.placed events with Dead Letter Exchange configuration.
     * This queue will receive messages published with the "order.placed" routing key.
     * 
     * When a message fails processing after the configured retry attempts (3 attempts with 
     * exponential backoff), it will be automatically routed to the Dead Letter Exchange.
     * 
     * @return Queue configured as durable with DLX settings
     */
    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    /**
     * Declares the Dead Letter Queue for failed order.placed messages.
     * Messages that fail processing after all retry attempts will be stored here 
     * for manual inspection and potential reprocessing.
     * 
     * This queue allows operators to:
     * - Investigate why messages failed
     * - Fix underlying issues
     * - Manually reprocess messages if needed
     * 
     * @return Queue configured as durable
     */
    @Bean
    public Queue orderPlacedDLQ() {
        return new Queue(dlqName, true);
    }

    /**
     * Binds the order.placed queue to the order exchange with the specified routing key.
     * Messages published to the exchange with routing key "order.placed" will be routed to this queue.
     * 
     * @return Binding between queue and exchange
     */
    @Bean
    public Binding orderPlacedBinding() {
        return BindingBuilder
                .bind(orderPlacedQueue())
                .to(orderExchange())
                .with(orderPlacedRoutingKey);
    }

    /**
     * Binds the Dead Letter Queue to the Dead Letter Exchange.
     * Failed messages will be routed from the main queue to the DLX, 
     * and then to this DLQ based on the routing key "order.placed.failed".
     * 
     * @return Binding between DLQ and DLX
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(orderPlacedDLQ())
                .to(deadLetterExchange())
                .with(dlqRoutingKey);
    }

    /**
     * Configures the message converter to use Jackson for JSON serialization/deserialization.
     * This allows automatic conversion of JSON messages to Java objects when consuming messages.
     * Registers JavaTimeModule to support Java 8 date/time types like LocalDateTime.
     * 
     * The retry policy is configured in application.yml:
     * - Max attempts: 3
     * - Initial interval: 1000ms
     * - Multiplier: 2.0 (exponential backoff)
     * - Max interval: 10000ms
     * 
     * @return MessageConverter configured for JSON
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
