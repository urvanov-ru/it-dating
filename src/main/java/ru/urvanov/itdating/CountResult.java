package ru.urvanov.itdating;

import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedSearchIterable;

public record CountResult (PagedSearchIterable<GHUser> pagedSearchIterable, int totalCount) {}
