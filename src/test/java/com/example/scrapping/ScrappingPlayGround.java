package com.example.scrapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ScrappingPlayGround {
    private static final Logger log = LoggerFactory.getLogger(ScrappingPlayGround.class);

    @Test
    public void keepNewLineOnFile() throws IOException {
        Files.write(Paths.get("newline.csv"), "a\nb".replace("\n", "\\n").getBytes());
        assertThat(Files.readAllLines(Paths.get("newline.csv"))).hasSize(1);
    }


    @ParameterizedTest
    @CsvFileSource(resources = "/url-cases.csv", numLinesToSkip = 1)
    public void extractProductLinkFromClickableLink(String expectedLink, String completeLink) {
        assertThat(WebScrappingExample.extractAndDecodeProductLink(completeLink)).isEqualTo(expectedLink);
    }

    @Test
    public void openWebPage() {
        WebDriver driver = WebDriverAllocator.allocate();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        driver.get("https://www.tokopedia.com/p/handphone-tablet/handphone?page=1");
        //This will scroll the web page till end.
        js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        final List<WebElement> elements = driver.findElements(By.xpath("//a[@class='css-89jnbj']"));
        log.debug("Found product: " + elements.size());
        for (WebElement element : elements) {
            log.debug(element.getAttribute("href"));
        }
        driver.quit();
    }
}

