package ua.delsix.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.delsix.RabbitQueue;

@Configuration
public class RabbitConfiguration {
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue MESSAGE_UPDATE() {
        return new Queue(RabbitQueue.MESSAGE_UPDATE);
    }

    @Bean
    public Queue ANSWER_UPDATE() {
    return new Queue(RabbitQueue.ANSWER_UPDATE);
    }

    @Bean
    public Queue EDIT_ANSWER_UPDATE() {
        return new Queue(RabbitQueue.EDIT_ANSWER_UPDATE);
    }
}
