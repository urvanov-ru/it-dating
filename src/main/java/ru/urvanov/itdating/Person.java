package ru.urvanov.itdating;

public record Person(int id, String login, String name, String avatarUrl, byte[] avatarData, String email, String blog, String location, String bio) {
    public static Person DUMMY =  new Person(-1, "<login>", "<name>", null, null, "<e-mail>", "<blog>", "<location>", "<bio>");
}
