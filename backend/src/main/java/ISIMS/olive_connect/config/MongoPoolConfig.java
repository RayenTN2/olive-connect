package ISIMS.olive_connect.config;

import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// A new Atlas connection takes 0.2 to 0.6 s to open, and the driver opens only 2 at a time by default.
// Keep a few ready, so the first dashboard after a start doesn't wait for them (measured: up to 0.9 s).
@Configuration
public class MongoPoolConfig {

    @Bean
    MongoClientSettingsBuilderCustomizer warmConnectionPool() {
        return settings -> settings.applyToConnectionPoolSettings(pool -> pool
                .minSize(5)
                .maxConnecting(5));
    }
}
