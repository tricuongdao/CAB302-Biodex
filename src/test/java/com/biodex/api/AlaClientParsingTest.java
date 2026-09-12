package com.biodex.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing rules for the profile enrichment in {@link AlaClient}: which occurrence-record fields
 * carry an Atlas-hosted photo, and how the Wikipedia description fallback is read. These tests are
 * offline on purpose: no permanent test may depend on the network.
 */
class AlaClientParsingTest {

    private static JsonObject json(String content) {
        return JsonParser.parseString(content).getAsJsonObject();
    }

    // ------------------------------------------------------------ imageUrlFromRecord

    @Test
    void theLargeImageIsPreferred() {
        String url = AlaClient.imageUrlFromRecord(json(
                "{\"largeImageUrl\": \"https://images.ala.org.au/image/proxyImageThumbnailLarge?imageId=abc\","
                        + " \"smallImageUrl\": \"https://images.ala.org.au/image/proxyImageThumbnail?imageId=abc\"}"));
        assertEquals("https://images.ala.org.au/image/proxyImageThumbnailLarge?imageId=abc", url);
    }

    @Test
    void anyImageFieldFillsInWhenTheLargeOneIsMissing() {
        String url = AlaClient.imageUrlFromRecord(json(
                "{\"smallImageUrl\": \"https://images.ala.org.au/image/proxyImageThumbnail?imageId=abc\"}"));
        assertEquals("https://images.ala.org.au/image/proxyImageThumbnail?imageId=abc", url);
    }

    @Test
    void recordsWithoutImagesYieldNoImageUrl() {
        assertNull(AlaClient.imageUrlFromRecord(json("{\"scientificName\": \"Rhinella marina\"}")));
        assertNull(AlaClient.imageUrlFromRecord(null));
    }

    // ------------------------------------------------------------ wikipedia description fallback

    @Test
    void scientificNameBecomesAnUnderscoredPathSegment() {
        assertEquals("Rhinella_marina", AlaClient.wikipediaTitle("Rhinella marina"));
    }

    @Test
    void whitespaceRunsCollapseToSingleUnderscores() {
        assertEquals("Cane_Toad", AlaClient.wikipediaTitle("  Cane   Toad  "));
    }

    @Test
    void blankAndNullNamesYieldNoTitle() {
        assertNull(AlaClient.wikipediaTitle(null));
        assertNull(AlaClient.wikipediaTitle(""));
        assertNull(AlaClient.wikipediaTitle("   "));
    }

    @Test
    void unusualCharactersArePercentEncodedForUseInAPath() {
        String title = AlaClient.wikipediaTitle("Banana (fruit)");
        assertTrue(title.contains("%28"), "parentheses should be percent-encoded: " + title);
        assertTrue(title.contains("%29"), "parentheses should be percent-encoded: " + title);
    }

    @Test
    void standardSummariesYieldTheirExtract() {
        String text = AlaClient.descriptionFromSummary(json(
                "{\"type\": \"standard\", \"extract\": \"The cane toad is a large toad.\"}"));
        assertEquals("The cane toad is a large toad.", text);
    }

    @Test
    void disambiguationPagesYieldNoDescription() {
        assertNull(AlaClient.descriptionFromSummary(json(
                "{\"type\": \"disambiguation\", \"extract\": \"May refer to many things.\"}")));
    }

    @Test
    void summariesWithoutExtractYieldNoDescription() {
        assertNull(AlaClient.descriptionFromSummary(json("{\"type\": \"standard\"}")));
        assertNull(AlaClient.descriptionFromSummary(json(
                "{\"type\": \"standard\", \"extract\": \"   \"}")));
        assertNull(AlaClient.descriptionFromSummary(null));
    }
}