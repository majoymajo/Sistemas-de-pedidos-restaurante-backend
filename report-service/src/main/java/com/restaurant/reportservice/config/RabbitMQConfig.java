package com.restaurant.reportservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for report-service.
 * Declares queues, exchanges, bindings, and DLQ configuration.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.routing-key.order-placed}")
    private String routingKeyOrderPlaced;

    @Value("${rabbitmq.queue.order-ready.name}")
    private String orderReadyQueueName;

    @Value("${rabbitmq.routing-key.order-ready}")
    private String routingKeyOrderReady;

    @Value("${rabbitmq.dlq.name}")
    private String dlqName;

    @Value("${rabbitmq.dlq.exchange}")
    private String dlxName;

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue reportQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqName)
                .build();
    }

    @Bean
    public Queue orderReadyReportQueue() {
        return QueueBuilder.durable(orderReadyQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqName)
                .build();
    }

    @Bean
    public Queue reportDlq() {
        return new Queue(dlqName, true);
    }

    @Bean
    public DirectExchange reportDlx() {
        return new DirectExchange(dlxName);
    }

    @Bean
    public Binding reportQueueBinding() {
        return BindingBuilder
                .bind(reportQueue())
                .to(orderExchange())
                .with(routingKeyOrderPlaced);
    }

    @Bean
    public Binding orderReadyReportQueueBinding() {
        return BindingBuilder
                .bind(orderReadyReportQueue())
                .to(orderExchange())
                .with(routingKeyOrderReady);
    }

    @Bean
    public Binding reportDlqBinding() {
        return BindingBuilder
                .bind(reportDlq())
                .to(reportDlx())
                .with(dlqName);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
