package com.backend.rootly.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Log4j2
@Configuration
@SuppressWarnings("deprecation")
public class MongoConfig {

    private static final String DIVIDER = "==================================================";

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean validatorFactory) {
        // LocalValidatorFactoryBean implements jakarta.validation.Validator,
        // so passing getValidator() extracts the exact interface required.
        return new ValidatingMongoEventListener(validatorFactory.getValidator());
    }

    @Bean
    public CommandLineRunner testMongoConnection(MongoDatabaseFactory mongoDatabaseFactory) {
        return args -> {
            try {
                String dbName = mongoDatabaseFactory.getMongoDatabase().getName();
                if (log.isInfoEnabled()) {
                    log.info(DIVIDER);
                    log.info(" SUCCESS: Successfully connected to MongoDB Atlas!");
                    log.info(" Connected Database: {}", dbName);
                    log.info(DIVIDER);
                }
            } catch (DataAccessException e) {
                if (log.isErrorEnabled()) {
                    log.error(DIVIDER);
                    log.error(" ERROR: MongoDB Connection Failed!");
                    log.error(" Reason: {}", e.getMessage(), e);
                    log.error(DIVIDER);
                }
            }
        };
    }

    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}
