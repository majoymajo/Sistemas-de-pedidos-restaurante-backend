package com.restaurant.reportservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RabbitMQConfig — queue & binding declarations")
class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new RabbitMQConfig();
        setField("exchangeName", "test.order.exchange");
        setField("queueName", "test.order.placed.report.queue");
        setField("routingKeyOrderPlaced", "order.placed");
        setField("dlqName", "test.order.report.dlq");
        setField("dlxName", "test.order.report.dlx");
        setField("orderReadyQueueName", "test.order.ready.report.queue");
        setField("routingKeyOrderReady", "order.ready");
    }

    private void setField(String fieldName, String value) throws Exception {
        Field field = RabbitMQConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(config, value);
    }

    // --- Regression tests for existing order.placed infrastructure ---

    @Test
    @DisplayName("reportQueue bean should be durable with DLQ arguments")
    void reportQueueShouldBeDurableWithDlqArguments() {
        Queue queue = config.reportQueue();
        assertEquals("test.order.placed.report.queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals("test.order.report.dlx", queue.getArguments().get("x-dead-letter-exchange"));
    }

    @Test
    @DisplayName("reportQueueBinding should bind to exchange with order.placed routing key")
    void reportQueueBindingShouldBindCorrectly() {
        Binding binding = config.reportQueueBinding();
        assertEquals("test.order.placed.report.queue", binding.getDestination());
        assertEquals("test.order.exchange", binding.getExchange());
        assertEquals("order.placed", binding.getRoutingKey());
    }

    // --- New tests for order.ready infrastructure ---

    @Test
    @DisplayName("orderReadyReportQueue bean should exist and be durable with DLQ arguments")
    void orderReadyReportQueueShouldExist() {
        Queue queue = config.orderReadyReportQueue();
        assertEquals("test.order.ready.report.queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals("test.order.report.dlx", queue.getArguments().get("x-dead-letter-exchange"));
    }

    @Test
    @DisplayName("orderReadyReportQueueBinding should bind to exchange with order.ready routing key")
    void orderReadyReportQueueBindingShouldBindCorrectly() {
        Binding binding = config.orderReadyReportQueueBinding();
        assertEquals("test.order.ready.report.queue", binding.getDestination());
        assertEquals("test.order.exchange", binding.getExchange());
        assertEquals("order.ready", binding.getRoutingKey());
    }

    @Test
    @DisplayName("orderExchange should be a topic exchange")
    void orderExchangeShouldBeTopicExchange() {
        TopicExchange exchange = config.orderExchange();
        assertEquals("test.order.exchange", exchange.getName());
    }

    @Test
    @DisplayName("DLQ binding routing key should use dlq name, not order.placed key")
    void dlqBindingShouldUseGenericRoutingKey() {
        Binding binding = config.reportDlqBinding();
        assertEquals("test.order.report.dlq", binding.getRoutingKey());
    }
}
