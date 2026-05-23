package io.aether.ingestion.mqtt;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.messaging.MessageChannel;

@Configuration
class MqttIntegrationConfig {

    @Bean
    Mqttv5PahoMessageHandler mqttOutboundHandler(
            @Value("${aether.mqtt.broker-url}") String brokerUrl) {
        var options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        var handler = new Mqttv5PahoMessageHandler(options, "aether-01-pub");
        handler.setDefaultQos(1);
        handler.setAsync(true);
        return handler;
    }

    @Bean
    IntegrationFlow mqttOutboundFlow(Mqttv5PahoMessageHandler mqttOutboundHandler) {
        return IntegrationFlow.from(mqttOutboundChannel())
                .handle(mqttOutboundHandler)
                .get();
    }

    @Bean
    MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }
}
