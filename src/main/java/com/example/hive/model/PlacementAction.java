package com.example.hive.model;

import java.util.Objects;

public class PlacementAction extends MoveAction {
    private final HexCoordinate destination;

    public PlacementAction(HexCoordinate destination) {
        this.destination = destination;
    }

    public HexCoordinate getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PlacementAction that = (PlacementAction) object;
        return Objects.equals(getDestination(), that.getDestination());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getDestination());
    }

    @Override
    public boolean isPlacement() {
        return true;
    }

    @Override
    public String toString() {
        return "PlacementAction{destination=" + destination + "}";
    }
}

