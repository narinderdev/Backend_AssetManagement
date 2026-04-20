package com.example.eam.Config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;

import java.util.Arrays;

@Configuration
@ConditionalOnProperty(prefix = "iot.mqtt", name = "enabled", havingValue = "true")
public class IotMqttConfig {

    public static final String IOT_MQTT_INPUT_CHANNEL = "iotMqttInputChannel";

    @Value("${iot.mqtt.url}")
    private String brokerUrl;

    @Value("${iot.mqtt.client-id:eam-iot-ingest}")
    private String clientId;

    @Value("${iot.mqtt.topic-filter:iot/telemetry/#}")
    private String topicFilter;

    @Value("${iot.mqtt.username:}")
    private String username;

    @Value("${iot.mqtt.password:}")
    private String password;

    @Value("${iot.mqtt.qos:1}")
    private int qos;

    @Value("${iot.mqtt.completion-timeout-ms:5000}")
    private long completionTimeoutMs;

    @Bean(name = IOT_MQTT_INPUT_CHANNEL)
    public MessageChannel iotMqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoClientFactory iotMqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        if (!username.isBlank()) {
            options.setUserName(username);
        }
        if (!password.isBlank()) {
            options.setPassword(password.toCharArray());
        }
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageProducer iotMqttInboundAdapter(MqttPahoClientFactory iotMqttClientFactory,
                                                 MessageChannel iotMqttInputChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-in",
                iotMqttClientFactory,
                resolveTopicFilters()
        );
        adapter.setCompletionTimeout(completionTimeoutMs);
        adapter.setQos(qos);
        adapter.setOutputChannel(iotMqttInputChannel);
        return adapter;
    }

    private String[] resolveTopicFilters() {
        return Arrays.stream(topicFilter.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
