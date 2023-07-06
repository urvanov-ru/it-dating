package ru.urvanov.itdating;

import java.io.IOException;

public record CheckBlogResult (String login, String avatarUrl, String email, String name, String blog, String location, String bio, boolean good, Exception ex) {};