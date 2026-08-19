package com.github._3gorr.joboard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Salary(Integer from, Integer to, String currency) {

    public static Salary none() {
        return new Salary(null, null, null);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return from == null && to == null;
    }
}
