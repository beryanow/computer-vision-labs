package ru.nsu.g.beryanov.behaviour;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public interface MouseClickedListener extends MouseListener {
    @Override
    public default void mousePressed(MouseEvent e) {}

    @Override
    public default void mouseReleased(MouseEvent e) {}

    @Override
    public default void mouseEntered(MouseEvent e) {}

    @Override
    public default void mouseExited(MouseEvent e) {}
}
