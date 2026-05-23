package io.aether.processing.mqtt;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;

@Configuration
class MqttConsumerConfig {

    @Bean
    Mqttv5PahoMessageDrivenChannelAdapter mqttInboundAdapter(
            @Value("${aether.mqtt.broker-url}") String brokerUrl) {
        var options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        var adapter = new Mqttv5PahoMessageDrivenChannelAdapter(options, "aether-01-sub", "sensors/#");
        adapter.setQos(1);
        return adapter;
    }

    @Bean
    IntegrationFlow mqttInboundFlow(
            Mqttv5PahoMessageDrivenChannelAdapter mqttInboundAdapter,
            MqttReadingConsumer consumer) {
        return IntegrationFlow.from(mqttInboundAdapter)
                .handle(consumer)
                .get();
    }
}
