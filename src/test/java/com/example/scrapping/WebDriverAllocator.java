package com.example.scrapping;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import stormpot.Allocator;
import stormpot.Slot;

import java.util.concurrent.TimeUnit;

public class WebDriverAllocator implements Allocator<WebDriverPoolable> {
    @Override
    public WebDriverPoolable allocate(Slot slot) {
        return new WebDriverPoolable(slot, allocate());
    }

    @Override
    public void deallocate(WebDriverPoolable poolable) {
        poolable.quit();
    }

    public static WebDriver allocate() {
        System.setProperty("webdriver.chrome.driver", "/home/achmad/bin/chromedriver");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        chromeOptions.setHeadless(false);
        final WebDriver driver = new ChromeDriver(chromeOptions);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        return driver;
    }
}
