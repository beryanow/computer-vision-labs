package ru.nsu.g.beryanov.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ImageTreeNode {
    private Integer index;
    private Integer parentIndex;
    private Integer segmentNumber;
    private Integer level;
    private Integer[] coordinates;

    private Integer[][] imagePart;
    private ImageTreeNode[] childrenNodes;
}
