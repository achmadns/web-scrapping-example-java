package com.example.scrapping;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.gargoylesoftware.htmlunit.BrowserVersion.FIREFOX;
import static java.net.URLDecoder.decode;
import static org.assertj.core.api.Assertions.assertThat;

public class WebScrappingExample {
    @Test
    public void scrappingShouldSuccess() {
        final ArrayList<String> phoneLinks = new ArrayList<>();
        final String baseUrl = "https://www.tokopedia.com/p/handphone-tablet/handphone?page=";
        final int targetLink = 100;
        try (WebClient webClient = new WebClient(FIREFOX)) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setPrintContentOnFailingStatusCode(true);
            int currentPage = 1;
            final String redirectionBaseUrl = "https://ta.tokopedia.com";
            final String outputFileName = "output.csv";
            Files.write(Paths.get(outputFileName), "".getBytes());
            HtmlPage page = null;
            List<?> anchors = null;
            HtmlAnchor anchor = null;
            try {
                page = webClient.getPage(baseUrl + currentPage);
                System.out.println("Page Title: " + page.getTitleText());
                while (phoneLinks.size() < targetLink) {
                    anchors = page.getByXPath("//a[@class='css-89jnbj']");
                    for (int i = 0; i < anchors.size(); i++) {
                        anchor = (HtmlAnchor) anchors.get(i);
                        String link = anchor.getHrefAttribute();
                        phoneLinks.add(link);
                    }
                    currentPage++;
                }
                assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(targetLink);
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
            }
            assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(targetLink);
            Thread.sleep(1000);
            String name = null;
            String description = null;
            String imageLink = null;
            String price = null;
            String rating = null;
            String merchantName = null;
            try {
                try (FileWriter recipesFile = new FileWriter(outputFileName, true)) {
                    recipesFile.write("Name, Rating (out of 5), Price, Merchant/Store Name, Image Link, Description, Original Link\n");
                    for (String link : phoneLinks) {
                        System.out.println("Scrapping: " + link);
                        if (link.startsWith(redirectionBaseUrl)) {
                            link = extractAndDecodeProductLink(link);
                            System.out.println("Extracted link: " + link);
                        }
                        page = webClient.getPage(link);
                        webClient.waitForBackgroundJavaScriptStartingBefore(10000);
                        JavaScriptJobManager manager = page.getEnclosingWindow().getJobManager();
                        while (manager.getJobCount() > 0) {
                            Thread.sleep(1000);
                        }
                        anchors = page.getByXPath("//h1[@class='css-1wtrxts']");
                        HtmlHeading1 nameHeading = (HtmlHeading1) anchors.get(0);
                        name = nameHeading.asNormalizedText();
                        description = ((HtmlDivision) page.getByXPath("//div[@data-testid='lblPDPDescriptionProduk']").get(0))
                                .getVisibleText().replace("\n", "\\n");
                        imageLink = ((HtmlImage) page.getByXPath("//img[@class='success fade']").get(0)).getSrc();
                        price = ((HtmlDivision) page.getByXPath("//div[@class='price']").get(0)).getVisibleText();
                        rating = ((DomAttr) ((HtmlMeta) page.getHead().getByXPath("//meta[@itemprop='ratingValue']").get(0)).getByXPath("@content").get(0))
                                .getTextContent();
                        merchantName = StringUtils.substringBetween(page.getBody().asXml(), "\"shopName\":\"", "\",\"minOrder\"");
                        recipesFile.write(String.format("\"%s\",%s,%s,\"%s\",%s,\"%s\",%s\n", name, rating, price, merchantName, imageLink, description, link));
                        // seems Tokopedia applies rate limiting, the final result is still intermittently success.
//                        Thread.sleep(10000);
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            webClient.getCurrentWindow().getJobManager().removeAllJobs();
            assertThat(new File(outputFileName)).exists().isNotEmpty();
            assertThat(Files.readAllLines(Paths.get(outputFileName))).hasSize(101);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String extractAndDecodeProductLink(String link) {
        String substring = link.substring(link.indexOf("r=") + 2);
        return decode(substring.indexOf("&") < 0 ? substring : substring.substring(0, substring.indexOf("&")),
                StandardCharsets.UTF_8);
    }

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
        assertThat(extractAndDecodeProductLink(completeLink)).isEqualTo(expectedLink);
    }

}
