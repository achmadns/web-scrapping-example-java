package com.example.scrapping;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.gargoylesoftware.htmlunit.BrowserVersion.FIREFOX;
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
                        if (link.startsWith(redirectionBaseUrl)) {
                            link = URLDecoder.decode(extractProductLink(link), StandardCharsets.UTF_8);
                        }
                        phoneLinks.add(link);
                    }
                    currentPage++;
                }
                assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(targetLink);
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
            }
            assertThat(phoneLinks).hasSizeGreaterThanOrEqualTo(targetLink);
            try {
                String name = null;
                String description = null;
                String imageLink = null;
                String price = null;
                String rating = null;
                String merchantName = null;
                try (FileWriter recipesFile = new FileWriter(outputFileName, true)) {
                    recipesFile.write("Name, Rating (out of 5), Price, Merchant/Store Name, Image Link, Description, Original Link\n");
                    for (String link : phoneLinks) {
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
                    }
                }
                webClient.getCurrentWindow().getJobManager().removeAllJobs();
            } catch (IOException | InterruptedException e) {
                System.out.println("An error occurred: " + e);
            }
            assertThat(new File(outputFileName)).exists().isNotEmpty();
            assertThat(Files.readAllLines(Paths.get(outputFileName))).hasSize(101);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractProductLink(String link) {
        String substring = link.substring(link.indexOf("&r=") + 3);
        return substring.indexOf("&") > -1 ? substring.substring(0, substring.indexOf("&")) : substring;
    }

    @Test
    public void keepNewLineOnFile() throws IOException {
        Files.write(Paths.get("newline.csv"), "a\nb".replace("\n", "\\n").getBytes());
        assertThat(Files.readAllLines(Paths.get("newline.csv"))).hasSize(1);
    }

    @Test
    public void extractShopNameFromTitleShouldSuccess() {
        String name = "Xiaomi Poco X3 Pro 8/256 GB Garansi Resmi - Biru";
        String title = "Promo Xiaomi Poco X3 Pro 8/256 GB Garansi Resmi - Biru - - Gudang-HP | Tokopedia".replace(name, "");
//        assertThat(title.substring(title.lastIndexOf("-") + 1, title.indexOf("|")).trim()).isEqualTo("Gudang-HP");
    }

    @Test
    public void extractShopNameFromBodyShouldSuccess() {
        final String body = "\"shopID\":\"342169\",\"shopName\":\"Gudang-HP\",\"minOrder\":1,\"maxOrder\":14,\"weight\":500";
        final Pattern pattern = Pattern.compile("\"shopName\":\"(.*?)\",\"minOrder\"");
        final String shopName = StringUtils.substringBetween(body, "\"shopName\":\"", "\",\"minOrder\"");
//        final String shopName = pattern.matcher(body).results().findFirst().get().group(0);
        assertThat(shopName).isEqualTo("Gudang-HP");
    }

    @Test
    @Deprecated
    public void extractProductLinkFromClickableLink() {
        final String link = "https://ta.tokopedia.com/promo/v1/clicks/8a-xgVY2gmUEH_UR6_jOosndbm-xgVY789CBUsthbm-FQRo2PcB5QiUEHZFiPcBWgZUEH_ypo_UOoAKOHZFiyRCsUst7HAnDUMVj9RosQR-BUstpo_KXo_JN6Aep6_UaHsUFopHDUSHp9fh5gaUEUMuNZM2jZJ2M33NGPMep_Mh-qMY2_3j7HjOJ1_uoqjJF_1zu81OJqpz6zJJpZ3BRqujp1SByH7ND3uxGqMVAZ_g-qBuO_OzsHjOJqpV6qj1p_Bz-o1YJgRP617BpZ3O7QcuygIgsQu-Myp-6PMoWu3Bvq1BRZ3BRq3-W69ugHBu2_fB-8VBE__ogu7j7_7HhQVB11_CHuV-k_S2SPVB9192qepV03uPqqjN9Z9xg8BjO33Ooq1hAZSgsQ3hXyurOgB2IuSPyHMh0Z325q1OAZ9o-Q_BNyuPjrc-D69PsQ_B0gVP6HVKaQcW-qMY2_1o-r7BW69BxufzFyMFN8MVI69PyHMh0Z325q1OAZ9o-Qjyh3BxGouKp1MxqH7O2_fBGgjOk1MgozsxR3I2mgBxEeMgozsxR3I2Cq1hAZS2gHsBN3ByNq3oW6_osHBu2_fB-8jN1gRu6uJ1O_7zz8jY1Z9BoqjBR_S2oq1hAZS2gHsBN3ByN8B29zSBgHMP2_fB-81NE19uouVJa_32CH1O1ypCvzJuN_Bzgq3gzv_7ibm-Orfua9fBjUstiHmUDUMVDgaUEUMoxPcuSQR-N9f-N9RCaQfzOyReibm-XP3Oig9-wQfgwy3zpUsthoZFiQSuWyMua9fVjrOYag9Ji6sJObm-sy9zwq3zpUs2Qos1DHseDHpnOoB7DUMNwyfVXgcBjy9zB9fVjraUEopnDUMVi9RzBrRei6i-6UiFircYpPVYxQcri6i-jg3yibm-fg9-pq3YXUstiPsUibm-sQIupPcua9fBj9RyaUsthopjdHaFirI-2yfuwyMBjUst7HAnDUMP5y3hwq3ei6soY?t=desktop&r=https%3A%2F%2Fwww.tokopedia.com%2Fjvish%2Fhp-nokia-105-jadul-dual-sim-new-hitam%3Fsrc%3Dtopads&src=directory&page=1&management_type=1&dv=desktop";
        assertThat(StringUtils.substringBetween(link, "&r=", "&src")).isEqualTo("https%3A%2F%2Fwww.tokopedia.com%2Fjvish%2Fhp-nokia-105-jadul-dual-sim-new-hitam%3Fsrc%3Dtopads");
        assertThat(URLDecoder.decode("https%3A%2F%2Fwww.tokopedia.com%2Fjvish%2Fhp-nokia-105-jadul-dual-sim-new-hitam%3Fsrc%3Dtopads", StandardCharsets.UTF_8))
                .isEqualTo("https://www.tokopedia.com/jvish/hp-nokia-105-jadul-dual-sim-new-hitam?src=topads");
    }

    @Test
    public void extractProductLinkFromClickableLink2() {
        String link = "https://ta.tokopedia.com/promo/v1/clicks/8a-xgVY2gmUEosUhH_nNosKDUMVj9RzNrc1i6sJDUSC5rfB7q3YXUsthbm-7q3OBUsthosHOHsjRosUhbm-srcHi6srOHmFiy3zwrfo5rM1i6sHfoZdfHAJho_1OosHOHp17HaFirpowQcYSUstig9BGqMzUZMggQj2fgAo6QJBkQfBo8_eF_uzSoJOJ__Coucr7_1zoHJO1qfBHe72kgJxGgMHauMxsQ1N5Z325q1O_oIPvucrO_1zVP7Y1gpuozJuR_Oz0q1hAZS-q3cFpysoGqOKp_M2iH72DZ325q1OAZ9o-Q_ufyMO6QJBkQfBqu77F3926Q1da_9z6qMPd_7HFHJO1gcxHuchW_BzSPJO9u_Cqqj22_BzCHOB9__o-q9P2ysoGrVtaQIuyHB-Dy7yNrV2AZ_g-qjV2_JoGPMoWQcNxupuMy7xGPB2UuM2jzsBF3jo-ojBke3BHe72fyfODQMV9o3gsHMxfy7yNrV2AZ_g-qjV2_JoG8cz9uSBBusjF3uPj8jBkQfBy8jjF3I2mgjOc6IPyH_xR3I2mgjOc6IP-q9P2yp-6PMoWuMggQj2fgAo6QJBkQfBo8Bjh_c2gP7O1z_V6uV1a_92u8jN1192-q9P2yp-6PMoWuMgsHBgtyfO6Q7BkQfBoqMHp_c2VHjOE39PvzVBE_1zz81N133BM1_7YUiFiP9oBrBY2gmUEUsnibm-xQcri6i-sy9zBgfYa8uYi8uYFrMYjP3o7UiFiQSuWyMua9fYM9fVjraUEH_1DUMNOQ3-BrBYxgIowrMuhUsthoZFiyfV79fBjraUE3pyObAU7bAHFo_xPbm-X9foxQMz2gcV7guYxgIHi6srFbm-xyBY7g9o7Usti_iUDUSC5rRzwy3hSUstigcuMUiFiPMuarfB5QiUEUSyaUiFiyfhOrRzBrBY2gVYfHiUEH_rdHsjDUSCaq3oB9f-2gmUEop1Fbm-SQfVD9fBjUstpwe?page=1&management_type=2&dv=desktop&t=desktop&src=directory&r=https%3A%2F%2Fwww.tokopedia.com%2Fblimbingcell%2Fnokia-1280-hitam%3Fsrc%3Dtopads";
        link = extractProductLink(link);
        assertThat(link).isEqualTo("https%3A%2F%2Fwww.tokopedia.com%2Fblimbingcell%2Fnokia-1280-hitam%3Fsrc%3Dtopads");
        assertThat(URLDecoder.decode(link, StandardCharsets.UTF_8))
                .isEqualTo("https://www.tokopedia.com/blimbingcell/nokia-1280-hitam?src=topads");
        link = "https://ta.tokopedia.com/promo/v1/clicks/8a-xgVY2gmUEH_UR6_jOosndbm-xgVY789CBUsthbm-FQRo2PcB5QiUEHZFiPcBWgZUEH_ypo_UOoAKOHZFiyRCsUst7HAnDUMVj9RosQR-BUstpo_KXo_JN6Aep6_UaHsUFopHDUSHp9fh5gaUEUMuNZM2jZJ2M33NGPMep_Mh-qMY2_3j7HjOJ1_uoqjJF_1zu81OJqpz6zJJpZ3BRqujp1SByH7ND3uxGqMVAZ_g-qBuO_OzsHjOJqpV6qj1p_Bz-o1YJgRP617BpZ3O7QcuygIgsQu-Myp-6PMoWu3Bvq1BRZ3BRq3-W69ugHBu2_fB-8VBE__ogu7j7_7HhQVB11_CHuV-k_S2SPVB9192qepV03uPqqjN9Z9xg8BjO33Ooq1hAZSgsQ3hXyurOgB2IuSPyHMh0Z325q1OAZ9o-Q_BNyuPjrc-D69PsQ_B0gVP6HVKaQcW-qMY2_1o-r7BW69BxufzFyMFN8MVI69PyHMh0Z325q1OAZ9o-Qjyh3BxGouKp1MxqH7O2_fBGgjOk1MgozsxR3I2mgBxEeMgozsxR3I2Cq1hAZS2gHsBN3ByNq3oW6_osHBu2_fB-8jN1gRu6uJ1O_7zz8jY1Z9BoqjBR_S2oq1hAZS2gHsBN3ByN8B29zSBgHMP2_fB-81NE19uouVJa_32CH1O1ypCvzJuN_Bzgq3gzv_7ibm-Orfua9fBjUstiHmUDUMVDgaUEUMoxPcuSQR-N9f-N9RCaQfzOyReibm-XP3Oig9-wQfgwy3zpUsthoZFiQSuWyMua9fVjrOYag9Ji6sJObm-sy9zwq3zpUs2Qos1DHseDHpnOoB7DUMNwyfVXgcBjy9zB9fVjraUEopnDUMVi9RzBrRei6i-6UiFircYpPVYxQcri6i-jg3yibm-fg9-pq3YXUstiPsUibm-sQIupPcua9fBj9RyaUsthopjdHaFirI-2yfuwyMBjUst7HAnDUMP5y3hwq3ei6soY?t=desktop&r=https%3A%2F%2Fwww.tokopedia.com%2Fjvish%2Fhp-nokia-105-jadul-dual-sim-new-hitam%3Fsrc%3Dtopads&src=directory&page=1&management_type=1&dv=desktop";
        link = extractProductLink(link);
        assertThat(link).isEqualTo("https%3A%2F%2Fwww.tokopedia.com%2Fjvish%2Fhp-nokia-105-jadul-dual-sim-new-hitam%3Fsrc%3Dtopads");
        assertThat(URLDecoder.decode(link, StandardCharsets.UTF_8))
                .isEqualTo("https://www.tokopedia.com/jvish/hp-nokia-105-jadul-dual-sim-new-hitam?src=topads");
    }

}
