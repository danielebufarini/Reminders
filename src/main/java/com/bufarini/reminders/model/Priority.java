package com.bufarini.reminders.model;

import android.graphics.Color;

import java.io.Serializable;

public class Priority implements Serializable {
    public static final Priority RED = new Priority(1, Color.RED);
    public static final Priority YELLOW = new Priority(2, Color.rgb(234,154,22));
    public static final Priority GREEN = new Priority(3, Color.rgb(6,135,135));
    public static final Priority BLUE = new Priority(4, Color.BLUE);
    public static final Priority CYAN = new Priority(5, Color.rgb(217,91,67));
    public static final Priority MAGENTA = new Priority(6, Color.MAGENTA);
    public static final Priority BLACK = new Priority(7, Color.BLACK);
    public static final Priority NONE = new Priority(8, Color.TRANSPARENT);

    public static final Priority[] PRIORITIES = {
            Priority.NONE, Priority.RED, Priority.YELLOW, Priority.GREEN,
            Priority.BLUE, Priority.CYAN, Priority.MAGENTA, Priority.BLACK
    };

    private int value;
    private int colour;

    public Priority(int value, int colour) {
        this.value = value;
        this.colour = colour;
    }

    public int getPriority() {
        return value;
    }

    public int getColour() {
        return colour;
    }

    public static int getColourForValue(int value) {
        if (value < 0 || value > PRIORITIES.length)
            return NONE.getColour();
        return PRIORITIES[value].getColour();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Priority)) return false;

        Priority priority = (Priority) o;

        if (value != priority.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
