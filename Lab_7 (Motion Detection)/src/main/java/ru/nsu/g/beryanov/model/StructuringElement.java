package ru.nsu.g.beryanov.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class StructuringElement {
    private byte[][] data;
}
