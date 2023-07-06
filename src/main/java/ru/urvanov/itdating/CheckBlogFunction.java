package ru.urvanov.itdating;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.function.Function;

public class CheckBlogFunction
        implements Function<FilterResult, CheckBlogResult> {

    @Override
    public CheckBlogResult apply(FilterResult filterResult) {
        boolean blogUrlValid = true;
        String blogUrl = filterResult.blog();
        if ((filterResult.good()) && (isBlogNormal(blogUrl))) {

            if (!blogUrl.startsWith("http"))
                blogUrl = "https://" + blogUrl.strip();

            int statusCode = 0;

            try {
                HttpRequest getRequest = HttpRequest
                        .newBuilder(new URI(blogUrl)).timeout(Duration.ofSeconds(10L)).GET().build();

                statusCode = HttpClient.newHttpClient().send(getRequest, BodyHandlers.ofString())
                        .statusCode();
            } catch (URISyntaxException | IOException
                    | InterruptedException e) {
                e.printStackTrace();
                blogUrlValid = false;
            }
            if ((statusCode < 200) && (statusCode >= 400))
                blogUrlValid = false;

            if (blogUrlValid) {
                return new CheckBlogResult(filterResult.login(),
                        filterResult.avatarUrl(), filterResult.email(),
                        filterResult.name(), blogUrl,
                        filterResult.location(), filterResult.bio(),
                        true, null);
            }
            System.out.println(blogUrl + " is invalid blog url");
            return new CheckBlogResult(filterResult.login(),
                    filterResult.avatarUrl(), filterResult.email(),
                    filterResult.name(), filterResult.blog(),
                    filterResult.location(), filterResult.bio(), false, null);
        } 

        return new CheckBlogResult(filterResult.login(),
                filterResult.avatarUrl(), filterResult.email(),
                filterResult.name(), filterResult.blog(),
                filterResult.location(), filterResult.bio(),
                filterResult.good(), filterResult.ex());
        
        
    }
    
    private boolean isBlogNormal(String blog) {
        return blog != null && !blog.isBlank() && !blog.contains("linkedin")
                && !blog.contains("vk") && !blog.contains("facebook")
                && !blog.contains("twitter") && !blog.contains("t.me")
                && !blog.contains("instagram.com")
                && !blog.contains("plus.google.com");
    }

}
