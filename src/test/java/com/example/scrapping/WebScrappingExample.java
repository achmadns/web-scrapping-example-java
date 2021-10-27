package com.example.scrapping;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
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

import static com.gargoylesoftware.htmlunit.BrowserVersion.FIREFOX;
import static java.net.URLDecoder.decode;
import static org.assertj.core.api.Assertions.assertThat;

public class WebScrappingExample {
    private final Logger log = LoggerFactory.getLogger(WebScrappingExample.class);

    @RepeatedTest(3)
    public void scrappingShouldSuccess() throws IOException {
        final ArrayList<String> phoneLinks = new ArrayList<>();
        final String baseUrl = "https://www.tokopedia.com/p/handphone-tablet/handphone?page=";
        final int targetLink = 100;
        final String redirectionBaseUrl = "https://ta.tokopedia.com";
        int currentPage = 1;
        final String outputFileName = "output.csv";
        try (WebClient webClient = new WebClient(FIREFOX)) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setPrintContentOnFailingStatusCode(true);
            Files.write(Paths.get(outputFileName), "".getBytes());
            HtmlPage page = null;
            List<?> anchors = null;
            HtmlAnchor anchor = null;
            try {
                page = webClient.getPage(baseUrl + currentPage);
                log.info("Page Title: " + page.getTitleText());
                while (phoneLinks.size() < targetLink) {
                    anchors = page.getByXPath("//a[@class='css-89jnbj']");
                    for (int i = 0; i < anchors.size(); i++) {
                        anchor = (HtmlAnchor) anchors.get(i);
                        String link = anchor.getHrefAttribute();
                        phoneLinks.add(link);
                        if (phoneLinks.size() >= targetLink) break;
                    }
                    currentPage++;
                }
                assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(targetLink);
            } catch (IOException e) {
                log.error("An error occurred: " + e);
            }
            assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(targetLink);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final CountDownLatch latch = new CountDownLatch(targetLink);
        final Pool<WebClientPoolable> webClientPool = Pool.from(new WebClientPoolableAllocator()).build();
        final ExecutorService executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        try (FileWriter recipesFile = new FileWriter(outputFileName, true)) {
            recipesFile.write("Name, Rating (out of 5), Price, Merchant/Store Name, Image Link, Description, Original Link\n");
            for (String link : phoneLinks) {
                log.info("Scrapping: " + link);
                if (link.startsWith(redirectionBaseUrl)) {
                    link = extractAndDecodeProductLink(link);
                    log.info("Extracted link: " + link);
                }
                final String productLink = link;
                CompletableFuture.supplyAsync(() -> {
                    try (WebClientPoolable webClientPoolable = webClientPool.claim(new Timeout(10, TimeUnit.SECONDS))) {
                        final WebClient webClient = webClientPoolable.getWebClient();
                        HtmlPage page = webClient.getPage(productLink);
                        return String.format("\"%s\",%s,%s,\"%s\",%s,\"%s\",%s\n",
                                ((HtmlHeading1) page.getByXPath("//h1[@class='css-1wtrxts']").get(0)).asNormalizedText(),
                                ((DomAttr) ((HtmlMeta) page.getHead().getByXPath("//meta[@itemprop='ratingValue']").get(0)).getByXPath("@content").get(0))
                                        .getTextContent(),
                                ((HtmlDivision) page.getByXPath("//div[@class='price']").get(0)).getVisibleText(),
                                StringUtils.substringBetween(page.getBody().asXml(), "\"shopName\":\"", "\",\"minOrder\""),
                                ((HtmlImage) page.getByXPath("//img[@class='success fade']").get(0)).getSrc(),
                                ((HtmlDivision) page.getByXPath("//div[@data-testid='lblPDPDescriptionProduk']").get(0))
                                        .getVisibleText().replace("\n", "\\n"), productLink);

                    } catch (InterruptedException | IOException e) {
                        log.error("An error occurred: " + e);
                        return "N/A";
                    }
                }, executors).thenAcceptAsync(row -> {
                    try {
                        recipesFile.write(row);
                        latch.countDown();
                    } catch (IOException e) {
                        log.error("An error occurred: " + e);
                    }
                }, singleThreadExecutor);
            }
            latch.await(10, TimeUnit.MINUTES);
        } catch (IOException | InterruptedException e) {
            log.error("An error occurred: " + e);
        }
        assertThat(new File(outputFileName)).exists().isNotEmpty();
        assertThat(Files.readAllLines(Paths.get(outputFileName))).hasSize(101);
    }

    static String extractAndDecodeProductLink(String link) {
        String substring = link.substring(link.indexOf("r=") + 2);
        return decode(!substring.contains("&") ? substring : substring.substring(0, substring.indexOf("&")),
                StandardCharsets.UTF_8);
    }

    @AfterEach
    public void afterEachTest() throws Exception {
        Thread.sleep(3000);
    }

}
