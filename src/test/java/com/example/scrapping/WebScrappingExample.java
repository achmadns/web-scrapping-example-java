package com.example.scrapping;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WebScrappingExample {
    @Test
    public void scrappingShouldSuccess() {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setPrintContentOnFailingStatusCode(false);
            try {
                HtmlPage page = webClient.getPage("https://foodnetwork.co.uk/italian-family-dinners/");
                String title = page.getTitleText();
                System.out.println("Page Title: " + title);
                final List<HtmlAnchor> links = page.getAnchors();
                for (HtmlAnchor link : links) {
                    String href = link.getHrefAttribute();
                    System.out.println("Link: " + href);
                }
                final List<?> anchors = page.getByXPath("//a[@class='card-link']");
                try (FileWriter recipesFile = new FileWriter("recipes.csv", true)) {
                    recipesFile.write("id,name,link\n");
                    for (int i = 0; i < anchors.size(); i++) {
                        HtmlAnchor link = (HtmlAnchor) anchors.get(i);
                        String recipeTitle = link.getAttribute("title").replace(',', ';');
                        String recipeLink = link.getHrefAttribute();
                        recipesFile.write(i + "," + recipeTitle + "," + recipeLink + "\n");
                    }
                }
                webClient.getCurrentWindow().getJobManager().removeAllJobs();
                assertThat(new File("recipes.csv")).exists().isNotEmpty();
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
            }
        }
    }
}
