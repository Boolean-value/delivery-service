package com.exercise.delivery.config;

import com.exercise.delivery.model.dto.DeliveryLetterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeliveryKafkaConsumerConfig {

    @Value("${app.kafka.topics.delivery-topic-dlt}")
    private String topic;
    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, DeliveryLetterDto> deliveryConsumerFactory() {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DeliveryLetterDto.class.getName());

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(DeliveryLetterDto.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryLetterDto> deliveryKafkaListenerFactory(
            ConsumerFactory<String, DeliveryLetterDto> deliveryConsumerFactory,
            KafkaTemplate<String, DeliveryLetterDto> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryLetterDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(topic, record.partition())
        );

        DefaultErrorHandler handler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        handler.addNotRetryableExceptions(IllegalArgumentException.class, DeserializationException.class);
        handler.setRetryListeners((record, ex, attempt) -> {
            log.warn("Retry attempt {} for topic={}, partition={}, offset={}",
                    attempt, record.topic(), record.partition(), record.offset(), ex);
        });

        factory.setConsumerFactory(deliveryConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(handler);
        factory.getContainerProperties().setPollTimeout(500);
        factory.getContainerProperties().setLogContainerConfig(true);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }
}
