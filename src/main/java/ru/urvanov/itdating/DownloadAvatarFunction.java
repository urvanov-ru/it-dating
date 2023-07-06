package ru.urvanov.itdating;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.function.Function;

public class DownloadAvatarFunction implements Function<CheckBlogResult, DownloadAvatarResult> {

    @Override
    public DownloadAvatarResult apply(CheckBlogResult checkBlogResult) {
        if (checkBlogResult.good()) {
            
            byte[] avatarBytes = null;
            try {
                HttpRequest getRequest = HttpRequest
                        .newBuilder(new URI(checkBlogResult.avatarUrl()))
                        .timeout(Duration.ofSeconds(10L)).GET().build();

                HttpResponse<byte[]> httpResponse = HttpClient.newHttpClient()
                        .send(getRequest, BodyHandlers.ofByteArray());
                if (httpResponse.statusCode() != 200) {
                    return new DownloadAvatarResult(checkBlogResult.login(), checkBlogResult.avatarUrl(), new byte[0], checkBlogResult.email(), checkBlogResult.name(), checkBlogResult.blog(), checkBlogResult.location(), checkBlogResult.bio(), false, null);
                }
                avatarBytes = httpResponse.body();
            } catch (URISyntaxException | IOException
                    | InterruptedException e) {
                System.out.println(checkBlogResult.avatarUrl() + " is invalid avatar url");
                e.printStackTrace();
                new DownloadAvatarResult(checkBlogResult.login(), checkBlogResult.avatarUrl(), new byte[0], checkBlogResult.email(), checkBlogResult.name(), checkBlogResult.blog(), checkBlogResult.location(), checkBlogResult.bio(), false, e);
            }
            return new DownloadAvatarResult(checkBlogResult.login(), checkBlogResult.avatarUrl(), avatarBytes, checkBlogResult.email(), checkBlogResult.name(), checkBlogResult.blog(), checkBlogResult.location(), checkBlogResult.bio(), true, null);
        }
        return new DownloadAvatarResult(checkBlogResult.login(), checkBlogResult.avatarUrl(), new byte[0], checkBlogResult.email(), checkBlogResult.name(), checkBlogResult.blog(), checkBlogResult.location(), checkBlogResult.bio(), checkBlogResult.good(), checkBlogResult.ex());
    }

}
