package com.biodex.db;

import com.biodex.model.Species;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seeds the local demo data: the curated pest species (matching the ONNX model labels), Brisbane
 * suburbs with map coordinates, and a set of realistic sample sightings so the heat map has
 * something to plot before anyone reports anything. Each table is seeded independently and only
 * when empty, so calling this on every startup is cheap and never duplicates rows. Sightings need
 * an existing user to attribute them to (the table's foreign key), so they are skipped until the
 * first account exists.
 */
public final class DataSeeder {

    private DataSeeder() {
    }

    /** Seeds each demo table that is still empty. Safe to call repeatedly. */
    public static void seedIfEmpty(Connection connection) {
        try {
            seedSpeciesIfEmpty(connection);
            seedSuburbsIfEmpty(connection);
            seedSampleSightingsIfEmpty(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to seed demo data", e);
        }
    }

    private static void seedSpeciesIfEmpty(Connection connection) throws SQLException {
        if (rowCount(connection, "species") > 0) {
            return; // Already seeded
        }
        for (Species s : getSeedSpecies()) {
            insertSpecies(connection, s);
        }
    }

    private static int rowCount(Connection connection, String table) throws SQLException {
        try (Statement stmt = connection.createStatement();
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void insertSpecies(Connection connection, Species s) throws SQLException {
        String sql = """
                INSERT INTO species
                      (common_name, scientific_name, threat_level, aggression, sting_severity, spread_risk,
                       typical_habitat, size_min_mm, size_max_mm, disposal_guidance, photo_path, ala_guid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getCommonName());
            ps.setString(2, s.getScientificName());
            ps.setString(3, s.getThreatLevel().name());
            ps.setInt(4, s.getAggression());
            ps.setInt(5, s.getStingSeverity());
            ps.setInt(6, s.getSpreadRisk());
            ps.setString(7, s.getTypicalHabitat());
            ps.setDouble(8, s.getSizeMinMm());
            ps.setDouble(9, s.getSizeMaxMm());
            ps.setString(10, s.getDisposalGuidance());
            ps.setString(11, s.getPhotoPath());
            ps.setString(12, s.getAlaGuid());
            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int speciesId = keys.getInt(1);
                    insertTags(connection, speciesId, s.getTags());
                }
            }
        }
    }

    private static void insertTags(Connection connection, int speciesId, List<String> tags) throws SQLException {
        if (tags == null || tags.isEmpty()) return;
        String sql = "INSERT INTO species_tags (species_id, tag) VALUES (?, ?)";
        try (var ps = connection.prepareStatement(sql)) {
            for (String tag : tags) {
                ps.setInt(1, speciesId);
                ps.setString(2, tag);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Matches the shape SQLite writes for datetime('now', 'localtime'), so stored values stay comparable. */
    private static final DateTimeFormatter SQLITE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /** Inserts Brisbane suburbs with coordinates, so plotted sightings land in the right place. */
    private static void seedSuburbsIfEmpty(Connection connection) throws SQLException {
        if (rowCount(connection, "suburbs") > 0) {
            return;
        }
        String sql = "INSERT INTO suburbs (name, postcode, latitude, longitude) VALUES (?, ?, ?, ?)";
        try (var ps = connection.prepareStatement(sql)) {
            for (SuburbSeed suburb : SUBURB_SEEDS) {
                ps.setString(1, suburb.name());
                ps.setString(2, suburb.postcode());
                ps.setDouble(3, suburb.latitude());
                ps.setDouble(4, suburb.longitude());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Inserts the sample sightings the heat map demonstrates with, dated across the last month.
     * Skipped until a user account exists, because a sighting must be attributed to someone.
     */
    private static void seedSampleSightingsIfEmpty(Connection connection) throws SQLException {
        if (rowCount(connection, "sightings") > 0) {
            return;
        }
        Integer userId = firstUserId(connection);
        if (userId == null) {
            return; // No account to attribute sightings to yet.
        }
        Map<String, Integer> suburbIds = suburbIdsByName(connection);
        if (suburbIds.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO sightings "
                + "(user_id, suburb_id, species_name, description, image_path, sighted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (var ps = connection.prepareStatement(sql)) {
            for (SightingSeed sighting : SIGHTING_SEEDS) {
                Integer suburbId = suburbIds.get(sighting.suburb().toLowerCase(Locale.ROOT));
                if (suburbId == null) {
                    continue;
                }
                ps.setInt(1, userId);
                ps.setInt(2, suburbId);
                ps.setString(3, sighting.species());
                ps.setString(4, sighting.description());
                ps.setString(5, null);
                ps.setString(6, SQLITE_TIMESTAMP.format(sightedAt(sighting)));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static Integer firstUserId(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
                var rs = stmt.executeQuery("SELECT user_id FROM users ORDER BY user_id LIMIT 1")) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    private static Map<String, Integer> suburbIdsByName(Connection connection) throws SQLException {
        Map<String, Integer> ids = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement();
                var rs = stmt.executeQuery("SELECT suburb_id, name FROM suburbs")) {
            while (rs.next()) {
                ids.put(rs.getString("name").toLowerCase(Locale.ROOT), rs.getInt("suburb_id"));
            }
        }
        return ids;
    }

    /** Local date and hour for a sample sighting, converted to the UTC shape SQLite stores. */
    private static Instant sightedAt(SightingSeed sighting) {
        return LocalDate.now()
                .minusDays(sighting.daysAgo())
                .atTime(LocalTime.of(sighting.hour(), 15))
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    /** One seeded suburb: display name, postcode and the coordinates the map projects. */
    private record SuburbSeed(String name, String postcode, double latitude, double longitude) {
    }

    /** One seeded sighting: where it was seen, what was seen, and when (days ago, hour). */
    private record SightingSeed(String suburb, String species, String description,
            long daysAgo, int hour) {
    }

    /** Brisbane suburbs spread across the map area, all inside the projection's bounds. */
    private static final List<SuburbSeed> SUBURB_SEEDS = List.of(
            new SuburbSeed("Algester", "4115", -27.6120, 153.0310),
            new SuburbSeed("Annerley", "4103", -27.5090, 153.0320),
            new SuburbSeed("Ascot", "4007", -27.4320, 153.0620),
            new SuburbSeed("Ashgrove", "4060", -27.4450, 152.9850),
            new SuburbSeed("Aspley", "4034", -27.3630, 153.0180),
            new SuburbSeed("Auchenflower", "4066", -27.4760, 152.9980),
            new SuburbSeed("Belmont", "4153", -27.5030, 153.1220),
            new SuburbSeed("Brisbane City", "4000", -27.4698, 153.0251),
            new SuburbSeed("Bulimba", "4171", -27.4520, 153.0600),
            new SuburbSeed("Calamvale", "4116", -27.6180, 153.0470),
            new SuburbSeed("Camp Hill", "4152", -27.4940, 153.0790),
            new SuburbSeed("Carina", "4152", -27.4900, 153.0910),
            new SuburbSeed("Carindale", "4152", -27.5050, 153.1013),
            new SuburbSeed("Chermside", "4032", -27.3842, 153.0306),
            new SuburbSeed("Coorparoo", "4151", -27.4950, 153.0560),
            new SuburbSeed("Darra", "4076", -27.5650, 152.9550),
            new SuburbSeed("Forest Lake", "4078", -27.6120, 152.9690),
            new SuburbSeed("Gordon Park", "4031", -27.4180, 153.0330),
            new SuburbSeed("Hamilton", "4007", -27.4380, 153.0640),
            new SuburbSeed("Highgate Hill", "4101", -27.4890, 153.0160),
            new SuburbSeed("Inala", "4077", -27.5970, 152.9740),
            new SuburbSeed("Indooroopilly", "4068", -27.4990, 152.9730),
            new SuburbSeed("Kangaroo Point", "4169", -27.4750, 153.0362),
            new SuburbSeed("Kedron", "4031", -27.4020, 153.0300),
            new SuburbSeed("Kenmore", "4069", -27.5080, 152.9390),
            new SuburbSeed("Lutwyche", "4030", -27.4230, 153.0340),
            new SuburbSeed("Manly", "4179", -27.4560, 153.1810),
            new SuburbSeed("Milton", "4064", -27.4700, 153.0000),
            new SuburbSeed("Mitchelton", "4053", -27.4130, 152.9780),
            new SuburbSeed("Mount Gravatt", "4122", -27.5370, 153.0800),
            new SuburbSeed("Nundah", "4012", -27.4030, 153.0570),
            new SuburbSeed("Oxley", "4075", -27.5540, 152.9720),
            new SuburbSeed("Paddington", "4064", -27.4620, 152.9970),
            new SuburbSeed("Richlands", "4077", -27.5860, 152.9530),
            new SuburbSeed("Runcorn", "4113", -27.5980, 153.0770),
            new SuburbSeed("Sherwood", "4075", -27.5310, 152.9850),
            new SuburbSeed("South Brisbane", "4101", -27.4816, 153.0176),
            new SuburbSeed("Sunnybank", "4109", -27.5800, 153.0550),
            new SuburbSeed("Tarragindi", "4121", -27.5260, 153.0460),
            new SuburbSeed("Taringa", "4068", -27.4920, 152.9780),
            new SuburbSeed("The Gap", "4061", -27.4440, 152.9440),
            new SuburbSeed("Toowong", "4066", -27.4840, 152.9920),
            new SuburbSeed("West End", "4101", -27.4830, 153.0100),
            new SuburbSeed("Windsor", "4030", -27.4380, 153.0290),
            new SuburbSeed("Woolloongabba", "4102", -27.4920, 153.0350),
            new SuburbSeed("Wynnum", "4178", -27.4450, 153.1730));

    /** Sample sightings weighted towards recent dates so the default 30-day filter shows them. */
    private static final List<SightingSeed> SIGHTING_SEEDS = List.of(
            // Cane Toad
            new SightingSeed("Kangaroo Point", "Cane Toad",
                    "Hopping along the riverwalk after dusk", 0, 19),
            new SightingSeed("Kedron", "Cane Toad",
                    "Under the outdoor light near the pool", 1, 21),
            new SightingSeed("Gordon Park", "Cane Toad",
                    "On the lawn after overnight rain", 2, 6),
            new SightingSeed("Kedron", "Cane Toad",
                    "In the drain beside the footpath", 3, 20),
            new SightingSeed("Toowong", "Cane Toad",
                    "Crossing the driveway at dusk", 4, 19),
            new SightingSeed("Indooroopilly", "Cane Toad",
                    "Near the creek at the end of the street", 5, 22),
            new SightingSeed("West End", "Cane Toad",
                    "Sitting beside the compost bin", 6, 5),
            new SightingSeed("Chermside", "Cane Toad",
                    "Under the street light on the corner", 8, 21),
            new SightingSeed("Bulimba", "Cane Toad",
                    "In the gutter outside the cafe", 10, 23),
            new SightingSeed("Nundah", "Cane Toad",
                    "On the patio after dark", 14, 20),
            new SightingSeed("Wynnum", "Cane Toad",
                    "In the garden bed by the fence", 2, 18),

            // Fire Ant - the south-west cluster the eradication program watches
            new SightingSeed("Inala", "Fire Ant",
                    "Mound beside the footpath on Poinsettia Street", 1, 9),
            new SightingSeed("Inala", "Fire Ant",
                    "Loose mound in the front yard - reported from a distance", 4, 15),
            new SightingSeed("Inala", "Fire Ant",
                    "Mound near the playground edge", 9, 11),
            new SightingSeed("Inala", "Fire Ant",
                    "Found while gardening, kept clear and marked", 21, 8),
            new SightingSeed("Forest Lake", "Fire Ant",
                    "Mound in the park near the lake", 2, 10),
            new SightingSeed("Forest Lake", "Fire Ant",
                    "Nest along the back fence line", 11, 16),
            new SightingSeed("Darra", "Fire Ant",
                    "Mound by the station car park", 3, 7),
            new SightingSeed("Darra", "Fire Ant",
                    "Nest in the garden mulch", 17, 14),
            new SightingSeed("Oxley", "Fire Ant",
                    "Mound on the nature strip", 6, 12),
            new SightingSeed("Oxley", "Fire Ant",
                    "Nest under the clothesline", 26, 9),
            new SightingSeed("Richlands", "Fire Ant",
                    "Mound near the estate walking path", 5, 10),
            new SightingSeed("Algester", "Fire Ant",
                    "Mound beside the school fence", 7, 8),
            new SightingSeed("Calamvale", "Fire Ant",
                    "Nest in the back corner of the yard", 13, 17),
            new SightingSeed("Sunnybank", "Fire Ant",
                    "Mound along the driveway edge", 12, 13),
            new SightingSeed("Runcorn", "Fire Ant",
                    "Ants swarming near the letterbox", 19, 15),

            // Water Hyacinth - the creeks and stormwater basins
            new SightingSeed("Kenmore", "Water Hyacinth",
                    "Thick mat on the creek behind the sports field", 3, 11),
            new SightingSeed("Indooroopilly", "Water Hyacinth",
                    "Clump drifting near the rowing club", 7, 9),
            new SightingSeed("Sherwood", "Water Hyacinth",
                    "Blocking the creek near the park", 10, 14),
            new SightingSeed("Mitchelton", "Water Hyacinth",
                    "Clump caught on the bank of Kedron Brook", 2, 8),
            new SightingSeed("West End", "Water Hyacinth",
                    "Floating in the stormwater basin", 15, 12),
            new SightingSeed("Carindale", "Water Hyacinth",
                    "Mat of weed along Bulimba Creek", 5, 10),

            // Common Myna
            new SightingSeed("Windsor", "Common Myna",
                    "Pair nesting in the roof gutter", 4, 7),
            new SightingSeed("Coorparoo", "Common Myna",
                    "Flock around the outdoor tables", 6, 12),
            new SightingSeed("Sunnybank", "Common Myna",
                    "Feeding on the lawn in a group of six", 9, 16),
            new SightingSeed("Wynnum", "Common Myna",
                    "Nesting in the palm by the esplanade", 16, 9),
            new SightingSeed("Highgate Hill", "Common Myna",
                    "On the powerlines outside the school", 22, 15),

            // European Red Fox
            new SightingSeed("The Gap", "European Red Fox",
                    "Crossed the road near the reserve at dawn", 4, 5),
            new SightingSeed("Kenmore", "European Red Fox",
                    "Den under the back shed", 18, 7),
            new SightingSeed("Aspley", "European Red Fox",
                    "Sighted along the golf course fence line", 27, 6),

            // Common Lantana
            new SightingSeed("The Gap", "Common Lantana",
                    "Thicket along the creek bank", 8, 10),
            new SightingSeed("Mount Gravatt", "Common Lantana",
                    "Clump on the fire trail", 12, 9),
            new SightingSeed("Kenmore", "Common Lantana",
                    "Large thicket behind the park", 24, 13),

            // Noisy Miner
            new SightingSeed("Ashgrove", "Noisy Miner",
                    "Noisy flock in the street trees", 6, 8),
            new SightingSeed("Tarragindi", "Noisy Miner",
                    "Harassing smaller birds in the garden", 11, 10),
            new SightingSeed("Camp Hill", "Noisy Miner",
                    "Group feeding in the bottlebrush", 15, 9),

            // Asian House Gecko
            new SightingSeed("Paddington", "Asian House Gecko",
                    "On the kitchen wall at night", 3, 22),
            new SightingSeed("South Brisbane", "Asian House Gecko",
                    "Calling from behind the shutters", 9, 21),

            // Striped Marsh Frog
            new SightingSeed("Milton", "Striped Marsh Frog",
                    "Calling from the drain after rain", 1, 22),
            new SightingSeed("Belmont", "Striped Marsh Frog",
                    "In the wetland near the golf course", 7, 20));

    private static List<Species> getSeedSpecies() {
        return List.of(
                // Fire Ant
                new SpeciesBuilder()
                        .commonName("Red Imported Fire Ant")
                        .scientificName("Solenopsis invicta")
                        .threatLevel(Species.ThreatLevel.HIGH)
                        .aggression(85).stingSeverity(80).spreadRisk(95)
                        .typicalHabitat("Open, disturbed ground: lawns, playing fields, roadsides, garden mulch and along footpaths")
                        .sizeMinMm(2).sizeMaxMm(6)
                        .disposalGuidance("Do not disturb the mound - disturbed ants scatter and start new colonies. Keep children and pets away, mark the spot, and report it to the National Fire Ant Eradication Program. Do not attempt to treat the nest yourself; eradication teams apply the registered bait.")
                        .alaGuid("fake:solenopsis-invicta")
                        .tags(List.of("Invasive", "Stinging", "Human health risk"))
                        .build(),

                // Cane Toad
                new SpeciesBuilder()
                        .commonName("Cane Toad")
                        .scientificName("Rhinella marina")
                        .threatLevel(Species.ThreatLevel.HIGH)
                        .aggression(40).stingSeverity(75).spreadRisk(90)
                        .typicalHabitat("Moist gardens, drains, under buildings, and around outdoor lights and water features")
                        .sizeMinMm(100).sizeMaxMm(150)
                        .disposalGuidance("Never squeeze or handle a cane toad bare-handed - the shoulder toxin can irritate skin and can kill pets that mouth the toad. Report sightings to Biosecurity Queensland and ask about a collection or control program in your area before acting. If removal is permitted where you live, wear gloves and follow the humane method approved by your council.")
                        .alaGuid("fake:rhinella-marina")
                        .tags(List.of("Invasive", "Toxic", "Pest"))
                        .build(),

                // Asian House Gecko
                new SpeciesBuilder()
                        .commonName("Asian House Gecko")
                        .scientificName("Hemidactylus frenatus")
                        .threatLevel(Species.ThreatLevel.LOW)
                        .aggression(10).stingSeverity(5).spreadRisk(55)
                        .typicalHabitat("Warm buildings, sheds, patios and tree trunks near artificial light")
                        .sizeMinMm(100).sizeMaxMm(130)
                        .disposalGuidance("No action needed - house geckos are harmless and eat insects. To discourage them indoors, seal gaps around doors and windows, reduce outdoor lights that attract moths, and never use sticky or glue traps. If one gets inside, guide it into a container and release it outside near shelter.")
                        .alaGuid("fake:hemidactylus-frenatus")
                        .tags(List.of("Introduced"))
                        .build(),

                // Common Myna
                new SpeciesBuilder()
                        .commonName("Common Myna")
                        .scientificName("Acridotheres tristis")
                        .threatLevel(Species.ThreatLevel.MEDIUM)
                        .aggression(50).stingSeverity(5).spreadRisk(70)
                        .typicalHabitat("Suburban and urban areas: lawns, parks, car parks, rooflines and tree hollows")
                        .sizeMinMm(230).sizeMaxMm(260)
                        .disposalGuidance("Do not harm or relocate mynas yourself. Remove the food sources that feed them - uncovered pet food, seed on the ground and accessible rubbish. If mynas are nesting in roof cavities, block entry points after the chicks have fledged. Ask your local council whether a trapping program operates in your area.")
                        .alaGuid("fake:acridotheres-tristis")
                        .tags(List.of("Invasive", "Cavity nester"))
                        .build(),

                // European Red Fox
                new SpeciesBuilder()
                        .commonName("European Red Fox")
                        .scientificName("Vulpes vulpes")
                        .threatLevel(Species.ThreatLevel.HIGH)
                        .aggression(45).stingSeverity(10).spreadRisk(85)
                        .typicalHabitat("Bushland edges, farmland, parklands and the rural-urban fringe, denning in earth burrows")
                        .sizeMinMm(600).sizeMaxMm(750)
                        .disposalGuidance("Do not approach or feed foxes - they can carry diseases and may bite if cornered. Secure bins and remove pet food so foxes are not attracted to your yard. Report foxes denning on your property, stock losses or sick animals to Biosecurity Queensland; baiting and shooting are regulated and usually run by councils or coordinated landcare groups.")
                        .alaGuid("fake:vulpes-vulpes")
                        .tags(List.of("Invasive", "Declared pest", "Predator"))
                        .build(),

                // Common Lantana
                new SpeciesBuilder()
                        .commonName("Common Lantana")
                        .scientificName("Lantana camara")
                        .threatLevel(Species.ThreatLevel.HIGH)
                        .aggression(25).stingSeverity(25).spreadRisk(85)
                        .typicalHabitat("Forest edges, roadsides, creek lines and cleared or grazed land")
                        .sizeMinMm(1000).sizeMaxMm(4000)
                        .disposalGuidance("Wear gloves, long sleeves and eye protection - the stems are prickly and the foliage can irritate skin. Hand-pull or scrape small plants, removing the root crown; for large thickets call a licensed contractor. Do not burn freshly pulled plants, as heat can trigger seeds on the ground, and do not dump material where it can regrow.")
                        .alaGuid("fake:lantana-camara")
                        .tags(List.of("Invasive", "Weed", "Toxic to stock"))
                        .build(),

                // Green Tree Frog
                new SpeciesBuilder()
                        .commonName("Green Tree Frog")
                        .scientificName("Litoria caerulea")
                        .threatLevel(Species.ThreatLevel.LOW)
                        .aggression(5).stingSeverity(5).spreadRisk(5)
                        .typicalHabitat("Trees and buildings near water; garden ponds, rain gutters and sheltered corners")
                        .sizeMinMm(70).sizeMaxMm(100)
                        .disposalGuidance("Native and protected - enjoy it and leave it in place. Frogs often investigate houses after rain; if one is indoors, guide it into a container and release it gently into garden cover at dusk. Keep pools and tanks covered so frogs do not drown, and avoid garden chemicals that wash into their breeding water.")
                        .alaGuid("fake:litoria-caerulea")
                        .tags(List.of("Native", "Protected"))
                        .build(),

                // Striped Marsh Frog
                new SpeciesBuilder()
                        .commonName("Striped Marsh Frog")
                        .scientificName("Limnodynastes peronii")
                        .threatLevel(Species.ThreatLevel.LOW)
                        .aggression(5).stingSeverity(5).spreadRisk(5)
                        .typicalHabitat("Ponds, wetlands, dams and flooded grass; lawns after heavy rain")
                        .sizeMinMm(50).sizeMaxMm(70)
                        .disposalGuidance("Native - leave it in place and protect backyard wetlands. Avoid draining ponds during the breeding season, keep garden chemicals out of waterways, and never release pets or exotic animals into frog habitat. Reporting is not required for native species.")
                        .alaGuid("fake:limnodynastes-peronii")
                        .tags(List.of("Native"))
                        .build(),

                // Noisy Miner
                new SpeciesBuilder()
                        .commonName("Noisy Miner")
                        .scientificName("Manorina melanocephala")
                        .threatLevel(Species.ThreatLevel.MEDIUM)
                        .aggression(55).stingSeverity(5).spreadRisk(30)
                        .typicalHabitat("Open woodland, parklands and suburban gardens with large trees and short grass")
                        .sizeMinMm(230).sizeMaxMm(280)
                        .disposalGuidance("Native - no removal is needed and it is not legal to harm them. To reduce their dominance in your garden, plant dense, layered native understorey and shrubs, which give smaller birds somewhere to hide and feed, and reduce lawn area so the habitat is less miner-friendly.")
                        .alaGuid("fake:manorina-melanocephala")
                        .tags(List.of("Native", "Aggressive to other birds"))
                        .build()
        );
    }

    /** Simple builder for seed data. */
    private static final class SpeciesBuilder {
        private String commonName, scientificName, typicalHabitat, disposalGuidance, alaGuid, photoPath;
        private Species.ThreatLevel threatLevel;
        private int aggression, stingSeverity, spreadRisk;
        private double sizeMinMm, sizeMaxMm;
        private List<String> tags;

        SpeciesBuilder commonName(String v) { commonName = v; return this; }
        SpeciesBuilder scientificName(String v) { scientificName = v; return this; }
        SpeciesBuilder threatLevel(Species.ThreatLevel v) { threatLevel = v; return this; }
        SpeciesBuilder aggression(int v) { aggression = v; return this; }
        SpeciesBuilder stingSeverity(int v) { stingSeverity = v; return this; }
        SpeciesBuilder spreadRisk(int v) { spreadRisk = v; return this; }
        SpeciesBuilder typicalHabitat(String v) { typicalHabitat = v; return this; }
        SpeciesBuilder sizeMinMm(double v) { sizeMinMm = v; return this; }
        SpeciesBuilder sizeMaxMm(double v) { sizeMaxMm = v; return this; }
        SpeciesBuilder disposalGuidance(String v) { disposalGuidance = v; return this; }
        SpeciesBuilder alaGuid(String v) { alaGuid = v; return this; }
        SpeciesBuilder photoPath(String v) { photoPath = v; return this; }
        SpeciesBuilder tags(List<String> v) { tags = v; return this; }

        Species build() {
            Species s = new Species();
            s.setCommonName(commonName);
            s.setScientificName(scientificName);
            s.setThreatLevel(threatLevel);
            s.setAggression(aggression);
            s.setStingSeverity(stingSeverity);
            s.setSpreadRisk(spreadRisk);
            s.setTypicalHabitat(typicalHabitat);
            s.setSizeMinMm(sizeMinMm);
            s.setSizeMaxMm(sizeMaxMm);
            s.setDisposalGuidance(disposalGuidance);
            s.setPhotoPath(photoPath);
            s.setAlaGuid(alaGuid);
            s.setTags(tags);
            return s;
        }
    }
}