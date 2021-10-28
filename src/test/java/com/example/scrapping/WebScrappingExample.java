package com.example.scrapping;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
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
    public void scrappingShouldSuccess() throws IOException, InterruptedException {
        final ArrayList<String> phoneLinks = new ArrayList<>();
        final int availableProcessor = Runtime.getRuntime().availableProcessors();
        final Pool<WebClientPoolable> webClientPool = Pool.from(new WebClientPoolableAllocator())
                .setSize(availableProcessor).build();
        final ExecutorService executors = Executors.newFixedThreadPool(availableProcessor);
        final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(TARGET_LINK);
        final String redirectionBaseUrl = "https://ta.tokopedia.com";
        final String outputFileName = "output.csv";
        collectPhoneLinks(phoneLinks, webClientPool);
        try (FileWriter outputFile = new FileWriter(outputFileName)) {
            outputFile.write("Name, Rating (out of 5), Price, Merchant/Store Name, Image Link, Description, Original Link\n");
            phoneLinks.forEach(link -> extractInformation(latch, webClientPool, executors, singleThreadExecutor, outputFile,
                    link.startsWith(redirectionBaseUrl) ? extractAndDecodeProductLink(link) : link));
            assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue();
        } catch (IOException e) {
            log.error("An error occurred when processing product links: " + e);
        }
        assertThat(new File(outputFileName)).exists().isNotEmpty();
        assertThat(Files.readAllLines(Paths.get(outputFileName))).hasSize(101);
    }

    private void extractInformation(CountDownLatch latch, Pool<WebClientPoolable> webClientPool, ExecutorService executors,
                                    ExecutorService singleThreadExecutor, FileWriter recipesFile, final String productLink) {
        CompletableFuture.supplyAsync(() -> {
            try (WebClientPoolable webClientPoolable = claimWebClient(webClientPool)) {
                log.info("Scrapping: " + productLink);
                final WebClient webClient = webClientPoolable.getWebClient();
                HtmlPage page = webClient.getPage(productLink);
                log.debug("Loaded: " + productLink);
                final String row = String.format("\"%s\",%s,%s,\"%s\",%s,\"%s\",%s\n",
                        Try.of(() -> ((HtmlHeading1) page.getByXPath("//h1[@class='css-1wtrxts']").get(0))
                                .asNormalizedText()).getOrElse(NOT_AVAILABLE),
                        Try.of(() -> ((DomAttr) ((HtmlMeta) page.getHead().getByXPath("//meta[@itemprop='ratingValue']")
                                .get(0)).getByXPath("@content").get(0)).getTextContent()).getOrElse(NOT_AVAILABLE),
                        Try.of(() -> ((HtmlDivision) page.getByXPath("//div[@class='price']").get(0))
                                .getVisibleText()).getOrElse(NOT_AVAILABLE),
                        Try.of(() -> StringUtils.substringBetween(page.getBody().asXml(), "\"shopName\":\"", "\",\"minOrder\""))
                                .getOrElse(NOT_AVAILABLE),
                        Try.of(() -> ((HtmlImage) page.getByXPath("//img[@class='success fade']").get(0)).getSrc())
                                .getOrElse(NOT_AVAILABLE),
                        Try.of(() -> ((HtmlDivision) page.getByXPath("//div[@data-testid='lblPDPDescriptionProduk']").get(0))
                                .getVisibleText().replace("\n", "\\n")).getOrElse(NOT_AVAILABLE)
                        , productLink);
                log.debug("Scrapped: " + productLink);
                return row;

            } catch (InterruptedException | IOException e) {
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

    private void collectPhoneLinks(ArrayList<String> phoneLinks, Pool<WebClientPoolable> webClientPool) {
        int currentPage = 1;
        final String baseUrl = "https://www.tokopedia.com/p/handphone-tablet/handphone?page=";
        try (WebClientPoolable webClientPoolable = claimWebClient(webClientPool)) {
            final WebClient webClient = webClientPoolable.getWebClient();
            HtmlPage page;
            List<?> anchors;
            HtmlAnchor anchor;
            try {
                while (phoneLinks.size() < TARGET_LINK) {
                    final String currentUrl = baseUrl + currentPage;
                    page = webClient.getPage(currentUrl);
                    log.info("Page Title: " + page.getTitleText() + "; " + currentUrl);
                    anchors = page.getByXPath("//a[@class='css-89jnbj']");
                    log.debug("Anchor size: " + anchors.size());
                    for (Object o : anchors) {
                        anchor = (HtmlAnchor) o;
                        String link = anchor.getHrefAttribute();
                        phoneLinks.add(link);
                        if (phoneLinks.size() >= TARGET_LINK) break;
                    }
                    log.debug("Phone links size: " + phoneLinks.size());
                    currentPage++;
                }
            } catch (IOException e) {
                log.error("An error occurred when listing down the product links: ", e);
            }
        } catch (InterruptedException e) {
            log.error("An error occurred when using webclient: ", e);
        }
        assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(TARGET_LINK);
    }

    private WebClientPoolable claimWebClient(Pool<WebClientPoolable> webClientPool) throws InterruptedException {
        return webClientPool.claim(new Timeout(3, TimeUnit.SECONDS));
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
