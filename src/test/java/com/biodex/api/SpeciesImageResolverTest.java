package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpeciesImageResolverTest {

    @Test
    void sameGuidSharesOneLookup() throws Exception {
        CountingImageService delegate = new CountingImageService();
        SpeciesImageResolver resolver = new SpeciesImageResolver(delegate);

        CompletableFuture<String> first = resolver.imageForGuid("guid:one");
        CompletableFuture<String> second = resolver.imageForGuid("guid:one");

        assertEquals("https://img/guid:one", first.get(5, TimeUnit.SECONDS));
        assertEquals("https://img/guid:one", second.get(5, TimeUnit.SECONDS));
        assertEquals(1, delegate.imageCalls, "duplicate cards must share one network call");
    }

    @Test
    void scientificNameLookupIsRemembered() throws Exception {
        CountingImageService delegate = new CountingImageService();
        SpeciesImageResolver resolver = new SpeciesImageResolver(delegate);

        String first = resolver.imageForScientificName("Rhinella marina").get(5, TimeUnit.SECONDS);
        String second = resolver.imageForScientificName("Rhinella marina").get(5, TimeUnit.SECONDS);

        assertEquals("https://img/thumb", first);
        assertEquals("https://img/thumb", second);
        assertEquals(1, delegate.autocompleteCalls, "second visit must reuse the remembered guid");
    }

    @Test
    void blankAndFakeGuidsResolveToNullWithoutNetwork() throws Exception {
        CountingImageService delegate = new CountingImageService();
        SpeciesImageResolver resolver = new SpeciesImageResolver(delegate);

        assertNull(resolver.imageForGuid(null).get(5, TimeUnit.SECONDS));
        assertNull(resolver.imageForGuid("fake:x").get(5, TimeUnit.SECONDS));
        assertNull(resolver.imageForScientificName("  ").get(5, TimeUnit.SECONDS));
        assertEquals(0, delegate.imageCalls + delegate.autocompleteCalls);
    }

    /** Stub that serves one thumbnail-less match plus a canned image per guid. */
    private static final class CountingImageService implements SpeciesService {
        int imageCalls;
        int autocompleteCalls;

        @Override
        public List<SpeciesSummary> autocomplete(String query, int limit) {
            autocompleteCalls++;
            return List.of(new SpeciesSummary("guid:toad", "Rhinella marina", "Cane Toad", null));
        }

        @Override
        public SpeciesProfile profile(String guid) {
            return null;
        }

        @Override
        public String imageUrl(String guid) {
            imageCalls++;
            if ("guid:toad".equals(guid)) {
                return "https://img/thumb";
            }
            return "https://img/" + guid;
        }

        @Override
        public List<OccurrencePoint> occurrencesNear(
                String scientificName, double lat, double lon, double radiusKm, int limit) {
            return List.of();
        }

        @Override
        public List<AreaCount> densityByArea(String scientificName, String facetField) {
            return List.of();
        }
    }
}
