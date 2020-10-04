package ru.nsu.g.beryanov;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ComputerVisionApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ComputerVisionApplication.class)
                .headless(false)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
