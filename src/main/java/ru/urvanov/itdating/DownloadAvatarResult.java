package ru.urvanov.itdating;

public record DownloadAvatarResult (String login, String avatarUrl, byte[] avatarData, String email, String name, String blog, String location, String bio, boolean good, Exception ex) {};