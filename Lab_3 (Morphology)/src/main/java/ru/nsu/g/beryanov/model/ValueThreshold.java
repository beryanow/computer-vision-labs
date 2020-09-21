package ru.nsu.g.beryanov.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValueThreshold {
    private boolean isStrong = false;
    private boolean isWeak = false;
    private double value;
}
