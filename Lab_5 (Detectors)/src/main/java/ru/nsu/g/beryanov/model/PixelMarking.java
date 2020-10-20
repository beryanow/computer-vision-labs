package ru.nsu.g.beryanov.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PixelMarking {
    private boolean labeled = false;
    private int label = 0;
}
