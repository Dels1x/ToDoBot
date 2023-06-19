package ua.delsix.configuration;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import ua.delsix.language.LanguageManager;

@Configuration
public class LanguageConfiguration {

    @Bean
    public MessageSource messageSource() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ClassPathResource("messages.yml"));

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setCommonMessages(yamlPropertiesFactoryBean.getObject());
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LanguageManager languageManager(MessageSource messageSource) {
        return new LanguageManager(messageSource);
    }
}
