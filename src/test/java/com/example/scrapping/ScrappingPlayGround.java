package com.example.scrapping;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ScrappingPlayGround {

    @Test
    public void keepNewLineOnFile() throws IOException {
        Files.write(Paths.get("newline.csv"), "a\nb".replace("\n", "\\n").getBytes());
        assertThat(Files.readAllLines(Paths.get("newline.csv"))).hasSize(1);
    }

    @Test
    public void extractShopNameFromBodyShouldSuccess() {
        final String body = "\"shopID\":\"342169\",\"shopName\":\"Gudang-HP\",\"minOrder\":1,\"maxOrder\":14,\"weight\":500";
        final Pattern pattern = Pattern.compile("\"shopName\":\"(.*?)\",\"minOrder\"");
        final String shopName = StringUtils.substringBetween(body, "\"shopName\":\"", "\",\"minOrder\"");
//        final String shopName = pattern.matcher(body).results().findFirst().get().group(0);
        assertThat(shopName).isEqualTo("Gudang-HP");
    }


    @ParameterizedTest
    @CsvFileSource(resources = "/url-cases.csv", numLinesToSkip = 1)
    public void extractProductLinkFromClickableLink(String expectedLink, String completeLink) {
        assertThat(WebScrappingExample.extractAndDecodeProductLink(completeLink)).isEqualTo(expectedLink);
    }

//    @Test
//    public void openWebPage() throws InterruptedException {
//        System.setProperty("webdriver.chrome.driver", "/home/achmad/bin/chromedriver");
//        final WebDriver driver = new ChromeDriver();
//        driver.get("http://www.google.com/");
//
//        Thread.sleep(5000);  // Let the user actually see something!
//
//        WebElement searchBox = driver.findElement(By.name("q"));
//
//        searchBox.sendKeys("ChromeDriver");
//
//        searchBox.submit();
//
//        Thread.sleep(5000);  // Let the user actually see something!
//
//        driver.quit();
//    }
}

