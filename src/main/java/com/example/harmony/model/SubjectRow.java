package com.example.harmony.model;

public record SubjectRow(int id, String name) {
    @Override public String toString() { return name; }
}
