/*
Copyright 2015 Daniele Bufarini

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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
