package com.herobattle;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HeroClashApplication {

    public static void main(String[] args) {
        // Some JVMs report the legacy zone id "Asia/Calcutta", which PostgreSQL 16's tzdata
        // rejects on connect ("invalid value for parameter TimeZone"). Pin to UTC before the
        // JDBC driver starts so this works from any launcher (IDE, jar, mvn spring-boot:run).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(HeroClashApplication.class, args);
    }
}
