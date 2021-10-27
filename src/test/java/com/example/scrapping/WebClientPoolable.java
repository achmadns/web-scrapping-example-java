package com.example.scrapping;

import com.gargoylesoftware.htmlunit.WebClient;
import stormpot.Poolable;
import stormpot.Slot;

import java.io.Closeable;

public class WebClientPoolable implements Poolable, Closeable {
    private final WebClient webClient;
    private final Slot slot;

    public WebClientPoolable(WebClient webClient, Slot slot) {
        this.webClient = webClient;
        this.slot = slot;
    }

    @Override
    public void release() {
        this.slot.release(this);
    }

    @Override
    public void close() {
        release();
    }

    public void cleanUp() {
        webClient.getCurrentWindow().getJobManager().removeAllJobs();
        webClient.close();
    }

    public WebClient getWebClient() {
        return webClient;
    }
}
