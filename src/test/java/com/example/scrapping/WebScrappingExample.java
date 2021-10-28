package com.example.scrapping;

import io.vavr.control.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Pool;
import stormpot.Timeout;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.net.URLDecoder.decode;
import static org.assertj.core.api.Assertions.assertThat;

public class WebScrappingExample {
    public static final int TARGET_LINK = 100;
    private static final Logger log = LoggerFactory.getLogger(WebScrappingExample.class);
    public static final String NOT_AVAILABLE = "N/A";

    @RepeatedTest(3)
    public void scrappingShouldSuccess(RepetitionInfo repetitionInfo) throws IOException, InterruptedException {
        final ArrayList<String> phoneLinks = new ArrayList<>();
        final int allocatedWorkerCount = Runtime.getRuntime().availableProcessors() / 2;
        final ExecutorService executors = Executors.newFixedThreadPool(allocatedWorkerCount);
        final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(TARGET_LINK);
        final String redirectionBaseUrl = "https://ta.tokopedia.com";
        final String outputFileName = "output-" + repetitionInfo.getCurrentRepetition() + ".csv";
        collectPhoneLinks(phoneLinks);
        final Pool<WebDriverPoolable> webDrivePool = Pool.from(new WebDriverAllocator())
                .setSize(allocatedWorkerCount).build();
        try (FileWriter outputFile = new FileWriter(outputFileName)) {
            outputFile.write("Rating (out of 5), Price, Name, Merchant/Store Name, Image Link, Description, Product Link\n");
            phoneLinks.forEach(link -> extractInformation(latch, webDrivePool, executors, singleThreadExecutor, outputFile,
                    link.startsWith(redirectionBaseUrl) ? extractAndDecodeProductLink(link) : link));
            assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue();
        } catch (IOException e) {
            log.error("An error occurred when processing product links: " + e);
        }
        assertThat(new File(outputFileName)).exists().isNotEmpty();
        assertThat(Files.readAllLines(Paths.get(outputFileName))).hasSize(101);
        webDrivePool.shutdown().await(new Timeout(1, TimeUnit.MINUTES));
    }

    private void extractInformation(CountDownLatch latch, Pool<WebDriverPoolable> webClientPool, ExecutorService executors,
                                    ExecutorService singleThreadExecutor, FileWriter recipesFile, final String productLink) {
        CompletableFuture.supplyAsync(() -> {
            try (WebDriverPoolable webDriverPoolable = claimWebDriver(webClientPool)) {
                log.info("Scrapping: " + productLink);
                final WebDriver driver = webDriverPoolable.driver();
                driver.get(productLink);
                log.debug("Loaded: " + productLink);
                final String row = String.format("%s,%s,\"%s\",\"%s\",%s,\"%s\",%s\n",
                        Try.of(() -> driver.findElements(By.xpath("//span[@data-testid='lblPDPDetailProductRatingNumber']"))
                                .get(0).getText()).getOrElse(NOT_AVAILABLE),
                        Try.of(() -> driver.findElements(By.xpath("//div[@class='price']")).get(0).getText())
                                .getOrElse(NOT_AVAILABLE),
                        Try.of(() -> driver.findElements(By.xpath("//h1[@class='css-1wtrxts']")).get(0).getText())
                                .getOrElse(NOT_AVAILABLE),
                        Try.of(() -> driver.findElements(By.xpath("//a[@data-testid='llbPDPFooterShopName']"))
                                .get(0).findElements(By.xpath("./child::h2")).get(0).getText()).getOrElse(NOT_AVAILABLE),
                        Try.of(() -> driver.findElements(By.xpath("//img[@class='success fade']")).get(0)
                                .getAttribute("src")).getOrElse(NOT_AVAILABLE),
                        Try.of(() -> driver.findElements(By.xpath("//div[@data-testid='lblPDPDescriptionProduk']"))
                                .get(0).getText().replace("\n", "\\n")).getOrElse(NOT_AVAILABLE),
                        productLink);
                log.debug("Scrapped: " + productLink);
                return row;

            } catch (InterruptedException e) {
                log.error("An error occurred when scrapping product page: ", e);
                return NOT_AVAILABLE;
            }
        }, executors).thenAcceptAsync(row -> {
            try {
                recipesFile.write(row);
                latch.countDown();
            } catch (IOException e) {
                log.error("An error occurred when writing the output: ", e);
            }
        }, singleThreadExecutor);
    }

    private void collectPhoneLinks(ArrayList<String> phoneLinks) {
        int currentPage = 1;
        final String baseUrl = "https://www.tokopedia.com/p/handphone-tablet/handphone?page=";
        final WebDriver driver = WebDriverAllocator.allocate();
        while (phoneLinks.size() < TARGET_LINK) {
            final String currentUrl = baseUrl + currentPage;
            driver.get(currentUrl);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
            final List<WebElement> elements = driver.findElements(By.xpath("//a[@class='css-89jnbj']"));
            log.debug("Found product: " + elements.size());
            for (WebElement element : elements) {
                final String link = element.getAttribute("href");
                log.debug(link);
                phoneLinks.add(link);
                if (phoneLinks.size() >= TARGET_LINK) break;
            }
            log.debug("Phone links size: " + phoneLinks.size());
            currentPage++;
        }
        driver.quit();
        assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(TARGET_LINK);
    }

    private WebDriverPoolable claimWebDriver(Pool<WebDriverPoolable> webDriverPool) throws InterruptedException {
        return webDriverPool.claim(new Timeout(3, TimeUnit.SECONDS));
    }

    static String extractAndDecodeProductLink(String link) {
        final String substring = link.substring(link.indexOf("r=") + 2);
        final String extractedLink = decode(!substring.contains("&") ? substring :
                substring.substring(0, substring.indexOf("&")), StandardCharsets.UTF_8);
        log.debug("Extracted link: " + extractedLink);
        return extractedLink;
    }

    @AfterEach
    public void afterEachTest() throws Exception {
        Thread.sleep(3000);
    }

}
