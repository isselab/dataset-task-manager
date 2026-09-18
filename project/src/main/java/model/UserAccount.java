package model;

public record UserAccount(String username, String email, String salt, String passwordHash) { }
