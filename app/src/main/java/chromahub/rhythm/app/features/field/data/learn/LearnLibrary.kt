package chromahub.rhythm.app.features.field.data.learn

/**
 * Offline-first learning library for FieldMind.
 *
 * This is a hand-curated, bundled dataset of real, free/open educational resources so the Learn
 * page is useful with no network and no API key. Links point to stable, well-known sources
 * (Wikipedia, OpenStax, Khan Academy, Cornell Lab, USGS, NOAA, Purdue OWL, Project Gutenberg, etc.).
 *
 * For live/online suggestions of papers and books, the Learn page also exposes the optional Gemini
 * assistant. See also [SuggestedOnlineApis] for free APIs that could power future in-app fetching.
 */
data class LearnResource(
    val title: String,
    val kind: String,        // "Article", "Course", "Book", "Paper", "Tool", "Video", "Dataset"
    val url: String,
    val why: String,
    val author: String = ""
)

data class LearnTopic(
    val name: String,
    val summary: String,
    val resources: List<LearnResource>
)

data class LearnCategory(
    val name: String,
    val description: String,
    val topics: List<LearnTopic>
)

/**
 * Free, public APIs that could power live in-app fetching of papers/books (no key or free tier).
 * Surfaced to the user as a proposal; not called automatically.
 */
data class OnlineApiProposal(val name: String, val baseUrl: String, val notes: String)

val SuggestedOnlineApis = listOf(
    OnlineApiProposal("Crossref", "https://api.crossref.org/works?query=", "Free, no key. Search 150M+ scholarly works by topic; returns title, authors, DOI, year."),
    OnlineApiProposal("OpenAlex", "https://api.openalex.org/works?search=", "Free, no key. Open index of papers, authors, venues; great for topic discovery and citations."),
    OnlineApiProposal("Semantic Scholar", "https://api.semanticscholar.org/graph/v1/paper/search?query=", "Free tier. Paper search with abstracts, TLDRs, citation counts, and open-access PDFs."),
    OnlineApiProposal("arXiv", "http://export.arxiv.org/api/query?search_query=", "Free, no key. Preprints in physics, CS, biology, stats; returns abstract + PDF link."),
    OnlineApiProposal("Open Library", "https://openlibrary.org/search.json?q=", "Free, no key. Book search by title/subject; covers, authors, editions, and read/borrow links."),
    OnlineApiProposal("GBIF", "https://api.gbif.org/v1/species/search?q=", "Free, no key. Global biodiversity occurrence + taxonomy for species observations."),
    OnlineApiProposal("iNaturalist", "https://api.inaturalist.org/v1/taxa?q=", "Free, no key. Taxa search with photos; good for species ID context.")
)

val LearnLibrary: List<LearnCategory> = listOf(
    LearnCategory(
        "Scientific thinking",
        "How knowledge is built: questions, evidence, bias, and testable claims.",
        listOf(
            LearnTopic(
                "The scientific method",
                "Turn curiosity into testable questions and separate observation from interpretation.",
                listOf(
                    LearnResource("The scientific method", "Article", "https://en.wikipedia.org/wiki/Scientific_method", "Clear overview of hypotheses, prediction, and testing.", "Wikipedia"),
                    LearnResource("OpenStax Biology 2e: The Study of Life", "Book", "https://openstax.org/books/biology-2e/pages/1-introduction", "Free peer-reviewed textbook chapter on scientific reasoning in biology.", "OpenStax"),
                    LearnResource("Understanding Science (UC Berkeley)", "Article", "https://undsci.berkeley.edu/", "University-built guide to how science really works beyond a simple flowchart.", "UC Berkeley")
                )
            ),
            LearnTopic(
                "Recognizing bias",
                "Spot confirmation bias, observer bias, and sampling bias in your own notes.",
                listOf(
                    LearnResource("List of cognitive biases", "Article", "https://en.wikipedia.org/wiki/List_of_cognitive_biases", "Reference catalog of common reasoning errors.", "Wikipedia"),
                    LearnResource("NIST: Bias in AI and measurement context", "Guide", "https://www.nist.gov/artificial-intelligence/bias", "Government resource for thinking about bias, measurement, and model limits.", "NIST"),
                    LearnResource("Calling Bullshit (UW course)", "Course", "https://www.callingbullshit.org/", "Free university course on spotting misleading data and claims.", "University of Washington")
                )
            )
        )
    ),
    LearnCategory(
        "Field observation",
        "Sharpen what you record in the field: detail, accuracy, and good field notes.",
        listOf(
            LearnTopic(
                "Keeping field notes",
                "Structure notes so they stay useful years later — date, place, conditions, facts.",
                listOf(
                    LearnResource("How to keep a field notebook (Cornell Lab)", "Article", "https://www.birds.cornell.edu/home/keeping-a-field-notebook/", "Practical field-notebook method from ornithologists."),
                    LearnResource("Grinnell method of field notes", "Article", "https://en.wikipedia.org/wiki/Joseph_Grinnell#Field_notes", "The classic, rigorous field-note system naturalists still use.")
                )
            ),
            LearnTopic(
                "Recording observations responsibly",
                "Use citizen-science platforms to log and verify what you see.",
                listOf(
                    LearnResource("iNaturalist", "Tool", "https://www.inaturalist.org/", "Log species observations and get community ID help."),
                    LearnResource("Merlin Bird ID", "Tool", "https://merlin.allaboutbirds.org/", "Identify birds by photo, sound, and a few questions.")
                )
            )
        )
    ),
    LearnCategory(
        "Birds & wildlife",
        "Identify, understand, and track animals you observe.",
        listOf(
            LearnTopic(
                "Bird identification & behavior",
                "Learn field marks, songs, and behavior to ID and interpret birds.",
                listOf(
                    LearnResource("All About Birds (Cornell Lab)", "Article", "https://www.allaboutbirds.org/", "Free species guide with ID, sounds, and life history."),
                    LearnResource("Bird Academy free courses", "Course", "https://academy.allaboutbirds.org/", "Short free lessons on bird biology and ID skills.")
                )
            ),
            LearnTopic(
                "Animal tracking & signs",
                "Read tracks, scat, and signs to know what passed through.",
                listOf(
                    LearnResource("Animal tracking", "Article", "https://en.wikipedia.org/wiki/Animal_track", "Intro to interpreting tracks and field signs.")
                )
            )
        )
    ),
    LearnCategory(
        "Plants & botany",
        "Identify plants and understand how they grow and interact.",
        listOf(
            LearnTopic(
                "Plant identification",
                "Use keys and apps to name plants and record traits.",
                listOf(
                    LearnResource("OpenStax Biology 2e", "Book", "https://openstax.org/details/books/biology-2e", "Free, peer-reviewed biology textbook covering plants."),
                    LearnResource("PlantNet", "Tool", "https://plantnet.org/", "Photo-based plant identification supported by botanists.")
                )
            )
        )
    ),
    LearnCategory(
        "Insects & entomology",
        "Observe and identify the most diverse group of animals on Earth.",
        listOf(
            LearnTopic(
                "Insect identification",
                "Learn orders and families to place an insect quickly.",
                listOf(
                    LearnResource("BugGuide", "Tool", "https://bugguide.net/", "Community-curated North American insect ID reference."),
                    LearnResource("Insect (overview)", "Article", "https://en.wikipedia.org/wiki/Insect", "Anatomy, orders, and life cycles at a glance.")
                )
            )
        )
    ),
    LearnCategory(
        "Geology & rocks",
        "Read the landscape: rocks, minerals, and earth processes.",
        listOf(
            LearnTopic(
                "Rocks & minerals",
                "Tell the three rock types apart and identify common minerals.",
                listOf(
                    LearnResource("USGS education resources", "Article", "https://www.usgs.gov/educational-resources", "Authoritative, free earth-science learning material."),
                    LearnResource("Rock (geology)", "Article", "https://en.wikipedia.org/wiki/Rock_(geology)", "Igneous, sedimentary, and metamorphic basics.")
                )
            )
        )
    ),
    LearnCategory(
        "Weather & climate",
        "Understand and record atmospheric conditions in the field.",
        listOf(
            LearnTopic(
                "Reading the weather",
                "Clouds, fronts, and instruments for honest weather logging.",
                listOf(
                    LearnResource("NOAA JetStream weather school", "Course", "https://www.noaa.gov/jetstream", "Free, in-depth introduction to weather and the atmosphere."),
                    LearnResource("Cloud types (WMO)", "Article", "https://cloudatlas.wmo.int/", "Official cloud atlas for identifying cloud types.")
                )
            )
        )
    ),
    LearnCategory(
        "Water & ecology",
        "Study water bodies, habitats, and how organisms interact.",
        listOf(
            LearnTopic(
                "Freshwater & ecosystems",
                "Basics of water quality and ecological relationships.",
                listOf(
                    LearnResource("USGS Water Science School", "Article", "https://www.usgs.gov/special-topics/water-science-school", "Free lessons on the water cycle and water quality.", "USGS"),
                    LearnResource("EPA: Indicators of water quality", "Guide", "https://www.epa.gov/national-aquatic-resource-surveys/indicators-water-quality", "Practical official indicators for field water observations.", "EPA"),
                    LearnResource("Ecology (overview)", "Article", "https://en.wikipedia.org/wiki/Ecology", "Populations, communities, and ecosystems.", "Wikipedia")
                )
            )
        )
    ),
    LearnCategory(
        "Data & statistics",
        "Collect, summarize, and interpret data without fooling yourself.",
        listOf(
            LearnTopic(
                "Statistics foundations",
                "Means, variation, sampling, and reading charts honestly.",
                listOf(
                    LearnResource("OpenStax Introductory Statistics", "Book", "https://openstax.org/details/books/introductory-statistics", "Free peer-reviewed textbook for descriptive statistics, sampling, and inference.", "OpenStax"),
                    LearnResource("OpenIntro Statistics", "Book", "https://www.openintro.org/book/os/", "Free, widely used statistics textbook with exercises.", "OpenIntro"),
                    LearnResource("Our World in Data", "Dataset", "https://ourworldindata.org/", "Free data and charts on science, environment, and society.", "Our World in Data")
                )
            )
        )
    ),
    LearnCategory(
        "Research writing & ethics",
        "Turn notes into clear, honest, well-cited reports.",
        listOf(
            LearnTopic(
                "Writing & citing",
                "Structure findings and cite sources properly.",
                listOf(
                    LearnResource("Purdue OWL", "Article", "https://owl.purdue.edu/owl/research_and_citation/resources.html", "Free, authoritative writing and citation guides.", "Purdue University"),
                    LearnResource("NCBI Bookshelf: How to read a scientific paper", "Book", "https://www.ncbi.nlm.nih.gov/books/", "Free biomedical books and reports; useful for primary-source reading practice.", "NCBI"),
                    LearnResource("On the Origin of Species (Darwin)", "Book", "https://www.gutenberg.org/ebooks/1228", "Free classic showing careful observation turned into argument.", "Project Gutenberg")
                )
            )
        )
    )
)

/**
 * An ordered "learn this skill" path: a short, opinionated sequence of resources that takes a
 * curious beginner from zero to competent on a single topic. Steps reuse [LearnResource] so they
 * open in the same in-app reader.
 */
data class GuidedPath(
    val title: String,
    val goal: String,
    val steps: List<LearnResource>
)

val GuidedPaths: List<GuidedPath> = listOf(
    GuidedPath(
        "Start thinking like a scientist",
        "Go from curiosity to a testable question you can actually investigate.",
        listOf(
            LearnResource("Understanding Science (UC Berkeley)", "Article", "https://undsci.berkeley.edu/", "How science really works — start here for the big picture."),
            LearnResource("The scientific method", "Article", "https://en.wikipedia.org/wiki/Scientific_method", "Hypotheses, predictions, and tests in one place."),
            LearnResource("List of cognitive biases", "Article", "https://en.wikipedia.org/wiki/List_of_cognitive_biases", "Learn the traps that distort your own observations.")
        )
    ),
    GuidedPath(
        "Keep field notes that last",
        "Record observations so they're still useful and trustworthy years later.",
        listOf(
            LearnResource("How to keep a field notebook (Cornell Lab)", "Article", "https://www.birds.cornell.edu/home/keeping-a-field-notebook/", "A practical, proven field-notebook method."),
            LearnResource("Grinnell method of field notes", "Article", "https://en.wikipedia.org/wiki/Joseph_Grinnell#Field_notes", "The rigorous system naturalists still rely on."),
            LearnResource("iNaturalist", "Tool", "https://www.inaturalist.org/", "See how others document and verify observations.")
        )
    ),
    GuidedPath(
        "Make sense of your data",
        "Turn raw counts and measurements into honest, readable conclusions.",
        listOf(
            LearnResource("Khan Academy: Statistics & probability", "Course", "https://www.khanacademy.org/math/statistics-probability", "Free lessons on describing and comparing data."),
            LearnResource("OpenIntro Statistics", "Book", "https://www.openintro.org/book/os/", "A free, complete statistics textbook with exercises."),
            LearnResource("Our World in Data", "Dataset", "https://ourworldindata.org/", "Worked examples of communicating data with charts.")
        )
    ),
    GuidedPath(
        "Write up what you found",
        "Structure findings into a clear, well-cited report others can trust.",
        listOf(
            LearnResource("Purdue OWL", "Article", "https://owl.purdue.edu/owl/research_and_citation/resources.html", "Authoritative guides on writing and citing."),
            LearnResource("On the Origin of Species (Darwin)", "Book", "https://www.gutenberg.org/ebooks/1228", "A masterclass in turning observation into argument.")
        )
    )
)
