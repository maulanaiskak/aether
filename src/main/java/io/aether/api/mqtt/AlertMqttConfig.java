package io.aether.api.mqtt;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.messaging.MessageChannel;

@Configuration
class AlertMqttConfig {

    @Bean
    Mqttv5PahoMessageHandler alertOutboundHandler(
            @Value("${aether.mqtt.broker-url}") String brokerUrl) {
        var options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setAutomaticReconnect(true);
        var handler = new Mqttv5PahoMessageHandler(options, "aerator-01-alert");
        handler.setDefaultQos(1);
        handler.setAsync(true);
        return handler;
    }

    @Bean
    IntegrationFlow alertOutboundFlow(Mqttv5PahoMessageHandler alertOutboundHandler) {
        return IntegrationFlow.from(alertOutboundChannel())
                .handle(alertOutboundHandler)
                .get();
    }

    @Bean
    MessageChannel alertOutboundChannel() {
        return new DirectChannel();
    }
}
