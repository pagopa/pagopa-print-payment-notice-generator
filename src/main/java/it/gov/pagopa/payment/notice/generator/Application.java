package it.gov.pagopa.payment.notice.generator;

import com.mongodb.client.MongoClient;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableSchedulerLock(defaultLockAtMostFor = "1m")
public class Application {
    @Value("${spring.data.mongodb.database}")
    private String database;


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Bean
    public LockProvider lockProvider(MongoClient mongo) {
        return new MongoLockProvider(mongo.getDatabase(database));
    }
}
