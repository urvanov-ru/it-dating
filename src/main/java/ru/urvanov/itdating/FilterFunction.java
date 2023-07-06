package ru.urvanov.itdating;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Function;

import org.kohsuke.github.GHException;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class FilterFunction implements Function<SearchResultItem, FilterResult> {

    static final Locale RUSSIAN_LOCALE = new Locale("ru", "RU");

    private static final long WAIT_BEFORE_RETRY_MILLIS = 60_000L;
    
    private GitHub github;
    
    private ObjectReader objectReader = new ObjectMapper().reader();

    public FilterFunction(GitHub github) {
        this.github = github;
    }

    @Override
    public FilterResult apply(SearchResultItem searchResultItem) {
        FilterResult result = null;
        
        while (result == null) {
            try {
                GHUser ghUser = github.getUser(searchResultItem.login());
                if ((isWoman(ghUser.getName()))
                        && (hasValue(ghUser.getBlog())
                                || (hasValue(ghUser.getEmail())))) {
                    result = new FilterResult(ghUser.getLogin(), ghUser.getAvatarUrl(), ghUser.getEmail(), ghUser.getName(), ghUser.getBlog(), ghUser.getLocation(), ghUser.getBio(), true, null);
                } else {
                    result = new FilterResult(ghUser.getLogin(), ghUser.getAvatarUrl(), ghUser.getEmail(), ghUser.getName(), ghUser.getBlog(), ghUser.getLocation(), ghUser.getBio(), false, null);
                } 
            } catch (GHException ghex) {
                boolean retry = checkNeedWaitAndRetry(ghex);
                if (retry) {
                    try {
                        Thread.sleep(WAIT_BEFORE_RETRY_MILLIS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        result = new FilterResult(null, null, null, null, null, null, null, false, e);
                    }
                } else {
                    result = new FilterResult(null, null, null, null, null, null, null, false, ghex);
                }
            }
            catch (IOException ioex) {
                result = new FilterResult(null, null, null, null, null, null, null, false, ioex);
            }
        }
        return result;
    }


    private boolean checkNeedWaitAndRetry(GHException ghex) {
        Throwable cause = ghex.getCause();
        if (cause == null) {
            return false;
        }
        if (cause instanceof org.kohsuke.github.HttpException httpException) {
            String message = httpException.getMessage();
            try {
                JsonNode jsonNode = objectReader.readTree(message);
                String messageFromGitHub = jsonNode.get("message").textValue();
                if ("You have exceeded a secondary rate limit. Please wait a few minutes before you try again.".equals(messageFromGitHub)) {
                    return true;
                }
            } catch (JsonMappingException jme) {
                jme.printStackTrace();
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean hasValue(String str) {
        return ((str != null) && (!str.isBlank()));
    }

    private boolean isWoman(String name) {
        if (name == null)
            return false;
        String lowerName = name.toLowerCase(RUSSIAN_LOCALE);
        return WomanNames.WOMAN_NAMES.parallelStream()
                .anyMatch(el -> lowerName.matches("\\b" + el + "\\b"));
    }

}
