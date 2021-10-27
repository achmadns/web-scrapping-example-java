package com.example.scrapping;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import stormpot.Allocator;
import stormpot.Slot;

import static com.gargoylesoftware.htmlunit.BrowserVersion.FIREFOX;

public class WebClientPoolableAllocator implements Allocator<WebClientPoolable> {
    @Override
    public WebClientPoolable allocate(Slot slot) {
        WebClient webClient = new WebClient(FIREFOX);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setPrintContentOnFailingStatusCode(true);
        return new WebClientPoolable(webClient, slot);
    }

    @Override
    public void deallocate(WebClientPoolable poolable) {
        poolable.cleanUp();
    }
}
