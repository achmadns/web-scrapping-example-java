package com.example.scrapping;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

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
}
