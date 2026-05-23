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
    IntegrationFlow mqttInboundFlow(
            @Value("${aether.mqtt.broker-url}") String brokerUrl,
            MqttReadingConsumer consumer) {
        var options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        var adapter = new Mqttv5PahoMessageDrivenChannelAdapter(options, "aether-01-sub", "sensors/#");
        adapter.setQos(1);
        return IntegrationFlow.from(adapter)
                .handle(consumer)
                .get();
    }
}
