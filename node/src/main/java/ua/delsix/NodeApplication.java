package ua.delsix;

import lombok.extern.log4j.Log4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Log4j
public class NodeApplication {
    public static void main(String[] args) {
        log.info("Starting Node Application");
        SpringApplication.run(NodeApplication.class);
    }
}
