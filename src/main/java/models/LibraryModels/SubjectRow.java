package models.LibraryModels;

public record SubjectRow(int id, String name) {
    @Override public String toString() { return name; }
}
