package ru.urvanov.itdating;

public interface Settings {
    void setGitHubAppInstallationToken(String token);

    String getGitHubAppInstallationToken();

    void save();

}
