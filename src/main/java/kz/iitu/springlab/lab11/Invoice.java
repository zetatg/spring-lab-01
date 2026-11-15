package kz.iitu.springlab.lab11;

/** A synthetic invoice of the university finance API (Task F). */
public record Invoice(long id, String department, String classification, String description, String amount) { }
