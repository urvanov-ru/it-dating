package ru.urvanov.itdating;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.kohsuke.github.GHException;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedIterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class SearchFunction implements Function<CountResult, SearchResult> {

    private static final long WAIT_BEFORE_RETRY_MILLIS = 60_000L;
    
    private ObjectReader objectReader = new ObjectMapper().reader();
    
    @Override
    public SearchResult apply(CountResult countResult) {
        boolean retry = true;
        PagedIterator<GHUser> pagedIterator = countResult.pagedSearchIterable().iterator();
        List<SearchResultItem> searchResultItems = new ArrayList<>();
        while (retry) {
            try {
                while (pagedIterator.hasNext()) {
                     searchResultItems.addAll(pagedIterator.nextPage().stream().map(ghUser -> new SearchResultItem(ghUser.getLogin(), ghUser.getAvatarUrl())).toList());
                }
                retry = false;
            } catch (GHException ghex) {
                retry = checkNeedWaitAndRetry(ghex);
                if (retry) {
                    try {
                        Thread.sleep(WAIT_BEFORE_RETRY_MILLIS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        
                    }
                }
            }
        }
        return new SearchResult(searchResultItems);
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

}
