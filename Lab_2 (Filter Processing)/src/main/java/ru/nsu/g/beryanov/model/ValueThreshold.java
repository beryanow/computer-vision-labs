package ru.nsu.g.beryanov.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

@Getter
@Setter
public class ValueThreshold {
    @Value("false")
    private boolean isStrong;
    @Value("false")
    private boolean isWeak;
    private double value;
}
