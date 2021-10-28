package com.example.scrapping;

import org.openqa.selenium.WebDriver;
import stormpot.Poolable;
import stormpot.Slot;

import java.io.Closeable;

public class WebDriverPoolable implements Poolable, Closeable {
    private final Slot slot;
    private final WebDriver driver;

    public WebDriverPoolable(Slot slot, WebDriver driver) {
        this.slot = slot;
        this.driver = driver;
    }

    @Override
    public void close() {
        release();
    }

    @Override
    public void release() {
        slot.release(this);
    }

    public void quit() {
        driver.quit();
    }

    public WebDriver driver() {
        return driver;
    }
}
