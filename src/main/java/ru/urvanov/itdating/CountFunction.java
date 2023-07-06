package ru.urvanov.itdating;

import java.util.function.Function;

import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedSearchIterable;

public class CountFunction implements Function<PagedSearchIterable<GHUser>, CountResult> {

    @Override
    public CountResult apply(PagedSearchIterable<GHUser> pagedSearchIterable) {
        int totalCount = pagedSearchIterable.getTotalCount();
        if (totalCount > 1000) totalCount = 1000; // limit for search requests
        
        return new CountResult(pagedSearchIterable, totalCount);
    }

}
