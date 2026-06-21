package fieldmind.research.app.features.field.data.species

/**
 * Comprehensive taxonomy reference data for the species registry picker dialogs.
 * Provides common values for each taxonomic rank used across FieldMind.
 */
object TaxonomyData {

    /** The six kingdoms of life used in biological classification. */
    val KINGDOMS = listOf(
        "Animalia",
        "Plantae",
        "Fungi",
        "Protista",
        "Archaea",
        "Bacteria"
    )

    /** Common phyla grouped by kingdom for context-aware suggestions. */
    val KINGDOM_PHYLA: Map<String, List<String>> = mapOf(
        "Animalia" to listOf(
            "Chordata", "Arthropoda", "Mollusca", "Annelida", "Cnidaria",
            "Echinodermata", "Platyhelminthes", "Nematoda", "Porifera",
            "Bryozoa", "Brachiopoda", "Chaetognatha", "Hemichordata",
            "Nemertea", "Onychophora", "Phoronida", "Rotifera",
            "Sipuncula", "Tardigrada", "Xenoturbellida"
        ),
        "Plantae" to listOf(
            "Angiospermae", "Coniferophyta", "Cycadophyta", "Ginkgophyta",
            "Gnetophyta", "Pteridophyta", "Lycopodiophyta", "Bryophyta",
            "Marchantiophyta", "Anthocerotophyta", "Chlorophyta", "Charophyta"
        ),
        "Fungi" to listOf(
            "Ascomycota", "Basidiomycota", "Zygomycota", "Glomeromycota",
            "Chytridiomycota", "Blastocladiomycota", "Neocallimastigomycota",
            "Microsporidia"
        ),
        "Protista" to listOf(
            "Ciliophora", "Sarcomastigophora", "Apicomplexa", "Dinoflagellata",
            "Ochrophyta", "Rhodophyta", "Euglenozoa", "Amoebozoa",
            "Choanozoa", "Foraminifera", "Radiolaria"
        ),
        "Archaea" to listOf(
            "Crenarchaeota", "Euryarchaeota", "Thaumarchaeota", "Nanoarchaeota",
            "Korarchaeota"
        ),
        "Bacteria" to listOf(
            "Proteobacteria", "Firmicutes", "Actinobacteria", "Bacteroidetes",
            "Cyanobacteria", "Spirochaetes", "Chlamydiae", "Thermotogae",
            "Deinococcus-Thermus"
        )
    )

    /** Common classes within each phylum. Organized by kingdom→phylum→classes. */
    val PHYLUM_CLASSES: Map<String, List<String>> = mapOf(
        // Animalia - Chordata
        "Chordata" to listOf(
            "Mammalia", "Aves", "Reptilia", "Amphibia", "Actinopterygii",
            "Chondrichthyes", "Agnatha", "Sarcopterygii", "Cephalaspidomorphi",
            "Myxini", "Ascidiacea", "Thaliacea", "Appendicularia"
        ),
        // Animalia - Arthropoda
        "Arthropoda" to listOf(
            "Insecta", "Arachnida", "Malacostraca", "Chilopoda", "Diplopoda",
            "Branchiopoda", "Maxillopoda", "Ostracoda", "Merostomata",
            "Pycnogonida", "Collembola", "Protura", "Diplura"
        ),
        // Animalia - Mollusca
        "Mollusca" to listOf(
            "Gastropoda", "Bivalvia", "Cephalopoda", "Polyplacophora",
            "Scaphopoda", "Monoplacophora", "Aplacophora"
        ),
        // Animalia - Annelida
        "Annelida" to listOf(
            "Clitellata", "Polychaeta", "Hirudinea", "Aeolosomatida",
            "Sipuncula"
        ),
        // Animalia - Cnidaria
        "Cnidaria" to listOf(
            "Anthozoa", "Hydrozoa", "Scyphozoa", "Cubozoa", "Staurozoa"
        ),
        // Animalia - Echinodermata
        "Echinodermata" to listOf(
            "Asteroidea", "Echinoidea", "Holothuroidea", "Ophiuroidea",
            "Crinoidea"
        ),
        // Plantae - Angiospermae (Magnoliophyta)
        "Angiospermae" to listOf(
            "Magnoliopsida", "Liliopsida", "Rosopsida", "Asteropsida",
            "Caryophyllales", "Fabales", "Lamiales", "Solanales",
            "Poales", "Asparagales", "Arecales", "Brassicales",
            "Malvales", "Sapindales", "Ericales", "Gentianales"
        ),
        // Plantae - Coniferophyta
        "Coniferophyta" to listOf(
            "Pinopsida"
        ),
        // Plantae - Pteridophyta
        "Pteridophyta" to listOf(
            "Polypodiopsida", "Marattiopsida", "Ophioglossopsida",
            "Psilotopsida"
        ),
        // Plantae - Bryophyta
        "Bryophyta" to listOf(
            "Bryopsida", "Sphagnopsida", "Andreaeopsida", "Polytrichopsida"
        ),
        // Fungi - Ascomycota
        "Ascomycota" to listOf(
            "Sordariomycetes", "Dothideomycetes", "Leotiomycetes",
            "Eurotiomycetes", "Saccharomycetes", "Pezizomycetes",
            "Lecanoromycetes", "Laboulbeniomycetes", "Neolectomycetes",
            "Pneumocystidomycetes", "Schizosaccharomycetes", "Taphrinomycetes"
        ),
        // Fungi - Basidiomycota
        "Basidiomycota" to listOf(
            "Agaricomycetes", "Pucciniomycetes", "Ustilaginomycetes",
            "Tremellomycetes", "Dacrymycetes", "Exobasidiomycetes",
            "Wallemiomycetes", "Mixiomycetes", "Atractiellomycetes",
            "Cystobasidiomycetes", "Microbotryomycetes"
        )
    )

    /** Common orders within each class. Organized by phylum→class→orders. */
    val CLASS_ORDERS: Map<String, List<String>> = mapOf(
        // Chordata - Mammalia
        "Mammalia" to listOf(
            "Primates", "Carnivora", "Rodentia", "Chiroptera", "Artiodactyla",
            "Perissodactyla", "Lagomorpha", "Eulipotyphla", "Didelphimorphia",
            "Diprotodontia", "Cetacea", "Proboscidea", "Sirenia",
            "Cingulata", "Pilosa", "Monotremata", "Dasyuromorphia",
            "Macroscelidea", "Scandentia", "Dermoptera", "Pholidota",
            "Tubulidentata", "Hyracoidea", "Afrosoricida"
        ),
        // Chordata - Aves
        "Aves" to listOf(
            "Passeriformes", "Accipitriformes", "Falconiformes", "Strigiformes",
            "Piciformes", "Columbiformes", "Anseriformes", "Galliformes",
            "Gruiformes", "Charadriiformes", "Ciconiiformes", "Pelecaniformes",
            "Suliformes", "Procellariiformes", "Sphenisciformes", "Gaviiformes",
            "Podicipediformes", "Psittaciformes", "Apodiformes", "Coraciiformes",
            "Bucerotiformes", "Cuculiformes", "Caprimulgiformes", "Trochiliformes",
            "Phaethontiformes", "Eurypygiformes", "Trogoniformes", "Coliiformes",
            "Leptosomiformes", "Cariamiformes", "Otidiformes", "Musophagiformes",
            "Phoenicopteriformes", "Pterocliformes", "Opisthocomiformes",
            "Struthioniformes", "Rheiformes", "Casuariiformes", "Apterygiformes"
        ),
        // Chordata - Reptilia
        "Reptilia" to listOf(
            "Squamata", "Testudines", "Crocodylia", "Rhynchocephalia"
        ),
        // Chordata - Amphibia
        "Amphibia" to listOf(
            "Anura", "Caudata", "Gymnophiona"
        ),
        // Chordata - Actinopterygii
        "Actinopterygii" to listOf(
            "Cypriniformes", "Perciformes", "Siluriformes", "Salmoniformes",
            "Anguilliformes", "Clupeiformes", "Gadiformes", "Scorpaeniformes",
            "Tetraodontiformes", "Pleuronectiformes", "Atheriniformes",
            "Beloniformes", "Characiformes", "Gymnotiformes", "Lepisosteiformes",
            "Acipenseriformes", "Amiiformes", "Osteoglossiformes",
            "Ophidiiformes", "Batrachoidiformes", "Lophiiformes",
            "Gobiiformes", "Labriformes", "Cichliformes", "Mugiliformes",
            "Esociformes", "Osmeriformes", "Stomiiformes", "Aulopiformes",
            "Myctophiformes", "Lampridiformes", "Polymixiiformes",
            "Beryciformes", "Zeiformes", "Syngnathiformes"
        ),
        // Arthropoda - Insecta
        "Insecta" to listOf(
            "Coleoptera", "Lepidoptera", "Hymenoptera", "Diptera",
            "Hemiptera", "Odonata", "Orthoptera", "Blattodea",
            "Isoptera", "Phasmatodea", "Mantodea", "Thysanoptera",
            "Psocoptera", "Phthiraptera", "Siphonaptera", "Mecoptera",
            "Neuroptera", "Trichoptera", "Ephemeroptera", "Plecoptera",
            "Dermaptera", "Embioptera", "Zoraptera", "Grylloblattodea",
            "Mantophasmatodea", "Archaeognatha", "Zygentoma",
            "Raphidioptera", "Megaloptera", "Strepsiptera"
        ),
        // Arthropoda - Arachnida
        "Arachnida" to listOf(
            "Araneae", "Scorpiones", "Acari", "Opiliones",
            "Pseudoscorpiones", "Solifugae", "Palpigradi", "Ricinulei",
            "Uropygi", "Amblypygi", "Schizomida"
        ),
        // Arthropoda - Malacostraca
        "Malacostraca" to listOf(
            "Decapoda", "Amphipoda", "Isopoda", "Euphausiacea",
            "Stomatopoda", "Mysida", "Cumacea", "Tanaidacea"
        ),
        // Mollusca - Gastropoda
        "Gastropoda" to listOf(
            "Stylommatophora", "Neogastropoda", "Architaenioglossa",
            "Littorinimorpha", "Caenogastropoda", "Vetigastropoda",
            "Nudibranchia", "Pulmonata", "Basommatophora",
            "Patellogastropoda", "Neritopsina", "Cocculiniformia"
        ),
        // Mollusca - Bivalvia
        "Bivalvia" to listOf(
            "Venerida", "Mytilida", "Pectinida", "Ostreida",
            "Arcida", "Unionida", "Cardiida", "Lucinida",
            "Myida", "Solemyida", "Nuculanida", "Limida"
        ),
        // Mollusca - Cephalopoda
        "Cephalopoda" to listOf(
            "Octopoda", "Teuthida", "Sepiida", "Myopsida",
            "Oegopsida", "Nautilida", "Vampyromorpha",
            "Spirulida"
        ),
        // Angiospermae - Magnoliopsida (dicots)
        "Magnoliopsida" to listOf(
            "Fabales", "Rosales", "Brassicales", "Malvales",
            "Sapindales", "Ericales", "Gentianales", "Lamiales",
            "Solanales", "Asterales", "Apiales", "Dipsacales",
            "Ranunculales", "Caryophyllales", "Saxifragales",
            "Vitales", "Myrtales", "Malpighiales", "Celastrales",
            "Oxalidales", "Cucurbitales", "Fagales", "Proteales",
            "Cornales", "Aquifoliales", "Escalloniales", "Boraginales"
        ),
        // Angiospermae - Liliopsida (monocots)
        "Liliopsida" to listOf(
            "Poales", "Asparagales", "Arecales", "Liliales",
            "Zingiberales", "Commelinales", "Alismatales",
            "Pandanales", "Dioscoreales", "Petrosaviales",
            "Acorales", "Dasypogonaceae"
        ),
        // Ascomycota - Sordariomycetes
        "Sordariomycetes" to listOf(
            "Xylariales", "Hypocreales", "Sordariales", "Diaporthales",
            "Ophiostomatales", "Magnaporthales", "Microascales",
            "Phyllachorales", "Chaetosphaeriales", "Coniochaetales"
        ),
        // Basidiomycota - Agaricomycetes
        "Agaricomycetes" to listOf(
            "Agaricales", "Boletales", "Russulales", "Polyporales",
            "Hymenochaetales", "Cantharellales", "Corticiales",
            "Atheliales", "Geastrales", "Gomphales", "Hysterangiales",
            "Phallales", "Thelephorales", "Trechisporales",
            "Auriculariales", "Dacrymycetales", "Sebacinales"
        )
    )

    /** Common families within each order. A focused subset of the most frequently encountered families. */
    val ORDER_FAMILIES: Map<String, List<String>> = mapOf(
        // Carnivora
        "Carnivora" to listOf(
            "Felidae", "Canidae", "Ursidae", "Mustelidae", "Procyonidae",
            "Mephitidae", "Otariidae", "Phocidae", "Viverridae",
            "Herpestidae", "Hyaenidae", "Nandiniidae", "Ailuridae"
        ),
        // Primates
        "Primates" to listOf(
            "Hominidae", "Cercopithecidae", "Callitrichidae", "Cebidae",
            "Lemuridae", "Lorisidae", "Galagidae", "Tarsiidae",
            "Indriidae", "Cheirogaleidae", "Daubentoniidae",
            "Pitheciidae", "Atelidae", "Aotidae"
        ),
        // Rodentia
        "Rodentia" to listOf(
            "Muridae", "Sciuridae", "Cricetidae", "Castoridae", "Erethizontidae",
            "Geomyidae", "Heteromyidae", "Dipodidae", "Gliridae",
            "Caviidae", "Chinchillidae", "Dasyproctidae", "Cuniculidae",
            "Bathyergidae", "Hystricidae", "Myocastoridae", "Octodontidae"
        ),
        // Passeriformes
        "Passeriformes" to listOf(
            "Corvidae", "Paridae", "Fringillidae", "Passeridae", "Sturnidae",
            "Turdidae", "Muscicapidae", "Sylviidae", "Troglodytidae",
            "Hirundinidae", "Motacillidae", "Alaudidae", "Emberizidae",
            "Cardinalidae", "Icteridae", "Parulidae", "Thraupidae",
            "Tyrannidae", "Vireonidae", "Polioptilidae", "Certhiidae",
            "Sittidae", "Regulidae", "Bombycillidae", "Laniidae",
            "Mimidae", "Prunellidae", "Nectariniidae", "Ploceidae",
            "Estrildidae", "Viduidae", "Oriolidae", "Dicruridae",
            "Artamidae", "Cracticidae", "Pachycephalidae"
        ),
        // Accipitriformes
        "Accipitriformes" to listOf(
            "Accipitridae", "Pandionidae", "Cathartidae", "Sagittariidae"
        ),
        // Falconiformes
        "Falconiformes" to listOf(
            "Falconidae"
        ),
        // Strigiformes
        "Strigiformes" to listOf(
            "Strigidae", "Tytonidae"
        ),
        // Anseriformes
        "Anseriformes" to listOf(
            "Anatidae", "Anhimidae", "Anseranatidae"
        ),
        // Squamata
        "Squamata" to listOf(
            "Colubridae", "Viperidae", "Pythonidae", "Boidae", "Elapidae",
            "Lacertidae", "Iguanidae", "Teiidae", "Scincidae", "Gekkonidae",
            "Chamaeleonidae", "Agamidae", "Cordylidae", "Anguidae",
            "Helodermatidae", "Xantusiidae", "Phrynosomatidae",
            "Dactyloidae", "Eublepharidae", "Sphenodontidae"
        ),
        // Anura
        "Anura" to listOf(
            "Ranidae", "Bufonidae", "Hylidae", "Dendrobatidae", "Leptodactylidae",
            "Microhylidae", "Pipidae", "Bombinatoridae", "Pelobatidae",
            "Scaphiopodidae", "Craugastoridae", "Eleutherodactylidae",
            "Centrolenidae", "Rhacophoridae", "Hyperoliidae",
            "Myobatrachidae", "Hylodidae", "Ceratophryidae"
        ),
        // Coleoptera
        "Coleoptera" to listOf(
            "Carabidae", "Scarabaeidae", "Cerambycidae", "Curculionidae",
            "Chrysomelidae", "Coccinellidae", "Elateridae", "Silphidae",
            "Staphylinidae", "Tenebrionidae", "Buprestidae", "Lucanidae",
            "Passalidae", "Dytiscidae", "Gyrinidae", "Hydrophilidae",
            "Lampyridae", "Cantharidae", "Melyridae", "Cleridae",
            "Mordellidae", "Meloidae", "Anthicidae", "Nitidulidae",
            "Erotylidae", "Cucujidae", "Bostrichidae", "Anobiidae"
        ),
        // Lepidoptera
        "Lepidoptera" to listOf(
            "Nymphalidae", "Papilionidae", "Pieridae", "Lycaenidae",
            "Hesperiidae", "Saturniidae", "Sphingidae", "Noctuidae",
            "Erebidae", "Geometridae", "Arctiidae", "Lasiocampidae",
            "Lymantriidae", "Tortricidae", "Pyralidae", "Crambidae",
            "Bombycidae", "Danaidae", "Morphidae", "Riodinidae"
        ),
        // Hymenoptera
        "Hymenoptera" to listOf(
            "Apidae", "Formicidae", "Vespidae", "Ichneumonidae",
            "Braconidae", "Tenthredinidae", "Megachilidae",
            "Halictidae", "Andrenidae", "Colletidae", "Sphecidae",
            "Crabronidae", "Chalcididae", "Mymaridae", "Pteromalidae",
            "Eulophidae", "Encyrtidae", "Cynipidae", "Figitidae",
            "Siricidae", "Xiphydriidae", "Orussidae"
        ),
        // Araneae
        "Araneae" to listOf(
            "Araneidae", "Theridiidae", "Salticidae", "Lycosidae",
            "Linyphiidae", "Thomisidae", "Tetragnathidae", "Pholcidae",
            "Agelenidae", "Gnaphosidae", "Clubionidae", "Dictynidae",
            "Amaurobiidae", "Corinnidae", "Anyphaenidae", "Sparassidae",
            "Oxyopidae", "Mimetidae", "Uloboridae", "Deinopidae",
            "Theraphosidae", "Filistatidae", "Segestriidae", "Dysderidae"
        ),
        // Decapoda
        "Decapoda" to listOf(
            "Portunidae", "Cancridae", "Grapsidae", "Majidae", "Paguridae",
            "Astacidae", "Cambaridae", "Nephropidae", "Palinuridae",
            "Penaeidae", "Palaemonidae", "Crangonidae", "Alpheidae",
            "Homolidae", "Lithodidae", "Ocypodidae", "Gecarcinidae",
            "Coenobitidae", "Galatheidae", "Porcellanidae"
        ),
        // Fabales
        "Fabales" to listOf(
            "Fabaceae", "Polygalaceae", "Quillajaceae", "Surianaceae"
        ),
        // Rosales
        "Rosales" to listOf(
            "Rosaceae", "Moraceae", "Urticaceae", "Rhamnaceae",
            "Ulmaceae", "Cannabaceae", "Elaeagnaceae", "Barbeyaceae"
        ),
        // Asparagales
        "Asparagales" to listOf(
            "Orchidaceae", "Asparagaceae", "Amaryllidaceae", "Iridaceae",
            "Asphodelaceae", "Xanthorrhoeaceae", "Alliaceae",
            "Hyacinthaceae", "Laxmanniaceae", "Ruscaceae"
        ),
        // Poales
        "Poales" to listOf(
            "Poaceae", "Cyperaceae", "Juncaceae", "Bromeliaceae",
            "Typhaceae", "Eriocaulaceae", "Xyridaceae", "Restionaceae",
            "Mayacaceae", "Rapateaceae", "Thurniaceae"
        ),
        // Agaricales
        "Agaricales" to listOf(
            "Agaricaceae", "Amanitaceae", "Bolbitiaceae", "Clavariaceae",
            "Cortinariaceae", "Entolomataceae", "Hygrophoraceae",
            "Hymenogastraceae", "Inocybaceae", "Lyophyllaceae",
            "Marasmiaceae", "Mycenaceae", "Omphalotaceae",
            "Physalacriaceae", "Pleurotaceae", "Pluteaceae",
            "Psathyrellaceae", "Schizophyllaceae", "Strophariaceae",
            "Tricholomataceae", "Tulostomataceae"
        ),
        // Boletales
        "Boletales" to listOf(
            "Boletaceae", "Suillaceae", "Paxillaceae", "Gomphidiaceae",
            "Gyroporaceae", "Calostomataceae", "Sclerodermataceae",
            "Rhizopogonaceae", "Serpulaceae", "Tapinellaceae"
        )
    )

    /**
     * Get suggested phyla for a given kingdom.
     */
    fun phylaForKingdom(kingdom: String): List<String> =
        KINGDOM_PHYLA[kingdom] ?: KINGDOM_PHYLA.values.flatten().distinct().sorted()

    /**
     * Get suggested classes for a given phylum.
     */
    fun classesForPhylum(phylum: String): List<String> =
        PHYLUM_CLASSES[phylum] ?: PHYLUM_CLASSES.values.flatten().distinct().sorted()

    /**
     * Get suggested orders for a given class.
     */
    fun ordersForClass(className: String): List<String> =
        CLASS_ORDERS[className] ?: CLASS_ORDERS.values.flatten().distinct().sorted()

    /**
     * Get suggested families for a given order.
     */
    fun familiesForOrder(order: String): List<String> =
        ORDER_FAMILIES[order] ?: ORDER_FAMILIES.values.flatten().distinct().sorted()

    /**
     * Get all unique values across all ranks (useful for unfiltered search).
     */
    val ALL_VALUES: Map<String, List<String>> by lazy {
        mapOf(
            "kingdom" to KINGDOMS,
            "phylum" to KINGDOM_PHYLA.values.flatten().distinct().sorted(),
            "class" to PHYLUM_CLASSES.values.flatten().distinct().sorted(),
            "order" to CLASS_ORDERS.values.flatten().distinct().sorted(),
            "family" to ORDER_FAMILIES.values.flatten().distinct().sorted()
        )
    }
}
