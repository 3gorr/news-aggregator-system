package com.github._3gorr.joboard.source;

import java.util.Optional;

public record FetchQuery(Optional<String> text, int maxPages, int perPage) {

    public static FetchQuery defaults() {
        return new FetchQuery(Optional.empty(), 1, 20);
    }

    public static FetchQuery of(String text, int maxPages, int perPage) {
        return new FetchQuery(Optional.ofNullable(text), maxPages, perPage);
    }
}
