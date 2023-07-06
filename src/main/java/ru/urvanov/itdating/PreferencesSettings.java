package ru.urvanov.itdating;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesSettings implements Settings {

    private static final String NODE_PATH = "ru/urvanov/itdating";
    private static final String GITHUB_APP_INSTALLATION_TOKEN = "githubAppInstallationToken";
    
    private Preferences preferences;

    public PreferencesSettings() {
        try {
            if (!Preferences.userRoot().nodeExists(NODE_PATH)) {
                preferences = Preferences.userRoot().node(
                    NODE_PATH);
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        preferences = Preferences.userRoot().node(NODE_PATH);
    }
    
    public void setGitHubAppInstallationToken(String token) {
        preferences.put(GITHUB_APP_INSTALLATION_TOKEN, token);
    }
    
    public String getGitHubAppInstallationToken() {
        return preferences.get(GITHUB_APP_INSTALLATION_TOKEN, "ghp_blablabla");
    }
    
    public void save() {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }
}
