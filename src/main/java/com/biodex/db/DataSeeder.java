package com.biodex.db;

import com.biodex.dao.SpeciesDAO;
import com.biodex.model.Species;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Seeds the local {@code species} table with the curated pest data that matches the ONNX model labels.
 * Runs after schema initialisation so the Pest Detail screen can show local data.
 */
public final class DataSeeder {

    private DataSeeder() {
    }

    /** Seeds species if the table is empty. Uses the provided connection. */
    public static void seedIfEmpty(Connection connection) {
        try {
            // Check if species table already has data
            try (Statement stmt = connection.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) FROM species")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // Already seeded
                }
            }

            SpeciesDAO dao = new SpeciesDAO(connection);
            for (Species s : getSeedSpecies()) {
                insertSpecies(connection, s);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to seed species data", e);
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