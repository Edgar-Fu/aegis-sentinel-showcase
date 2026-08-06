package com.aegis.showcase.model;

public record BoundingBox(double x, double y, double width, double height) {
    public BoundingBox {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width) || !Double.isFinite(height)
                || x < 0.0 || y < 0.0 || width <= 0.0 || height <= 0.0
                || x + width > 1.0001 || y + height > 1.0001) {
            throw new IllegalArgumentException("Bounding boxes must use finite normalized coordinates.");
        }
    }
}
