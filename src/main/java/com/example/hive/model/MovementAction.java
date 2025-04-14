package com.example.hive.model;

import java.util.Objects;

public class MovementAction extends MoveAction {
    private final HexCoordinate from;
    private final HexCoordinate to;

    public MovementAction(HexCoordinate from, HexCoordinate to) {
        this.from = from;
        this.to = to;
    }

    public HexCoordinate getFrom() {
        return from;
    }

    public HexCoordinate getTo() {
        return to;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        MovementAction that = (MovementAction) object;
        return Objects.equals(getFrom(), that.getFrom()) && Objects.equals(getTo(), that.getTo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFrom(), getTo());
    }

    @Override
    public boolean isPlacement() {
        return false;
    }

    @Override
    public String toString() {
        return "MovementAction{from=" + from + ", to=" + to + "}";
    }
}

