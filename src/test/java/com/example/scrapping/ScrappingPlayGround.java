package com.example.scrapping;

//import org.apache.commons.lang3.StringUtils;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ScrappingPlayGround {

    @Test
    public void keepNewLineOnFile() throws IOException {
        Files.write(Paths.get("newline.csv"), "a\nb".replace("\n", "\\n").getBytes());
        assertThat(Files.readAllLines(Paths.get("newline.csv"))).hasSize(1);
    }


//    @ParameterizedTest
//    @CsvFileSource(resources = "/url-cases.csv", numLinesToSkip = 1)
//    public void extractProductLinkFromClickableLink(String expectedLink, String completeLink) {
//        assertThat(WebScrappingExample.extractAndDecodeProductLink(completeLink)).isEqualTo(expectedLink);
//    }

    @Test
    public void openWebPage() throws InterruptedException {
        System.setProperty("webdriver.chrome.driver", "/home/achmad/bin/chromedriver");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        WebDriver driver = new ChromeDriver(chromeOptions);
        JavascriptExecutor js = (JavascriptExecutor) driver;


        driver.get("https://www.tokopedia.com/p/handphone-tablet/handphone?page=1");
        //This will scroll the web page till end.
        js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        driver.quit();
    }
}

