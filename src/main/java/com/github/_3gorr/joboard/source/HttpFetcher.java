package com.github._3gorr.joboard.source;

import java.io.IOException;

public interface HttpFetcher {

    String get(String url) throws IOException;
}
