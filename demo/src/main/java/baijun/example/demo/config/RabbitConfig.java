package baijun.example.demo.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PUBLIC_EXCHANGE = "chat.public.exchange";
    public static final String PRIVATE_EXCHANGE = "chat.private.exchange";

    @Bean
    public FanoutExchange publicExchange() {
        return new FanoutExchange(PUBLIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue publicQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding publicBinding(Queue publicQueue, FanoutExchange publicExchange) {
        return BindingBuilder.bind(publicQueue).to(publicExchange);
    }

    @Bean
    public DirectExchange privateExchange() {
        return new DirectExchange(PRIVATE_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.addTrustedPackages("baijun.example.demo.dto");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
