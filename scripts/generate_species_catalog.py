#!/usr/bin/env python3
"""
FieldMind Species Catalog Generator
Expands the bundled species catalog to 500+ species by:
1. Fixing missing taxonomic data for existing species
2. Fetching real species from iNaturalist API
3. Generating additional species locally
4. Adding continent/region information
"""

import json, time, random, sys, os, re, urllib.request, urllib.error, urllib.parse

INAT_BASE = "https://api.inaturalist.org/v2"
ASSETS_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "species")
CATALOG_PATH = os.path.join(ASSETS_DIR, "species_catalog.json")

# ── Continent definitions with iNaturalist place_ids ──
CONTINENTS = {
    "North America": {"place_id": 1, "abbr": "na"},
    "South America": {"place_id": 26, "abbr": "sa"},
    "Europe": {"place_id": 4, "abbr": "eu"},
    "Africa": {"place_id": 29, "abbr": "af"},
    "Asia": {"place_id": 2, "abbr": "as"},
    "Oceania": {"place_id": 15, "abbr": "oc"},
    "Antarctica": {"place_id": 30, "abbr": "an"},
}

# ── Taxonomic corrections for existing species with wrong data ──
TAXONOMY_FIXES = {
    "Tufted Titmouse": {"order": "Passeriformes", "family": "Paridae"},
    "Eastern Gray Squirrel": {"order": "Rodentia", "family": "Sciuridae"},
    "Eastern Cottontail": {"order": "Lagomorpha", "family": "Leporidae"},
    "Eastern Chipmunk": {"order": "Rodentia", "family": "Sciuridae"},
    "Eastern Mole": {"order": "Eulipotyphla", "family": "Talpidae"},
    "Eastern Tiger Swallowtail": {"order": "Lepidoptera", "family": "Papilionidae"},
    "Firefly": {"order": "Coleoptera", "family": "Lampyridae"},
    "Japanese Beetle": {"order": "Coleoptera", "family": "Scarabaeidae"},
    "Eastern Tent Caterpillar": {"order": "Lepidoptera", "family": "Lasiocampidae"},
    "Eastern Hemlock": {"order": "Pinales", "family": "Pinaceae"},
    "Common Dandelion": {"order": "Asterales", "family": "Asteraceae"},
    "Sugar Maple": {"order": "Sapindales", "family": "Sapindaceae"},
    "Eastern White Pine": {"order": "Pinales", "family": "Pinaceae"},
    "Great Crested Flycatcher": {"order": "Passeriformes", "family": "Tyrannidae"},
    "Eastern Phoebe": {"order": "Passeriformes", "family": "Tyrannidae"},
    "Eastern Red-backed Salamander": {"order": "Caudata", "family": "Plethodontidae"},
    "Eastern Garter Snake": {"order": "Squamata", "family": "Colubridae"},
    "Eastern Box Turtle": {"order": "Testudines", "family": "Emydidae"},
    "European Starling": {"order": "Passeriformes", "family": "Sturnidae"},
    "Mallard": {"order": "Anseriformes", "family": "Anatidae"},
    "Black-capped Chickadee": {"order": "Passeriformes", "family": "Paridae"},
    "White-breasted Nuthatch": {"order": "Passeriformes", "family": "Sittidae"},
    "Dark-eyed Junco": {"order": "Passeriformes", "family": "Passerellidae"},
    "Northern Mockingbird": {"order": "Passeriformes", "family": "Mimidae"},
    "Red-winged Blackbird": {"order": "Passeriformes", "family": "Icteridae"},
    "Common Grackle": {"order": "Passeriformes", "family": "Icteridae"},
    "Barn Swallow": {"order": "Passeriformes", "family": "Hirundinidae"},
    "American Kestrel": {"order": "Falconiformes", "family": "Falconidae"},
    "Northern Flicker": {"order": "Piciformes", "family": "Picidae"},
    "Osprey": {"order": "Accipitriformes", "family": "Pandionidae"},
    "Yellow-bellied Sapsucker": {"order": "Piciformes", "family": "Picidae"},
    "Peregrine Falcon": {"order": "Falconiformes", "family": "Falconidae"},
    "Belted Kingfisher": {"order": "Coraciiformes", "family": "Alcedinidae"},
    "Mourning Dove": {"order": "Columbiformes", "family": "Columbidae"},
    "Raccoon": {"order": "Carnivora", "family": "Procyonidae"},
    "Striped Skunk": {"order": "Carnivora", "family": "Mephitidae"},
    "River Otter": {"order": "Carnivora", "family": "Mustelidae"},
    "Virginia Opossum": {"order": "Didelphimorphia", "family": "Didelphidae"},
    "Beaver": {"order": "Rodentia", "family": "Castoridae"},
    "Muskrat": {"order": "Rodentia", "family": "Cricetidae"},
    "White-footed Mouse": {"order": "Rodentia", "family": "Cricetidae"},
    "Meadow Vole": {"order": "Rodentia", "family": "Cricetidae"},
    "Spotted Salamander": {"order": "Caudata", "family": "Ambystomatidae"},
    "Five-lined Skink": {"order": "Squamata", "family": "Scincidae"},
    "Common Snapping Turtle": {"order": "Testudines", "family": "Chelydridae"},
    "Timber Rattlesnake": {"order": "Squamata", "family": "Viperidae"},
    "Largemouth Bass": {"order": "Perciformes", "family": "Centrarchidae"},
    "Bluegill": {"order": "Perciformes", "family": "Centrarchidae"},
    "Brook Trout": {"order": "Salmoniformes", "family": "Salmonidae"},
    "Atlantic Salmon": {"order": "Salmoniformes", "family": "Salmonidae"},
    "Garden Snail": {"order": "Stylommatophora", "family": "Helicidae"},
    "Eastern Oyster": {"order": "Ostreida", "family": "Ostreidae"},
    "Fly Agaric": {"order": "Agaricales", "family": "Amanitaceae"},
    "Turkey Tail": {"order": "Polyporales", "family": "Polyporaceae"},
    "Shaggy Mane": {"order": "Agaricales", "family": "Psathyrellaceae"},
    "Chicken of the Woods": {"order": "Polyporales", "family": "Laetiporaceae"},
    "Morel": {"order": "Pezizales", "family": "Morchellaceae"},
    "Artist's Conk": {"order": "Polyporales", "family": "Ganodermataceae"},
    "Painted Turtle": {"order": "Testudines", "family": "Emydidae"},
    "Mourning Dove": {"order": "Columbiformes", "family": "Columbidae"},
}

# ── Known continent distributions for common species (fallback) ──
KNOWN_DISTRIBUTIONS = {
    "American Robin": ["North America"],
    "Northern Cardinal": ["North America"],
    "Blue Jay": ["North America"],
    "American Crow": ["North America"],
    "Black-capped Chickadee": ["North America"],
    "Mourning Dove": ["North America", "Central America"],
    "Red-tailed Hawk": ["North America", "Central America"],
    "Bald Eagle": ["North America"],
    "American Goldfinch": ["North America"],
    "Downy Woodpecker": ["North America"],
    "House Sparrow": ["Europe", "Asia", "North America", "Africa", "Oceania"],
    "European Starling": ["Europe", "Asia", "North America", "Africa", "Oceania"],
    "Great Blue Heron": ["North America", "Central America", "South America"],
    "Canada Goose": ["North America", "Europe"],
    "Mallard": ["North America", "Europe", "Asia"],
    "Ruby-throated Hummingbird": ["North America", "Central America"],
    "Eastern Bluebird": ["North America"],
    "Tufted Titmouse": ["North America"],
    "White-breasted Nuthatch": ["North America"],
    "Northern Mockingbird": ["North America"],
    "Red-winged Blackbird": ["North America"],
    "Common Grackle": ["North America"],
    "Barn Swallow": ["Europe", "Asia", "Africa", "North America"],
    "Song Sparrow": ["North America"],
    "Dark-eyed Junco": ["North America"],
    "White-tailed Deer": ["North America", "Central America", "South America"],
    "Eastern Gray Squirrel": ["North America", "Europe"],
    "Eastern Cottontail": ["North America", "Central America"],
    "Red Fox": ["North America", "Europe", "Asia", "Africa"],
    "Raccoon": ["North America", "Europe", "Asia"],
    "American Black Bear": ["North America"],
    "Coyote": ["North America", "Central America"],
    "Virginia Opossum": ["North America"],
    "Striped Skunk": ["North America"],
    "Little Brown Bat": ["North America"],
    "Eastern Chipmunk": ["North America"],
    "Muskrat": ["North America"],
    "Beaver": ["North America", "Europe"],
    "River Otter": ["North America"],
    "Bobcat": ["North America"],
    "Eastern Mole": ["North America"],
    "White-footed Mouse": ["North America"],
    "Meadow Vole": ["North America"],
    "Gray Wolf": ["North America", "Europe", "Asia"],
    "Moose": ["North America", "Europe", "Asia"],
    "Monarch Butterfly": ["North America", "Central America", "South America"],
    "Eastern Tiger Swallowtail": ["North America"],
    "Honey Bee": ["Europe", "Asia", "Africa", "North America", "South America", "Oceania"],
    "Bumble Bee": ["North America", "Europe", "Asia"],
    "Seven-spotted Ladybug": ["Europe", "Asia", "North America"],
    "Firefly": ["North America", "Europe", "Asia"],
    "Dragonfly": ["North America", "Europe", "Asia", "Africa", "Oceania"],
    "Cicada": ["North America", "Europe", "Asia", "Africa"],
    "Praying Mantis": ["Europe", "Asia", "Africa", "North America"],
    "Eastern Tent Caterpillar": ["North America"],
    "Japanese Beetle": ["North America", "Asia"],
    "Luna Moth": ["North America"],
    "Red Maple": ["North America"],
    "Sugar Maple": ["North America"],
    "White Oak": ["North America"],
    "Eastern White Pine": ["North America"],
    "Black-eyed Susan": ["North America"],
    "Purple Coneflower": ["North America"],
    "Common Dandelion": ["Europe", "Asia", "North America"],
    "Queen Anne's Lace": ["Europe", "Asia", "North America"],
    "Milkweed": ["North America"],
    "Goldenrod": ["North America"],
    "Trillium": ["North America", "Asia"],
    "Poison Ivy": ["North America"],
    "Eastern Hemlock": ["North America"],
    "American Elm": ["North America"],
    "Fly Agaric": ["Europe", "Asia", "North America"],
    "Turkey Tail": ["North America", "Europe", "Asia"],
    "Shaggy Mane": ["North America", "Europe", "Asia"],
    "Chicken of the Woods": ["North America", "Europe", "Asia"],
    "Morel": ["North America", "Europe", "Asia"],
    "Artist's Conk": ["North America", "Europe", "Asia"],
    "American Bullfrog": ["North America"],
    "Gray Tree Frog": ["North America"],
    "Eastern Red-backed Salamander": ["North America"],
    "Spotted Salamander": ["North America"],
    "Eastern Garter Snake": ["North America"],
    "Eastern Box Turtle": ["North America"],
    "Painted Turtle": ["North America"],
    "Five-lined Skink": ["North America"],
    "Common Snapping Turtle": ["North America"],
    "Timber Rattlesnake": ["North America"],
    "Largemouth Bass": ["North America"],
    "Bluegill": ["North America"],
    "Brook Trout": ["North America"],
    "Atlantic Salmon": ["North America", "Europe"],
    "Garden Snail": ["Europe", "Asia", "Africa", "North America"],
    "Eastern Oyster": ["North America"],
    "Peregrine Falcon": ["North America", "Europe", "Asia", "Africa", "Oceania", "South America"],
    "American Kestrel": ["North America", "Central America", "South America"],
    "Great Horned Owl": ["North America"],
    "Barred Owl": ["North America"],
    "Eastern Screech Owl": ["North America"],
    "Snowy Owl": ["North America", "Europe", "Asia"],
    "Osprey": ["North America", "Europe", "Asia", "Africa", "Oceania"],
    "Cooper's Hawk": ["North America"],
    "Sharp-shinned Hawk": ["North America", "Central America", "South America"],
    "Broad-winged Hawk": ["North America", "Central America", "South America"],
    "Turkey Vulture": ["North America", "Central America", "South America"],
    "Black Vulture": ["North America", "Central America", "South America"],
    "Pileated Woodpecker": ["North America"],
    "Red-bellied Woodpecker": ["North America"],
    "Northern Flicker": ["North America"],
    "Hairy Woodpecker": ["North America"],
    "Yellow-bellied Sapsucker": ["North America"],
    "Eastern Phoebe": ["North America"],
    "Great Crested Flycatcher": ["North America"],
    "Eastern Wood Pewee": ["North America"],
    "Belted Kingfisher": ["North America", "Central America", "South America"],
}


def fetch_inat(url, retries=3):
    """Fetch data from iNaturalist API with rate limiting."""
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "FieldMind/1.0"})
            with urllib.request.urlopen(req, timeout=15) as resp:
                return json.loads(resp.read().decode())
        except (urllib.error.HTTPError, urllib.error.URLError, ConnectionError) as e:
            if attempt < retries - 1:
                time.sleep(2 ** attempt)
            else:
                print(f"  [WARN] Failed to fetch {url}: {e}", file=sys.stderr)
                return None


def fetch_species_batch(category, place_id=None, page=1, per_page=50):
    """Fetch species taxa from iNaturalist for a given category/region."""
    params = {
        "taxon_id": TAXON_IDS.get(category, 1),
        "rank": "species",
        "page": page,
        "per_page": per_page,
        "order": "desc",
        "order_by": "observations_count",
    }
    if place_id:
        params["place_id"] = place_id
    url = f"{INAT_BASE}/taxa?" + urllib.parse.urlencode(params)
    data = fetch_inat(url)
    if not data or "results" not in data:
        return []
    return data["results"]


TAXON_IDS = {
    "Bird": 3,
    "Mammal": 40151,
    "Insect": 47158,
    "Plant": 47126,
    "Fungi": 47170,
    "Amphibian": 20978,
    "Reptile": 26036,
    "Fish": 47178,
    "Mollusk": 47115,
    "Arachnid": 47119,
    "Crustacean": 47129,
}


def parse_inat_taxon(t):
    """Convert iNaturalist taxon result to our SpeciesRecord format."""
    name = t.get("name", "")
    common = t.get("preferred_common_name") or ""
    if not common or not name:
        return None

    category = guess_category_from_taxon(t)
    ancestor_ids = t.get("ancestor_ids", [])

    return {
        "id": "inat_" + name.lower().replace(" ", "_"),
        "common_name": common,
        "scientific_name": name,
        "category": category,
        "description": (t.get("wikipedia_summary") or "")[:300],
        "habitat": "",
        "diet": "",
        "conservation_status": extract_conservation(t),
        "tags": build_tags(common, category),
        "key_features": [],
        "similar_species": [],
        "kingdom": extract_rank(t, "kingdom", ancestor_ids) or "Animalia",
        "phylum": extract_rank(t, "phylum", ancestor_ids) or "",
        "order": extract_rank(t, "order", ancestor_ids) or "",
        "family": extract_rank(t, "family", ancestor_ids) or "",
        "genus": extract_rank(t, "genus", ancestor_ids) or name.split(" ")[0] if " " in name else "",
        "continents": [],
    }


def extract_rank(taxon, rank, ancestor_ids):
    """Extract a specific rank name from taxon ancestors."""
    ancestors = taxon.get("ancestors", [])
    for a in ancestors:
        if a.get("rank") == rank:
            return a.get("name", "")
    return ""


def extract_conservation(t):
    """Extract conservation status from iNaturalist taxon."""
    cs = t.get("conservation_status")
    if cs:
        return cs.get("status_name") or cs.get("status") or ""
    return ""


def guess_category_from_taxon(t):
    """Map iNaturalist taxon to our category."""
    ancestors = t.get("ancestors", [])
    ranks = {a.get("rank", ""): a.get("name", "") for a in ancestors}
    phylum = ranks.get("phylum", "")
    class_name = ranks.get("class", "")
    order = ranks.get("order", "")

    if class_name in ("Aves",): return "Bird"
    if class_name in ("Mammalia",): return "Mammal"
    if class_name in ("Insecta",): return "Insect"
    if class_name in ("Arachnida",): return "Arachnid"
    if class_name in ("Amphibia",): return "Amphibian"
    if class_name in ("Reptilia",): return "Reptile"
    if phylum in ("Chordata",) and class_name in ("Actinopterygii", "Chondrichthyes", "Myxini", "Cephalaspidomorphi"): return "Fish"
    if phylum in ("Mollusca",): return "Mollusk"
    if phylum in ("Arthropoda",) and class_name in ("Malacostraca", "Branchiopoda", "Maxillopoda", "Ostracoda"): return "Crustacean"
    rank_name = ranks.get("kingdom", "")
    if rank_name in ("Plantae",): return "Plant"
    if rank_name in ("Fungi",): return "Fungi"
    return "Other"


def build_tags(common, category):
    tags = [category.lower()]
    keywords = {
        "raptor": ["hawk", "eagle", "falcon", "kite", "osprey", "vulture", "harrier", "owl"],
        "songbird": ["robin", "warbler", "finch", "sparrow", "thrush", "wren", "titmouse", "chickadee", "bluebird", "cardinal", "grosbeak", "bunting", "oriole", "tanager", "vireo", "gnatcatcher"],
        "waterfowl": ["duck", "goose", "swan", "grebe", "loon", "cormorant", "pelican"],
        "shorebird": ["sandpiper", "plover", "godwit", "curlew", "avocet", "stilt", "oystercatcher"],
        "woodpecker": ["woodpecker", "flicker", "sapsucker"],
        "migratory": ["migratory", "warbler", "swallow", "hummingbird"],
        "predator": ["fox", "wolf", "coyote", "bear", "bobcat", "lynx", "cougar", "panther", "raccoon"],
        "nocturnal": ["owl", "bat", "raccoon", "opossum", "skunk", "moth"],
        "venomous": ["rattlesnake", "copperhead", "cottonmouth", "coral snake", "scorpion", "black widow"],
        "pollinator": ["bee", "butterfly", "moth", "hummingbird", "beetle", "wasp"],
        "invasive": ["starling", "house sparrow", "kudzu", "japanese beetle", "zebra mussel", "purple loosestrife", "garlic mustard"],
        "endangered": ["piping plover", "whooping crane", "california condor", "red wolf", "black-footed ferret"],
        "forest": ["oak", "maple", "pine", "hemlock", "spruce", "fir", "cedar", "birch", "beech"],
        "aquatic": ["fish", "turtle", "snapping turtle", "frog", "salamander", "newt"],
        "social": ["wolf", "bee", "ant", "termite", "crow", "starling", "sparrow"],
    }
    for tag, words in keywords.items():
        for word in words:
            if word.lower() in common.lower():
                tags.append(tag)
                break
    return list(set(tags))


# ── Additional species to generate locally ──
LOCAL_SPECIES = [
    # === MORE BIRDS (30+) ===
    {"common_name": "Hermit Thrush", "scientific_name": "Catharus guttatus", "category": "Bird", "order": "Passeriformes", "family": "Turdidae", "genus": "Catharus", "description": "Small thrush with spotted breast and reddish tail, known for its ethereal song.", "habitat": "Forests, woodlands, mountainous regions", "diet": "Insectivore — beetles, ants, caterpillars, spiders; also berries", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Wood Thrush", "scientific_name": "Hylocichla mustelina", "category": "Bird", "order": "Passeriformes", "family": "Turdidae", "genus": "Hylocichla", "description": "Medium-sized thrush with spotted breast and distinctive flute-like song.", "habitat": "Deciduous and mixed forests", "diet": "Insectivore — insects, spiders, snails; fruits in fall", "continents": ["North America", "Central America"], "conservation_status": "Near Threatened"},
    {"common_name": "Swainson's Thrush", "scientific_name": "Catharus ustulatus", "category": "Bird", "order": "Passeriformes", "family": "Turdidae", "genus": "Catharus", "description": "Olive-brown thrush with buffy eye ring and spotted breast, long-distance migrant.", "habitat": "Boreal forests, woodlands", "diet": "Insectivore — insects, berries", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Cedar Waxwing", "scientific_name": "Bombycilla cedrorum", "category": "Bird", "order": "Passeriformes", "family": "Bombycillidae", "genus": "Bombycilla", "description": "Sleek brown bird with crest, black mask, and waxy red wing tips.", "habitat": "Woodlands, orchards, urban areas", "diet": "Frugivore — berries, fruits; also insects", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Yellow Warbler", "scientific_name": "Setophaga petechia", "category": "Bird", "order": "Passeriformes", "family": "Parulidae", "genus": "Setophaga", "description": "Bright yellow warbler with reddish streaks on breast (male).", "habitat": "Wetlands, riparian areas, gardens", "diet": "Insectivore — caterpillars, beetles, flies", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Common Yellowthroat", "scientific_name": "Geothlypis trichas", "category": "Bird", "order": "Passeriformes", "family": "Parulidae", "genus": "Geothlypis", "description": "Small warbler with black mask (male) and yellow throat, skulking behavior.", "habitat": "Marshes, wetlands, thickets", "diet": "Insectivore — insects, spiders", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "American Redstart", "scientific_name": "Setophaga ruticilla", "category": "Bird", "order": "Passeriformes", "family": "Parulidae", "genus": "Setophaga", "description": "Active black-orange warbler that fans its tail to flush insects.", "habitat": "Deciduous forests, woodlands", "diet": "Insectivore — flies, moths, caterpillars", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Black-throated Blue Warbler", "scientific_name": "Setophaga caerulescens", "category": "Bird", "order": "Passeriformes", "family": "Parulidae", "genus": "Setophaga", "description": "Striking black-and-blue warbler with white belly.", "habitat": "Deciduous and mixed forests", "diet": "Insectivore — caterpillars, beetles, flies", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Indigo Bunting", "scientific_name": "Passerina cyanea", "category": "Bird", "order": "Passeriformes", "family": "Cardinalidae", "genus": "Passerina", "description": "Small finch with brilliant blue plumage (male).", "habitat": "Brushy areas, woodland edges, overgrown fields", "diet": "Granivore — seeds, insects", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Rose-breasted Grosbeak", "scientific_name": "Pheucticus ludovicianus", "category": "Bird", "order": "Passeriformes", "family": "Cardinalidae", "genus": "Pheucticus", "description": "Black-and-white bird with triangular rose-red breast patch.", "habitat": "Deciduous and mixed forests", "diet": "Insectivore — insects, seeds, fruits", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Baltimore Oriole", "scientific_name": "Icterus galbula", "category": "Bird", "order": "Passeriformes", "family": "Icteridae", "genus": "Icterus", "description": "Bright orange-and-black bird with hanging pouch nest.", "habitat": "Deciduous forests, parks, orchards", "diet": "Insectivore — caterpillars, fruits, nectar", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Common Loon", "scientific_name": "Gavia immer", "category": "Bird", "order": "Gaviiformes", "family": "Gaviidae", "genus": "Gavia", "description": "Large black-and-white waterbird with eerie yodeling call.", "habitat": "Northern lakes, coastal waters", "diet": "Piscivore — fish", "continents": ["North America", "Europe"], "conservation_status": "Least Concern"},
    {"common_name": "Sandhill Crane", "scientific_name": "Antigone canadensis", "category": "Bird", "order": "Gruiformes", "family": "Gruidae", "genus": "Antigone", "description": "Tall gray crane with red forehead, known for spectacular migrations.", "habitat": "Wetlands, grasslands, agricultural fields", "diet": "Omnivore — grains, insects, small vertebrates", "continents": ["North America", "Asia"], "conservation_status": "Least Concern"},
    {"common_name": "American Woodcock", "scientific_name": "Scolopax minor", "category": "Bird", "order": "Charadriiformes", "family": "Scolopacidae", "genus": "Scolopax", "description": "Stocky shorebird with long bill, performs spectacular sky dance display.", "habitat": "Moist woodlands, thickets", "diet": "Insectivore — earthworms, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Whip-poor-will", "scientific_name": "Antrostomus vociferus", "category": "Bird", "order": "Caprimulgiformes", "family": "Caprimulgidae", "genus": "Antrostomus", "description": "Nocturnal bird with camouflaged plumage and repetitive whistled call.", "habitat": "Deciduous and mixed forests", "diet": "Insectivore — moths, beetles, flying insects", "continents": ["North America", "Central America"], "conservation_status": "Near Threatened"},
    {"common_name": "Common Nighthawk", "scientific_name": "Chordeiles minor", "category": "Bird", "order": "Caprimulgiformes", "family": "Caprimulgidae", "genus": "Chordeiles", "description": "Nocturnal bird with white wing bars, booms during aerial dives.", "habitat": "Open woodlands, grasslands, urban areas", "diet": "Insectivore — flying insects", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Chimney Swift", "scientific_name": "Chaetura pelagica", "category": "Bird", "order": "Apodiformes", "family": "Apodidae", "genus": "Chaetura", "description": "Small sooty-gray bird with cigar-shaped body, constantly flying.", "habitat": "Urban areas, forests", "diet": "Insectivore — flying insects", "continents": ["North America", "Central America", "South America"], "conservation_status": "Vulnerable"},
    {"common_name": "Ruby-crowned Kinglet", "scientific_name": "Corthylio calendula", "category": "Bird", "order": "Passeriformes", "family": "Regulidae", "genus": "Corthylio", "description": "Tiny olive-gray bird with concealed red crown patch, hyperactive forager.", "habitat": "Coniferous and mixed forests", "diet": "Insectivore — insects, spiders", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Golden-crowned Kinglet", "scientific_name": "Regulus satrapa", "category": "Bird", "order": "Passeriformes", "family": "Regulidae", "genus": "Regulus", "description": "Tiny bird with bright yellow-orange crown stripe.", "habitat": "Coniferous forests", "diet": "Insectivore — insects, spiders", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Brown Creeper", "scientific_name": "Certhia americana", "category": "Bird", "order": "Passeriformes", "family": "Certhiidae", "genus": "Certhia", "description": "Small brown bird that spirals up tree trunks searching for insects.", "habitat": "Mature forests with large trees", "diet": "Insectivore — insects, spiders", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Winter Wren", "scientific_name": "Troglodytes hiemalis", "category": "Bird", "order": "Passeriformes", "family": "Troglodytidae", "genus": "Troglodytes", "description": "Tiny brown bird with short tail, remarkably loud bubbling song.", "habitat": "Forest understory, wooded ravines", "diet": "Insectivore — insects, spiders", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Carolina Wren", "scientific_name": "Thryothorus ludovicianus", "category": "Bird", "order": "Passeriformes", "family": "Troglodytidae", "genus": "Thryothorus", "description": "Rusty-brown wren with white eyebrow stripe and loud tea-kettle song.", "habitat": "Woodlands, thickets, suburban areas", "diet": "Insectivore — insects, spiders", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "House Wren", "scientific_name": "Troglodytes aedon", "category": "Bird", "order": "Passeriformes", "family": "Troglodytidae", "genus": "Troglodytes", "description": "Small brown wren with barred tail and bubbly song, nests in cavities.", "habitat": "Open woodlands, gardens, farms", "diet": "Insectivore — insects, spiders, caterpillars", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Savannah Sparrow", "scientific_name": "Passerculus sandwichensis", "category": "Bird", "order": "Passeriformes", "family": "Passerellidae", "genus": "Passerculus", "description": "Streaky brown sparrow with yellow eyebrow and notched tail.", "habitat": "Grasslands, agricultural fields, marshes", "diet": "Granivore — seeds, insects", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Chipping Sparrow", "scientific_name": "Spizella passerina", "category": "Bird", "order": "Passeriformes", "family": "Passerellidae", "genus": "Spizella", "description": "Small gray-breasted sparrow with rufous cap and black eye-line.", "habitat": "Forest edges, parks, gardens", "diet": "Granivore — seeds, insects", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Field Sparrow", "scientific_name": "Spizella pusilla", "category": "Bird", "order": "Passeriformes", "family": "Passerellidae", "genus": "Spizella", "description": "Pink-billed sparrow with plain gray breast and bouncy song.", "habitat": "Old fields, grasslands, brushy areas", "diet": "Granivore — seeds, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "White-throated Sparrow", "scientific_name": "Zonotrichia albicollis", "category": "Bird", "order": "Passeriformes", "family": "Passerellidae", "genus": "Zonotrichia", "description": "Large sparrow with white throat patch and yellow lores, whistled song.", "habitat": "Forests, woodland edges, brushy areas", "diet": "Granivore — seeds, fruits, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Fox Sparrow", "scientific_name": "Passerella iliaca", "category": "Bird", "order": "Passeriformes", "family": "Passerellidae", "genus": "Passerella", "description": "Large heavily-streaked sparrow with rufous coloring, scratches vigorously for food.", "habitat": "Thickets, brushy areas, forests", "diet": "Granivore — seeds, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Lincoln's Sparrow", "scientific_name": "Melospiza lincolnii", "category": "Bird", "order": "Passeriformes", "family": "Passerellidae", "genus": "Melospiza", "description": "Finely streaked sparrow with buffy breast band, secretive nature.", "habitat": "Bogs, wet meadows, shrubby areas", "diet": "Granivore — seeds, insects", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Swamp Sparrow", "scientific_name": "Melospiza georgiana", "category": "Bird", "order": "Passeriformes", "family": "Passerellidae", "genus": "Melospiza", "description": "Rusty-winged sparrow with gray breast and sweet trilling song.", "habitat": "Marshes, wet meadows, bogs", "diet": "Granivore — seeds, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Brown-headed Cowbird", "scientific_name": "Molothrus ater", "category": "Bird", "order": "Passeriformes", "family": "Icteridae", "genus": "Molothrus", "description": "Blackbird with brown head (male), brood parasite that lays eggs in other birds' nests.", "habitat": "Open woodlands, fields, urban areas", "diet": "Granivore — seeds, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Orchard Oriole", "scientific_name": "Icterus spurius", "category": "Bird", "order": "Passeriformes", "family": "Icteridae", "genus": "Icterus", "description": "Small oriole with chestnut-and-black plumage (male), weaves hanging nest.", "habitat": "Open woodlands, orchards, parks", "diet": "Insectivore — insects, fruits, nectar", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Scarlet Tanager", "scientific_name": "Piranga olivacea", "category": "Bird", "order": "Passeriformes", "family": "Cardinalidae", "genus": "Piranga", "description": "Brilliant scarlet bird with black wings and tail (male), moss-green female.", "habitat": "Deciduous forests", "diet": "Insectivore — insects, fruits", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Summer Tanager", "scientific_name": "Piranga rubra", "category": "Bird", "order": "Passeriformes", "family": "Cardinalidae", "genus": "Piranga", "description": "All-red tanager (male), the only entirely red bird in North America.", "habitat": "Deciduous and pine forests", "diet": "Insectivore — bees, wasps, insects, fruits", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Blue-gray Gnatcatcher", "scientific_name": "Polioptila caerulea", "category": "Bird", "order": "Passeriformes", "family": "Polioptilidae", "genus": "Polioptila", "description": "Tiny blue-gray bird with long white-edged tail, constantly flitting.", "habitat": "Deciduous forests, scrub, gardens", "diet": "Insectivore — small insects, spiders", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Gray Catbird", "scientific_name": "Dumetella carolinensis", "category": "Bird", "order": "Passeriformes", "family": "Mimidae", "genus": "Dumetella", "description": "Slate-gray bird with black cap and rusty undertail, cat-like mewing call.", "habitat": "Thickets, brushy areas, gardens", "diet": "Insectivore — insects, berries", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Brown Thrasher", "scientific_name": "Toxostoma rufum", "category": "Bird", "order": "Passeriformes", "family": "Mimidae", "genus": "Toxostoma", "description": "Rusty-brown bird with heavily streaked breast and long curved bill.", "habitat": "Thickets, brushy areas, forest edges", "diet": "Insectivore — insects, fruits, seeds", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Loggerhead Shrike", "scientific_name": "Lanius ludovicianus", "category": "Bird", "order": "Passeriformes", "family": "Laniidae", "genus": "Lanius", "description": "Predatory songbird with hooked bill, impales prey on thorns.", "habitat": "Open grasslands, scrub, farmlands", "diet": "Carnivore — insects, small mammals, birds, lizards", "continents": ["North America"], "conservation_status": "Near Threatened"},

    # === MORE MAMMALS (20+) ===
    {"common_name": "Snowshoe Hare", "scientific_name": "Lepus americanus", "category": "Mammal", "order": "Lagomorpha", "family": "Leporidae", "genus": "Lepus", "description": "Large-footed hare that turns white in winter for camouflage.", "habitat": "Boreal forests, coniferous woodlands", "diet": "Herbivore — twigs, bark, buds, leaves", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Arctic Fox", "scientific_name": "Vulpes lagopus", "category": "Mammal", "order": "Carnivora", "family": "Canidae", "genus": "Vulpes", "description": "Small fox with thick white winter coat, adapted to extreme cold.", "habitat": "Arctic tundra", "diet": "Carnivore — lemmings, voles, birds, fish, carrion", "continents": ["North America", "Europe", "Asia"], "conservation_status": "Least Concern"},
    {"common_name": "Gray Fox", "scientific_name": "Urocyon cinereoargenteus", "category": "Mammal", "order": "Carnivora", "family": "Canidae", "genus": "Urocyon", "description": "Medium-sized canid with gray fur, black-tipped tail, climbs trees.", "habitat": "Deciduous forests, woodlands, rocky areas", "diet": "Omnivore — small mammals, birds, insects, fruits", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "North American Porcupine", "scientific_name": "Erethizon dorsatum", "category": "Mammal", "order": "Rodentia", "family": "Erethizontidae", "genus": "Erethizon", "description": "Large rodent covered in sharp quills for defense.", "habitat": "Forests, woodlands, tundra", "diet": "Herbivore — tree bark, twigs, leaves, evergreens", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Woodchuck", "scientific_name": "Marmota monax", "category": "Mammal", "order": "Rodentia", "family": "Sciuridae", "genus": "Marmota", "description": "Stocky ground squirrel, also known as groundhog, hibernates in winter.", "habitat": "Open fields, woodland edges, meadows", "diet": "Herbivore — grasses, clover, garden vegetables", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Red Squirrel", "scientific_name": "Tamiasciurus hudsonicus", "category": "Mammal", "order": "Rodentia", "family": "Sciuridae", "genus": "Tamiasciurus", "description": "Small russet-red squirrel with white belly and chattering calls.", "habitat": "Coniferous and mixed forests", "diet": "Herbivore — pine cones, seeds, nuts, berries", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Northern Flying Squirrel", "scientific_name": "Glaucomys sabrinus", "category": "Mammal", "order": "Rodentia", "family": "Sciuridae", "genus": "Glaucomys", "description": "Nocturnal squirrel with furry gliding membrane between wrists and ankles.", "habitat": "Mature coniferous and mixed forests", "diet": "Omnivore — nuts, seeds, fungi, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Fox Squirrel", "scientific_name": "Sciurus niger", "category": "Mammal", "order": "Rodentia", "family": "Sciuridae", "genus": "Sciurus", "description": "Large tree squirrel with variable coloration, from gray to rusty brown.", "habitat": "Open woodlands, pine forests, urban areas", "diet": "Herbivore — nuts, seeds, fruits, buds", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "American Mink", "scientific_name": "Neogale vison", "category": "Mammal", "order": "Carnivora", "family": "Mustelidae", "genus": "Neogale", "description": "Semi-aquatic mustelid with glossy dark fur and white chin.", "habitat": "Wetlands, rivers, lakes, coastal areas", "diet": "Carnivore — fish, frogs, crayfish, birds, small mammals", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Fisher", "scientific_name": "Pekania pennanti", "category": "Mammal", "order": "Carnivora", "family": "Mustelidae", "genus": "Pekania", "description": "Large dark mustelid, one of few predators of porcupines.", "habitat": "Mature coniferous and mixed forests", "diet": "Carnivore — porcupines, hares, rodents, birds", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Long-tailed Weasel", "scientific_name": "Neogale frenata", "category": "Mammal", "order": "Carnivora", "family": "Mustelidae", "genus": "Neogale", "description": "Slender brown carnivore with black-tipped tail, turns white in winter at northern range.", "habitat": "Woodlands, grasslands, farmlands", "diet": "Carnivore — mice, voles, rabbits, birds", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Spotted Skunk", "scientific_name": "Spilogale putorius", "category": "Mammal", "order": "Carnivora", "family": "Mephitidae", "genus": "Spilogale", "description": "Small black skunk with white spots and stripes, does handstand before spraying.", "habitat": "Woodlands, brushy areas, rocky outcrops", "diet": "Omnivore — insects, small mammals, fruits, eggs", "continents": ["North America"], "conservation_status": "Vulnerable"},
    {"common_name": "Nine-banded Armadillo", "scientific_name": "Dasypus novemcinctus", "category": "Mammal", "order": "Cingulata", "family": "Dasypodidae", "genus": "Dasypus", "description": "Armored mammal with bony shell plates, digs for insects.", "habitat": "Forests, grasslands, scrublands", "diet": "Insectivore — ants, beetles, grubs, termites", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Cougar", "scientific_name": "Puma concolor", "category": "Mammal", "order": "Carnivora", "family": "Felidae", "genus": "Puma", "description": "Large tawny cat, also known as mountain lion or puma.", "habitat": "Mountains, forests, deserts, grasslands", "diet": "Carnivore — deer, elk, raccoons, small mammals", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Canada Lynx", "scientific_name": "Lynx canadensis", "category": "Mammal", "order": "Carnivora", "family": "Felidae", "genus": "Lynx", "description": "Medium-sized wildcat with oversized paws for walking on snow.", "habitat": "Boreal and mixed forests", "diet": "Carnivore — snowshoe hares", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Caribou", "scientific_name": "Rangifer tarandus", "category": "Mammal", "order": "Artiodactyla", "family": "Cervidae", "genus": "Rangifer", "description": "Large deer with antlers on both sexes, long-distance Arctic migrants.", "habitat": "Arctic tundra, boreal forests", "diet": "Herbivore — lichens, mosses, grasses, leaves", "continents": ["North America", "Europe", "Asia"], "conservation_status": "Vulnerable"},
    {"common_name": "Bison", "scientific_name": "Bison bison", "category": "Mammal", "order": "Artiodactyla", "family": "Bovidae", "genus": "Bison", "description": "Massive humped bovine, the largest land mammal in North America.", "habitat": "Grasslands, prairies, plains", "diet": "Herbivore — grasses, sedges", "continents": ["North America"], "conservation_status": "Near Threatened"},
    {"common_name": "Pronghorn", "scientific_name": "Antilocapra americana", "category": "Mammal", "order": "Artiodactyla", "family": "Antilocapridae", "genus": "Antilocapra", "description": "Fleet-footed ungulate, the fastest land mammal in North America.", "habitat": "Grasslands, sagebrush plains", "diet": "Herbivore — grasses, sagebrush, forbs", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Big Brown Bat", "scientific_name": "Eptesicus fuscus", "category": "Mammal", "order": "Chiroptera", "family": "Vespertilionidae", "genus": "Eptesicus", "description": "Large reddish-brown bat, common in urban and rural areas.", "habitat": "Caves, buildings, hollow trees", "diet": "Insectivore — beetles, moths, flying insects", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Red Bat", "scientific_name": "Lasiurus borealis", "category": "Mammal", "order": "Chiroptera", "family": "Vespertilionidae", "genus": "Lasiurus", "description": "Vibrant reddish-orange bat with white-tipped fur.", "habitat": "Forests, roosts in tree foliage", "diet": "Insectivore — moths, beetles, flies", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},

    # === MORE REPTILES (15+) ===
    {"common_name": "Common Garter Snake", "scientific_name": "Thamnophis sirtalis", "category": "Reptile", "order": "Squamata", "family": "Colubridae", "genus": "Thamnophis", "description": "Widespread striped snake, highly variable in color and pattern.", "habitat": "Wetlands, meadows, forests, gardens", "diet": "Carnivore — earthworms, amphibians, fish", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Northern Water Snake", "scientific_name": "Nerodia sipedon", "category": "Reptile", "order": "Squamata", "family": "Colubridae", "genus": "Nerodia", "description": "Thick-bodied non-venomous snake, often mistaken for cottonmouth.", "habitat": "Lakes, rivers, ponds, marshes", "diet": "Carnivore — fish, amphibians", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Milk Snake", "scientific_name": "Lampropeltis triangulum", "category": "Reptile", "order": "Squamata", "family": "Colubridae", "genus": "Lampropeltis", "description": "Colorful constrictor with red-black-yellow banding mimicking coral snake.", "habitat": "Forests, grasslands, farmlands", "diet": "Carnivore — rodents, lizards, other snakes", "continents": ["North America", "Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "Black Racer", "scientific_name": "Coluber constrictor", "category": "Reptile", "order": "Squamata", "family": "Colubridae", "genus": "Coluber", "description": "Fast-moving sleek black snake with white chin, active daytime hunter.", "habitat": "Open fields, woodlands, scrublands", "diet": "Carnivore — insects, rodents, lizards, birds", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Rat Snake", "scientific_name": "Pantherophis alleghaniensis", "category": "Reptile", "order": "Squamata", "family": "Colubridae", "genus": "Pantherophis", "description": "Large black constrictor, excellent climber, helps control rodent populations.", "habitat": "Forests, farmlands, suburban areas", "diet": "Carnivore — rodents, birds, eggs", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Northern Copperhead", "scientific_name": "Agkistrodon contortrix", "category": "Reptile", "order": "Squamata", "family": "Viperidae", "genus": "Agkistrodon", "description": "Venomous pit viper with copper-colored head and hourglass markings.", "habitat": "Deciduous forests, rocky hillsides", "diet": "Carnivore — mice, voles, insects, amphibians", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Diamondback Rattlesnake", "scientific_name": "Crotalus adamanteus", "category": "Reptile", "order": "Squamata", "family": "Viperidae", "genus": "Crotalus", "description": "Largest venomous snake in North America with diamond pattern.", "habitat": "Pine forests, sandhills, coastal scrub", "diet": "Carnivore — rabbits, rats, squirrels, birds", "continents": ["North America"], "conservation_status": "Vulnerable"},
    {"common_name": "Common Snapping Turtle", "scientific_name": "Chelydra serpentina", "category": "Reptile", "order": "Testudines", "family": "Chelydridae", "genus": "Chelydra", "description": "Large aquatic turtle with powerful beak-like jaws.", "habitat": "Lakes, rivers, ponds, marshes", "diet": "Omnivore — fish, amphibians, plants, carrion", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Wood Turtle", "scientific_name": "Glyptemys insculpta", "category": "Reptile", "order": "Testudines", "family": "Emydidae", "genus": "Glyptemys", "description": "Medium-sized turtle with sculpted shell and orange neck and legs.", "habitat": "Forests near streams and rivers", "diet": "Omnivore — insects, worms, berries, fungi", "continents": ["North America"], "conservation_status": "Endangered"},
    {"common_name": "Spotted Turtle", "scientific_name": "Clemmys guttata", "category": "Reptile", "order": "Testudines", "family": "Emydidae", "genus": "Clemmys", "description": "Small black turtle with distinct yellow spots on shell and skin.", "habitat": "Marshes, bogs, wet meadows", "diet": "Omnivore — insects, crayfish, plants", "continents": ["North America"], "conservation_status": "Endangered"},
    {"common_name": "Blanding's Turtle", "scientific_name": "Emydoidea blandingii", "category": "Reptile", "order": "Testudines", "family": "Emydidae", "genus": "Emydoidea", "description": "Medium-sized turtle with bright yellow chin and throat, domed shell.", "habitat": "Wetlands, marshes, ponds", "diet": "Omnivore — crayfish, insects, fish, plants", "continents": ["North America"], "conservation_status": "Endangered"},
    {"common_name": "Green Anole", "scientific_name": "Anolis carolinensis", "category": "Reptile", "order": "Squamata", "family": "Dactyloidae", "genus": "Anolis", "description": "Small green lizard that can change color, with pink throat fan (dewlap).", "habitat": "Forests, gardens, shrubs", "diet": "Insectivore — insects, spiders", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Fence Lizard", "scientific_name": "Sceloporus undulatus", "category": "Reptile", "order": "Squamata", "family": "Phrynosomatidae", "genus": "Sceloporus", "description": "Rough-scaled gray-brown lizard with blue patches on belly.", "habitat": "Forests, rocky areas, fence rows", "diet": "Insectivore — ants, beetles, grasshoppers, spiders", "continents": ["North America"], "conservation_status": "Least Concern"},

    # === MORE AMPHIBIANS (12+) ===
    {"common_name": "Spring Peeper", "scientific_name": "Pseudacris crucifer", "category": "Amphibian", "order": "Anura", "family": "Hylidae", "genus": "Pseudacris", "description": "Tiny chorus frog with X-shaped cross on back, loud piping call.", "habitat": "Woodlands, ponds, wetlands", "diet": "Insectivore — small insects, spiders", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Wood Frog", "scientific_name": "Lithobates sylvaticus", "category": "Amphibian", "order": "Anura", "family": "Ranidae", "genus": "Lithobates", "description": "Brown frog with dark raccoon-like mask, freezes solid in winter.", "habitat": "Forests, vernal pools", "diet": "Insectivore — beetles, spiders, slugs, worms", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Green Frog", "scientific_name": "Lithobates clamitans", "category": "Amphibian", "order": "Anura", "family": "Ranidae", "genus": "Lithobates", "description": "Medium green or brown frog with prominent eardrums.", "habitat": "Ponds, lakes, streams, wetlands", "diet": "Carnivore — insects, crayfish, small fish", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Northern Leopard Frog", "scientific_name": "Lithobates pipiens", "category": "Amphibian", "order": "Anura", "family": "Ranidae", "genus": "Lithobates", "description": "Green or brown frog with round dark spots, long-distance jumper.", "habitat": "Wetlands, meadows, ponds", "diet": "Insectivore — insects, worms, small snakes", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Pickerel Frog", "scientific_name": "Lithobates palustris", "category": "Amphibian", "order": "Anura", "family": "Ranidae", "genus": "Lithobates", "description": "Tan frog with square-shaped spots, toxic skin secretions.", "habitat": "Cold streams, springs, bogs", "diet": "Insectivore — insects, spiders", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "American Toad", "scientific_name": "Anaxyrus americanus", "category": "Amphibian", "order": "Anura", "family": "Bufonidae", "genus": "Anaxyrus", "description": "Warty brown toad with two large bumps (parotoid glands) behind eyes.", "habitat": "Woodlands, gardens, fields", "diet": "Insectivore — insects, earthworms, slugs", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Fowler's Toad", "scientific_name": "Anaxyrus fowleri", "category": "Amphibian", "order": "Anura", "family": "Bufonidae", "genus": "Anaxyrus", "description": "Gray to greenish toad with three or more warts per dark spot.", "habitat": "Sandy areas, woodlands, fields", "diet": "Insectivore — ants, beetles, insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Newt", "scientific_name": "Notophthalmus viridescens", "category": "Amphibian", "order": "Caudata", "family": "Salamandridae", "genus": "Notophthalmus", "description": "Small salamander with complex life cycle — aquatic larvae, terrestrial red eft stage, then aquatic adult.", "habitat": "Ponds, lakes, forests", "diet": "Carnivore — insects, worms, crustaceans", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Jefferson Salamander", "scientific_name": "Ambystoma jeffersonianum", "category": "Amphibian", "order": "Caudata", "family": "Ambystomatidae", "genus": "Ambystoma", "description": "Long slender gray-brown salamander with blue flecks.", "habitat": "Deciduous forests, vernal pools", "diet": "Carnivore — earthworms, insects, slugs", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Northern Dusky Salamander", "scientific_name": "Desmognathus fuscus", "category": "Amphibian", "order": "Caudata", "family": "Plethodontidae", "genus": "Desmognathus", "description": "Small brown salamander with pale diagonal line from eye to jaw.", "habitat": "Streams, seeps, wet rocks", "diet": "Insectivore — insects, worms, small crustaceans", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Mudpuppy", "scientific_name": "Necturus maculosus", "category": "Amphibian", "order": "Caudata", "family": "Proteidae", "genus": "Necturus", "description": "Large fully aquatic salamander with bushy red gills and four toes.", "habitat": "Lakes, rivers, streams", "diet": "Carnivore — crayfish, fish, insects, worms", "continents": ["North America"], "conservation_status": "Least Concern"},

    # === MORE INSECTS (15+) ===
    {"common_name": "Eastern Tiger Swallowtail", "scientific_name": "Papilio glaucus", "category": "Insect", "order": "Lepidoptera", "family": "Papilionidae", "genus": "Papilio", "description": "Large yellow swallowtail butterfly with black tiger stripes.", "habitat": "Deciduous forests, gardens, meadows", "diet": "Herbivore/Nectarivore — nectar (adult), leaves (caterpillar)", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Black Swallowtail", "scientific_name": "Papilio polyxenes", "category": "Insect", "order": "Lepidoptera", "family": "Papilionidae", "genus": "Papilio", "description": "Black butterfly with yellow spots and blue scaling on hindwing.", "habitat": "Open fields, gardens, meadows", "diet": "Herbivore/Nectarivore — nectar (adult), parsley/carrot leaves (caterpillar)", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Red Admiral", "scientific_name": "Vanessa atalanta", "category": "Insect", "order": "Lepidoptera", "family": "Nymphalidae", "genus": "Vanessa", "description": "Dark butterfly with bright red-orange bands and white spots.", "habitat": "Gardens, forests, urban areas", "diet": "Herbivore/Nectarivore — tree sap, rotting fruit, nectar", "continents": ["North America", "Europe", "Asia", "Africa"], "conservation_status": "Least Concern"},
    {"common_name": "Painted Lady", "scientific_name": "Vanessa cardui", "category": "Insect", "order": "Lepidoptera", "family": "Nymphalidae", "genus": "Vanessa", "description": "Orange-and-brown butterfly with black and white wing tips, most widespread butterfly.", "habitat": "Open fields, gardens, everywhere", "diet": "Herbivore/Nectarivore — nectar, thistle", "continents": ["North America", "Europe", "Asia", "Africa", "Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Cabbage White", "scientific_name": "Pieris rapae", "category": "Insect", "order": "Lepidoptera", "family": "Pieridae", "genus": "Pieris", "description": "Small white butterfly with black wing tips, common garden visitor.", "habitat": "Gardens, fields, urban areas", "diet": "Herbivore/Nectarivore — nectar, cabbage family plants", "continents": ["North America", "Europe", "Asia", "Africa", "Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Great Spangled Fritillary", "scientific_name": "Speyeria cybele", "category": "Insect", "order": "Lepidoptera", "family": "Nymphalidae", "genus": "Speyeria", "description": "Large orange butterfly with silver spots on hindwing underside.", "habitat": "Meadows, open woodlands, fields", "diet": "Herbivore/Nectarivore — nectar, violet leaves (caterpillar)", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Cecropia Moth", "scientific_name": "Hyalophora cecropia", "category": "Insect", "order": "Lepidoptera", "family": "Saturniidae", "genus": "Hyalophora", "description": "North America's largest moth, with striking reddish-brown and white pattern.", "habitat": "Deciduous forests, woodlands", "diet": "Herbivore — leaves of maple, birch, cherry trees", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Common Whitetail", "scientific_name": "Plathemis lydia", "category": "Insect", "order": "Odonata", "family": "Libellulidae", "genus": "Plathemis", "description": "Dragonfly with broad white or brown banded wings, common near water.", "habitat": "Ponds, lakes, slow streams", "diet": "Carnivore — mosquitoes, flies, small insects", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Pondhawk", "scientific_name": "Erythemis simplicicollis", "category": "Insect", "order": "Odonata", "family": "Libellulidae", "genus": "Erythemis", "description": "Green dragonfly with white abdomen (young) becoming pruinose blue (male).", "habitat": "Ponds, lakes, wetlands", "diet": "Carnivore — mosquitoes, flies, butterflies", "continents": ["North America", "Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Green Darner", "scientific_name": "Anax junius", "category": "Insect", "order": "Odonata", "family": "Aeshnidae", "genus": "Anax", "description": "Large green and blue dragonfly, strong migrant, eats mosquitoes.", "habitat": "Ponds, lakes, wetlands", "diet": "Carnivore — mosquitoes, flies, moths, bees", "continents": ["North America", "Central America", "South America", "Asia"], "conservation_status": "Least Concern"},
    {"common_name": "Dog-day Cicada", "scientific_name": "Neotibicen canicularis", "category": "Insect", "order": "Hemiptera", "family": "Cicadidae", "genus": "Neotibicen", "description": "Green and black cicada with loud droning summer call.", "habitat": "Deciduous forests, woodlands", "diet": "Herbivore — tree sap (nymphs), leaves (adult)", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Periodical Cicada", "scientific_name": "Magicicada septendecim", "category": "Insect", "order": "Hemiptera", "family": "Cicadidae", "genus": "Magicicada", "description": "Black cicada with red eyes, emerges in massive synchronized broods every 17 years.", "habitat": "Deciduous forests", "diet": "Herbivore — tree root sap (nymphs)", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "European Honey Bee", "scientific_name": "Apis mellifera", "category": "Insect", "order": "Hymenoptera", "family": "Apidae", "genus": "Apis", "description": "Social bee living in large colonies, vital pollinator of crops and wild plants.", "habitat": "Gardens, meadows, forests, agricultural areas", "diet": "Herbivore — nectar, pollen, honey", "continents": ["Europe", "Asia", "Africa", "North America", "South America", "Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Bald-faced Hornet", "scientific_name": "Dolichovespula maculata", "category": "Insect", "order": "Hymenoptera", "family": "Vespidae", "genus": "Dolichovespula", "description": "Black-and-white social wasp that builds large paper nests in trees.", "habitat": "Forests, woodlands, urban areas", "diet": "Carnivore — insects, caterpillars, nectar", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Yellow Jacket", "scientific_name": "Vespula maculifrons", "category": "Insect", "order": "Hymenoptera", "family": "Vespidae", "genus": "Vespula", "description": "Black-and-yellow social wasp, nests underground, aggressive near food.", "habitat": "Forests, gardens, urban areas", "diet": "Carnivore — insects, carrion, sweets", "continents": ["North America"], "conservation_status": "Least Concern"},

    # === MORE PLANTS (15+) ===
    {"common_name": "Northern Red Oak", "scientific_name": "Quercus rubra", "category": "Plant", "order": "Fagales", "family": "Fagaceae", "genus": "Quercus", "description": "Fast-growing deciduous oak with pointed-lobed leaves and reddish fall color.", "habitat": "Forests, woodlands", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Silver Maple", "scientific_name": "Acer saccharinum", "category": "Plant", "order": "Sapindales", "family": "Sapindaceae", "genus": "Acer", "description": "Fast-growing maple with deeply lobed leaves, silvery underside.", "habitat": "Floodplains, riverbanks, wetlands", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Paper Birch", "scientific_name": "Betula papyrifera", "category": "Plant", "order": "Fagales", "family": "Betulaceae", "genus": "Betula", "description": "Deciduous tree with peeling white bark marked with black.", "habitat": "Northern forests, cool climates", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Trembling Aspen", "scientific_name": "Populus tremuloides", "category": "Plant", "order": "Malpighiales", "family": "Salicaceae", "genus": "Populus", "description": "Deciduous tree with fluttering leaves and white bark, forms clonal colonies.", "habitat": "Forests, mountains, disturbed areas", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Eastern Redbud", "scientific_name": "Cercis canadensis", "category": "Plant", "order": "Fabales", "family": "Fabaceae", "genus": "Cercis", "description": "Small tree with rosy-pink flowers blooming before leaves in spring.", "habitat": "Forest edges, woodlands", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Flowering Dogwood", "scientific_name": "Cornus florida", "category": "Plant", "order": "Cornales", "family": "Cornaceae", "genus": "Cornus", "description": "Small tree with showy white or pink bracts in spring, red berries in fall.", "habitat": "Understory of deciduous forests", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Sassafras", "scientific_name": "Sassafras albidum", "category": "Plant", "order": "Laurales", "family": "Lauraceae", "genus": "Sassafras", "description": "Aromatic tree with mitten-shaped, three-lobed, and oval leaves.", "habitat": "Forest edges, old fields, woodlands", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Black Cherry", "scientific_name": "Prunus serotina", "category": "Plant", "order": "Rosales", "family": "Rosaceae", "genus": "Prunus", "description": "Tall deciduous tree with dark fruit, important for wildlife.", "habitat": "Forests, woodlands", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Sugar Maple", "scientific_name": "Acer saccharum", "category": "Plant", "order": "Sapindales", "family": "Sapindaceae", "genus": "Acer", "description": "Deciduous tree with brilliant orange-red fall color, source of maple syrup.", "habitat": "Deciduous forests", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "New England Aster", "scientific_name": "Symphyotrichum novae-angliae", "category": "Plant", "order": "Asterales", "family": "Asteraceae", "genus": "Symphyotrichum", "description": "Tall late-blooming perennial with vibrant purple daisy-like flowers.", "habitat": "Meadows, fields, roadsides", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Butterfly Weed", "scientific_name": "Asclepias tuberosa", "category": "Plant", "order": "Gentianales", "family": "Apocynaceae", "genus": "Asclepias", "description": "Bright orange milkweed, critical host plant for monarch butterflies.", "habitat": "Meadows, prairies, open fields", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Wild Bergamot", "scientific_name": "Monarda fistulosa", "category": "Plant", "order": "Lamiales", "family": "Lamiaceae", "genus": "Monarda", "description": "Aromatic perennial with lavender tubular flowers, attracts pollinators.", "habitat": "Prairies, meadows, open woodlands", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Joe-Pye Weed", "scientific_name": "Eutrochium maculatum", "category": "Plant", "order": "Asterales", "family": "Asteraceae", "genus": "Eutrochium", "description": "Tall perennial with spotted stem and large pink flower clusters.", "habitat": "Wet meadows, marshes, stream banks", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Cardinal Flower", "scientific_name": "Lobelia cardinalis", "category": "Plant", "order": "Asterales", "family": "Campanulaceae", "genus": "Lobelia", "description": "Striking perennial with brilliant red flower spikes, hummingbird favorite.", "habitat": "Stream banks, wetlands, moist woods", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Least Concern"},

    # === MORE FUNGI (8+) ===
    {"common_name": "Death Cap", "scientific_name": "Amanita phalloides", "category": "Fungi", "order": "Agaricales", "family": "Amanitaceae", "genus": "Amanita", "description": "Deadly poisonous mushroom with olive-green cap and white gills.", "habitat": "Woodlands, forests near oaks and pines", "diet": "Mycorrhizal", "continents": ["Europe", "Asia", "North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Destroying Angel", "scientific_name": "Amanita bisporigera", "category": "Fungi", "order": "Agaricales", "family": "Amanitaceae", "genus": "Amanita", "description": "Pure white deadly poisonous mushroom, among the most toxic.", "habitat": "Deciduous forests", "diet": "Mycorrhizal", "continents": ["North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Hen of the Woods", "scientific_name": "Grifola frondosa", "category": "Fungi", "order": "Polyporales", "family": "Meripilaceae", "genus": "Grifola", "description": "Edible polypore with clustered gray-brown caps, grows at tree base.", "habitat": "Base of oak and hardwood trees", "diet": "Parasitic/Saprotrophic", "continents": ["North America", "Europe", "Asia"], "conservation_status": "Not Evaluated"},
    {"common_name": "Lion's Mane", "scientific_name": "Hericium erinaceus", "category": "Fungi", "order": "Russulales", "family": "Hericiaceae", "genus": "Hericium", "description": "White tooth fungus with cascading spines, resembles a lion's mane.", "habitat": "Hardwood trees, logs, stumps", "diet": "Saprotrophic", "continents": ["North America", "Europe", "Asia"], "conservation_status": "Not Evaluated"},
    {"common_name": "Reishi", "scientific_name": "Ganoderma lingzhi", "category": "Fungi", "order": "Polyporales", "family": "Ganodermataceae", "genus": "Ganoderma", "description": "Reddish varnished shelf fungus, highly valued in traditional medicine.", "habitat": "Deciduous trees, logs", "diet": "Parasitic/Saprotrophic", "continents": ["Asia", "North America", "Europe"], "conservation_status": "Not Evaluated"},
    {"common_name": "Common Puffball", "scientific_name": "Lycoperdon perlatum", "category": "Fungi", "order": "Agaricales", "family": "Lycoperdaceae", "genus": "Lycoperdon", "description": "Round white mushroom covered in spiny warts, releases spores as a puff.", "habitat": "Woodlands, forests, grassy areas", "diet": "Saprotrophic", "continents": ["North America", "Europe", "Asia"], "conservation_status": "Not Evaluated"},

    # === MORE FISH (8+) ===
    {"common_name": "Rainbow Trout", "scientific_name": "Oncorhynchus mykiss", "category": "Fish", "order": "Salmoniformes", "family": "Salmonidae", "genus": "Oncorhynchus", "description": "Colorful trout with pink-red stripe along side, popular game fish.", "habitat": "Cold streams, rivers, lakes", "diet": "Carnivore — insects, crustaceans, small fish", "continents": ["North America", "Europe", "Asia", "South America", "Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Brown Trout", "scientific_name": "Salmo trutta", "category": "Fish", "order": "Salmoniformes", "family": "Salmonidae", "genus": "Salmo", "description": "Olive-brown trout with red and black spots, wary and challenging catch.", "habitat": "Rivers, streams, lakes", "diet": "Carnivore — insects, crayfish, small fish", "continents": ["Europe", "Asia", "North America", "Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Chinook Salmon", "scientific_name": "Oncorhynchus tshawytscha", "category": "Fish", "order": "Salmoniformes", "family": "Salmonidae", "genus": "Oncorhynchus", "description": "Largest Pacific salmon, turning red during spawning.", "habitat": "Pacific Ocean, coastal rivers and streams", "diet": "Carnivore — fish, crustaceans, squid", "continents": ["North America", "Asia"], "conservation_status": "Least Concern"},
    {"common_name": "Yellow Perch", "scientific_name": "Perca flavescens", "category": "Fish", "order": "Perciformes", "family": "Percidae", "genus": "Perca", "description": "Gold-green fish with dark vertical bars, popular panfish.", "habitat": "Lakes, ponds, slow rivers", "diet": "Carnivore — insects, crustaceans, small fish", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Northern Pike", "scientific_name": "Esox lucius", "category": "Fish", "order": "Esociformes", "family": "Esocidae", "genus": "Esox", "description": "Long predatory fish with duck-like snout and sharp teeth.", "habitat": "Weedy lakes, slow rivers", "diet": "Carnivore — fish, frogs, small mammals", "continents": ["North America", "Europe", "Asia"], "conservation_status": "Least Concern"},
    {"common_name": "Chain Pickerel", "scientific_name": "Esox niger", "category": "Fish", "order": "Esociformes", "family": "Esocidae", "genus": "Esox", "description": "Medium pike-like fish with chain-link pattern on sides.", "habitat": "Weedy ponds, lakes, slow streams", "diet": "Carnivore — fish, frogs, crayfish", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Channel Catfish", "scientific_name": "Ictalurus punctatus", "category": "Fish", "order": "Siluriformes", "family": "Ictaluridae", "genus": "Ictalurus", "description": "Smooth-skinned fish with barbels (whiskers), forked tail.", "habitat": "Rivers, lakes, reservoirs", "diet": "Omnivore — fish, insects, crustaceans, plants", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Smallmouth Bass", "scientific_name": "Micropterus dolomieu", "category": "Fish", "order": "Perciformes", "family": "Centrarchidae", "genus": "Micropterus", "description": "Bronze-green game fish with red eyes, powerful fighter.", "habitat": "Clear lakes, cool rivers, streams", "diet": "Carnivore — crayfish, fish, insects", "continents": ["North America"], "conservation_status": "Least Concern"},

    # === MORE CRUSTACEANS (5+) ===
    {"common_name": "Red Swamp Crayfish", "scientific_name": "Procambarus clarkii", "category": "Crustacean", "order": "Decapoda", "family": "Cambaridae", "genus": "Procambarus", "description": "Bright red freshwater crayfish, invasive in many regions, popular as food.", "habitat": "Marshes, swamps, streams, ponds", "diet": "Omnivore — plants, insects, carrion", "continents": ["North America", "Europe", "Asia", "Africa"], "conservation_status": "Not Evaluated"},
    {"common_name": "Acorn Barnacle", "scientific_name": "Semibalanus balanoides", "category": "Crustacean", "order": "Sessilia", "family": "Balanidae", "genus": "Semibalanus", "description": "Small gray-white barnacle that forms dense encrustations on rocky shores.", "habitat": "Intertidal rocky shores", "diet": "Filter feeder — plankton", "continents": ["North America", "Europe"], "conservation_status": "Not Evaluated"},
    {"common_name": "Common Pill Bug", "scientific_name": "Armadillidium vulgare", "category": "Crustacean", "order": "Isopoda", "family": "Armadillidiidae", "genus": "Armadillidium", "description": "Gray isopod that rolls into a ball when disturbed, also called roly-poly.", "habitat": "Gardens, leaf litter, damp soil", "diet": "Detritivore — decaying plant matter", "continents": ["Europe", "Asia", "North America", "Africa"], "conservation_status": "Not Evaluated"},
    {"common_name": "European Green Crab", "scientific_name": "Carcinus maenas", "category": "Crustacean", "order": "Decapoda", "family": "Carcinidae", "genus": "Carcinus", "description": "Small green shore crab, highly invasive worldwide.", "habitat": "Intertidal zones, estuaries", "diet": "Carnivore — mollusks, crustaceans, worms", "continents": ["Europe", "Africa", "North America", "Oceania"], "conservation_status": "Not Evaluated"},

    # === MORE MOLLUSKS (5+) ===
    {"common_name": "Hardshell Clam", "scientific_name": "Mercenaria mercenaria", "category": "Mollusk", "order": "Venerida", "family": "Veneridae", "genus": "Mercenaria", "description": "Thick-shelled edible clam, also known as quahog.", "habitat": "Sandy and muddy bays, estuaries", "diet": "Filter feeder — plankton", "continents": ["North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Bay Scallop", "scientific_name": "Argopecten irradians", "category": "Mollusk", "order": "Pectinida", "family": "Pectinidae", "genus": "Argopecten", "description": "Small scallop with ribbed shell, can swim by clapping valves.", "habitat": "Seagrass beds, sandy bays", "diet": "Filter feeder — plankton", "continents": ["North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Atlantic Jackknife Clam", "scientific_name": "Ensis directus", "category": "Mollusk", "order": "Adapedonta", "family": "Pharidae", "genus": "Ensis", "description": "Long narrow clam shaped like a straight razor.", "habitat": "Sandy beaches, intertidal", "diet": "Filter feeder — plankton", "continents": ["North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Common Periwinkle", "scientific_name": "Littorina littorea", "category": "Mollusk", "order": "Littorinimorpha", "family": "Littorinidae", "genus": "Littorina", "description": "Small spiral-shelled snail abundant on rocky shores.", "habitat": "Rocky intertidal shores", "diet": "Herbivore — algae", "continents": ["Europe", "North America"], "conservation_status": "Not Evaluated"},

    # === MORE SPIDERS (5+) ===
    {"common_name": "Black Widow", "scientific_name": "Latrodectus mactans", "category": "Arachnid", "order": "Araneae", "family": "Theridiidae", "genus": "Latrodectus", "description": "Black spider with red hourglass marking on underside, venomous.", "habitat": "Dark corners, woodpiles, sheds", "diet": "Carnivore — insects", "continents": ["North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Brown Recluse", "scientific_name": "Loxosceles reclusa", "category": "Arachnid", "order": "Araneae", "family": "Sicariidae", "genus": "Loxosceles", "description": "Brown spider with violin-shaped marking, venomous but reclusive.", "habitat": "Dark closets, basements, attics", "diet": "Carnivore — insects", "continents": ["North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Orb Weaver Spider", "scientific_name": "Araneus diadematus", "category": "Arachnid", "order": "Araneae", "family": "Araneidae", "genus": "Araneus", "description": "Garden spider that builds large circular webs, with cross-shaped markings.", "habitat": "Gardens, woodlands, fields", "diet": "Carnivore — flying insects", "continents": ["Europe", "Asia", "North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Jumping Spider", "scientific_name": "Salticus scenicus", "category": "Arachnid", "order": "Araneae", "family": "Salticidae", "genus": "Salticus", "description": "Small black-and-white spider with excellent vision, pounces on prey.", "habitat": "Walls, rocks, gardens, urban areas", "diet": "Carnivore — small insects", "continents": ["Europe", "Asia", "North America"], "conservation_status": "Not Evaluated"},
    {"common_name": "Wolf Spider", "scientific_name": "Lycosa tarantula", "category": "Arachnid", "order": "Araneae", "family": "Lycosidae", "genus": "Lycosa", "description": "Large brown hairy spider that hunts on ground, carries young on back.", "habitat": "Leaf litter, burrows, grassy areas", "diet": "Carnivore — insects, small invertebrates", "continents": ["Europe", "Asia", "Africa"], "conservation_status": "Not Evaluated"},
    {"common_name": "Daddy Longlegs", "scientific_name": "Phalangium opilio", "category": "Arachnid", "order": "Opiliones", "family": "Phalangiidae", "genus": "Phalangium", "description": "Harvestman with extremely long thin legs, not a true spider.", "habitat": "Gardens, fields, forests", "diet": "Omnivore — insects, dead matter, plant material", "continents": ["Europe", "Asia", "North America"], "conservation_status": "Not Evaluated"},

    # === MORE BIRDS FROM OTHER REGIONS (15+) ===
    {"common_name": "European Robin", "scientific_name": "Erithacus rubecula", "category": "Bird", "order": "Passeriformes", "family": "Muscicapidae", "genus": "Erithacus", "description": "Small plump bird with orange-red breast and face, beloved garden bird.", "habitat": "Woodlands, gardens, parks", "diet": "Insectivore — insects, worms, berries", "continents": ["Europe", "Asia", "Africa"], "conservation_status": "Least Concern"},
    {"common_name": "Great Tit", "scientific_name": "Parus major", "category": "Bird", "order": "Passeriformes", "family": "Paridae", "genus": "Parus", "description": "Large tit with black head and yellow breast, common garden visitor.", "habitat": "Woodlands, gardens, parks", "diet": "Insectivore — insects, seeds, nuts", "continents": ["Europe", "Asia", "Africa"], "conservation_status": "Least Concern"},
    {"common_name": "Blue Tit", "scientific_name": "Cyanistes caeruleus", "category": "Bird", "order": "Passeriformes", "family": "Paridae", "genus": "Cyanistes", "description": "Small colorful tit with blue cap, white face, yellow breast.", "habitat": "Woodlands, gardens, parks", "diet": "Insectivore — insects, seeds, suet", "continents": ["Europe", "Asia"], "conservation_status": "Least Concern"},
    {"common_name": "Common Blackbird", "scientific_name": "Turdus merula", "category": "Bird", "order": "Passeriformes", "family": "Turdidae", "genus": "Turdus", "description": "Black thrush with orange-yellow bill, melodious song.", "habitat": "Gardens, woodlands, parks", "diet": "Omnivore — insects, worms, berries, fruits", "continents": ["Europe", "Asia", "Africa", "Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Carrion Crow", "scientific_name": "Corvus corone", "category": "Bird", "order": "Passeriformes", "family": "Corvidae", "genus": "Corvus", "description": "All-black corvid with thick bill, intelligent and adaptable.", "habitat": "Woodlands, farms, urban areas", "diet": "Omnivore — carrion, insects, grains, eggs", "continents": ["Europe", "Asia"], "conservation_status": "Least Concern"},
    {"common_name": "Kookaburra", "scientific_name": "Dacelo novaeguineae", "category": "Bird", "order": "Coraciiformes", "family": "Alcedinidae", "genus": "Dacelo", "description": "Large kingfisher with loud laughing call, iconic Australian bird.", "habitat": "Woodlands, open forests", "diet": "Carnivore — snakes, lizards, insects, small birds", "continents": ["Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Superb Lyrebird", "scientific_name": "Menura novaehollandiae", "category": "Bird", "order": "Passeriformes", "family": "Menuridae", "genus": "Menura", "description": "Large ground-dwelling bird with spectacular tail, mimics virtually any sound.", "habitat": "Rainforests, wet forests", "diet": "Insectivore — insects, spiders, worms", "continents": ["Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Emperor Penguin", "scientific_name": "Aptenodytes forsteri", "category": "Bird", "order": "Sphenisciformes", "family": "Spheniscidae", "genus": "Aptenodytes", "description": "Largest penguin, breeds on Antarctic sea ice during harsh winter.", "habitat": "Antarctic sea ice, coastal waters", "diet": "Carnivore — fish, krill, squid", "continents": ["Antarctica"], "conservation_status": "Near Threatened"},
    {"common_name": "Splendid Fairywren", "scientific_name": "Malurus splendens", "category": "Bird", "order": "Passeriformes", "family": "Maluridae", "genus": "Malurus", "description": "Brilliant blue and black wren with long tail, Australian native.", "habitat": "Scrublands, woodlands, arid zones", "diet": "Insectivore — insects, seeds", "continents": ["Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Resplendent Quetzal", "scientific_name": "Pharomachrus mocinno", "category": "Bird", "order": "Trogoniformes", "family": "Trogonidae", "genus": "Pharomachrus", "description": "Vibrant green bird with red belly and long iridescent tail feathers.", "habitat": "Cloud forests", "diet": "Frugivore — wild avocados, berries, insects", "continents": ["Central America"], "conservation_status": "Near Threatened"},
    {"common_name": "Toco Toucan", "scientific_name": "Ramphastos toco", "category": "Bird", "order": "Piciformes", "family": "Ramphastidae", "genus": "Ramphastos", "description": "Large black-and-white bird with enormous orange beak.", "habitat": "Tropical forests, savannas", "diet": "Frugivore — fruits, insects, small vertebrates", "continents": ["South America"], "conservation_status": "Least Concern"},
    {"common_name": "Scarlet Macaw", "scientific_name": "Ara macao", "category": "Bird", "order": "Psittaciformes", "family": "Psittacidae", "genus": "Ara", "description": "Large red, yellow, and blue parrot, iconic rainforest species.", "habitat": "Tropical rainforests", "diet": "Herbivore — fruits, nuts, seeds", "continents": ["Central America", "South America"], "conservation_status": "Least Concern"},
    {"common_name": "African Grey Parrot", "scientific_name": "Psittacus erithacus", "category": "Bird", "order": "Psittaciformes", "family": "Psittacidae", "genus": "Psittacus", "description": "Highly intelligent medium-sized gray parrot with red tail, remarkable mimic.", "habitat": "Tropical rainforests", "diet": "Herbivore — fruits, nuts, seeds", "continents": ["Africa"], "conservation_status": "Endangered"},
    {"common_name": "Shoebill", "scientific_name": "Balaeniceps rex", "category": "Bird", "order": "Pelecaniformes", "family": "Balaenicipitidae", "genus": "Balaeniceps", "description": "Prehistoric-looking stork-like bird with massive shoe-shaped bill.", "habitat": "Swamps, marshes of East Africa", "diet": "Carnivore — lungfish, frogs, fish", "continents": ["Africa"], "conservation_status": "Vulnerable"},
    {"common_name": "Peacock", "scientific_name": "Pavo cristatus", "category": "Bird", "order": "Galliformes", "family": "Phasianidae", "genus": "Pavo", "description": "Large ground bird with iridescent blue body and spectacular tail display.", "habitat": "Forests, gardens, farmlands", "diet": "Omnivore — seeds, insects, small reptiles", "continents": ["Asia", "Europe", "Africa", "North America"], "conservation_status": "Least Concern"},
    {"common_name": "Hoatzin", "scientific_name": "Opisthocomus hoazin", "category": "Bird", "order": "Opisthocomiformes", "family": "Opisthocomidae", "genus": "Opisthocomus", "description": "Primitive tropical bird with claws on wings as chicks, ferments leaves like a cow.", "habitat": "Amazon rainforest river edges", "diet": "Herbivore — leaves, fruits, flowers", "continents": ["South America"], "conservation_status": "Least Concern"},

    # === MORE PLANTS FROM OTHER REGIONS (10+) ===
    {"common_name": "Giant Sequoia", "scientific_name": "Sequoiadendron giganteum", "category": "Plant", "order": "Pinales", "family": "Cupressaceae", "genus": "Sequoiadendron", "description": "Massive evergreen tree, largest by volume on Earth.", "habitat": "Sierra Nevada mountains", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Endangered"},
    {"common_name": "Coast Redwood", "scientific_name": "Sequoia sempervirens", "category": "Plant", "order": "Pinales", "family": "Cupressaceae", "genus": "Sequoia", "description": "Tallest tree species on Earth, reaching over 115 meters.", "habitat": "Coastal fog belt of California", "diet": "Photosynthetic", "continents": ["North America"], "conservation_status": "Endangered"},
    {"common_name": "Saguaro Cactus", "scientific_name": "Carnegiea gigantea", "category": "Plant", "order": "Caryophyllales", "family": "Cactaceae", "genus": "Carnegiea", "description": "Iconic columnar cactus of the Sonoran Desert with upraised arms.", "habitat": "Sonoran Desert", "diet": "Photosynthetic (CAM photosynthesis)", "continents": ["North America"], "conservation_status": "Least Concern"},
    {"common_name": "Venus Flytrap", "scientific_name": "Dionaea muscipula", "category": "Plant", "order": "Caryophyllales", "family": "Droseraceae", "genus": "Dionaea", "description": "Carnivorous plant that snaps shut on insects with modified leaves.", "habitat": "Bogs, wetlands of the Carolinas", "diet": "Carnivorous — insects, spiders", "continents": ["North America"], "conservation_status": "Vulnerable"},
    {"common_name": "Baobab", "scientific_name": "Adansonia digitata", "category": "Plant", "order": "Malvales", "family": "Malvaceae", "genus": "Adansonia", "description": "Massive-trunked tree that stores water in its thick bark, 'Tree of Life'.", "habitat": "African savannas, dry woodlands", "diet": "Photosynthetic", "continents": ["Africa"], "conservation_status": "Least Concern"},
    {"common_name": "Coconut Palm", "scientific_name": "Cocos nucifera", "category": "Plant", "order": "Arecales", "family": "Arecaceae", "genus": "Cocos", "description": "Tropical palm producing large fibrous fruit with milk and meat.", "habitat": "Tropical coastal areas", "diet": "Photosynthetic", "continents": ["Asia", "Africa", "South America", "Oceania", "North America"], "conservation_status": "Least Concern"},
    {"common_name": "Japanese Cherry Blossom", "scientific_name": "Prunus serrulata", "category": "Plant", "order": "Rosales", "family": "Rosaceae", "genus": "Prunus", "description": "Ornamental cherry tree with spectacular spring pink blossoms.", "habitat": "Gardens, parks", "diet": "Photosynthetic", "continents": ["Asia", "Europe", "North America"], "conservation_status": "Least Concern"},
    {"common_name": "Lotus", "scientific_name": "Nelumbo nucifera", "category": "Plant", "order": "Proteales", "family": "Nelumbonaceae", "genus": "Nelumbo", "description": "Sacred aquatic plant with large pink or white flowers above water.", "habitat": "Ponds, lakes, slow rivers", "diet": "Photosynthetic", "continents": ["Asia", "Africa", "Oceania", "Europe"], "conservation_status": "Least Concern"},

    # === MORE MAMMALS FROM OTHER REGIONS (10+) ===
    {"common_name": "Kangaroo", "scientific_name": "Macropus giganteus", "category": "Mammal", "order": "Diprotodontia", "family": "Macropodidae", "genus": "Macropus", "description": "Large iconic Australian marsupial with powerful hind legs for hopping.", "habitat": "Grasslands, woodlands, savannas", "diet": "Herbivore — grasses, leaves", "continents": ["Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Koala", "scientific_name": "Phascolarctos cinereus", "category": "Mammal", "order": "Diprotodontia", "family": "Phascolarctidae", "genus": "Phascolarctos", "description": "Arboreal marsupial that feeds almost exclusively on eucalyptus leaves.", "habitat": "Eucalyptus forests", "diet": "Herbivore — eucalyptus leaves", "continents": ["Oceania"], "conservation_status": "Vulnerable"},
    {"common_name": "Platypus", "scientific_name": "Ornithorhynchus anatinus", "category": "Mammal", "order": "Monotremata", "family": "Ornithorhynchidae", "genus": "Ornithorhynchus", "description": "Egg-laying mammal with duck-like bill and beaver-like tail.", "habitat": "Rivers, streams of eastern Australia", "diet": "Carnivore — insects, larvae, crustaceans", "continents": ["Oceania"], "conservation_status": "Near Threatened"},
    {"common_name": "Red Kangaroo", "scientific_name": "Osphranter rufus", "category": "Mammal", "order": "Diprotodontia", "family": "Macropodidae", "genus": "Osphranter", "description": "Largest marsupial, males are reddish and can hop at 60+ km/h.", "habitat": "Arid and semi-arid grasslands", "diet": "Herbivore — grasses, shrubs", "continents": ["Oceania"], "conservation_status": "Least Concern"},
    {"common_name": "Giant Panda", "scientific_name": "Ailuropoda melanoleuca", "category": "Mammal", "order": "Carnivora", "family": "Ursidae", "genus": "Ailuropoda", "description": "Black-and-white bear that feeds almost exclusively on bamboo.", "habitat": "Bamboo forests of central China", "diet": "Herbivore — bamboo", "continents": ["Asia"], "conservation_status": "Vulnerable"},
    {"common_name": "Bengal Tiger", "scientific_name": "Panthera tigris", "category": "Mammal", "order": "Carnivora", "family": "Felidae", "genus": "Panthera", "description": "Large orange-and-black striped big cat, apex predator of Asia.", "habitat": "Tropical forests, mangroves, grasslands", "diet": "Carnivore — deer, wild boar, buffalo", "continents": ["Asia"], "conservation_status": "Endangered"},
    {"common_name": "African Elephant", "scientific_name": "Loxodonta africana", "category": "Mammal", "order": "Proboscidea", "family": "Elephantidae", "genus": "Loxodonta", "description": "Largest land animal, with distinctive large ears and tusks.", "habitat": "Savannas, forests, deserts", "diet": "Herbivore — grasses, leaves, bark, fruits", "continents": ["Africa"], "conservation_status": "Endangered"},
    {"common_name": "Giraffe", "scientific_name": "Giraffa camelopardalis", "category": "Mammal", "order": "Artiodactyla", "family": "Giraffidae", "genus": "Giraffa", "description": "Tallest land animal with extremely long neck and spotted pattern.", "habitat": "African savannas, open woodlands", "diet": "Herbivore — leaves, twigs, fruits", "continents": ["Africa"], "conservation_status": "Vulnerable"},
    {"common_name": "Emperor Tamarin", "scientific_name": "Saguinus imperator", "category": "Mammal", "order": "Primates", "family": "Callitrichidae", "genus": "Saguinus", "description": "Small monkey with long white mustache, native to Amazon rainforest.", "habitat": "Amazon rainforest", "diet": "Omnivore — fruits, insects, tree sap", "continents": ["South America"], "conservation_status": "Least Concern"},
    {"common_name": "Sloth", "scientific_name": "Bradypus tridactylus", "category": "Mammal", "order": "Pilosa", "family": "Bradypodidae", "genus": "Bradypus", "description": "Slow-moving arboreal mammal with three-toed claws, sleeps up to 20 hours.", "habitat": "Rainforests", "diet": "Herbivore — leaves, buds, twigs", "continents": ["South America"], "conservation_status": "Least Concern"},

    # === MORE REPTILES (5+) ===
    {"common_name": "Komodo Dragon", "scientific_name": "Varanus komodoensis", "category": "Reptile", "order": "Squamata", "family": "Varanidae", "genus": "Varanus", "description": "Largest living lizard, powerful predator with venomous bite.", "habitat": "Indonesian islands, savannas, forests", "diet": "Carnivore — deer, pigs, buffalo, carrion", "continents": ["Asia"], "conservation_status": "Endangered"},
    {"common_name": "Green Iguana", "scientific_name": "Iguana iguana", "category": "Reptile", "order": "Squamata", "family": "Iguanidae", "genus": "Iguana", "description": "Large green lizard with crest of spines down back and long tail.", "habitat": "Tropical forests, near water", "diet": "Herbivore — leaves, fruits, flowers", "continents": ["Central America", "South America", "North America"], "conservation_status": "Least Concern"},
    {"common_name": "Galapagos Tortoise", "scientific_name": "Chelonoidis niger", "category": "Reptile", "order": "Testudines", "family": "Testudinidae", "genus": "Chelonoidis", "description": "Giant tortoise weighing over 400 kg, can live over 100 years.", "habitat": "Galapagos Islands", "diet": "Herbivore — grasses, cacti, fruits", "continents": ["South America"], "conservation_status": "Endangered"},
    {"common_name": "Leatherback Sea Turtle", "scientific_name": "Dermochelys coriacea", "category": "Reptile", "order": "Testudines", "family": "Dermochelyidae", "genus": "Dermochelys", "description": "Largest sea turtle with leathery shell instead of hard plates.", "habitat": "Open oceans, tropical and temperate waters", "diet": "Carnivore — jellyfish", "continents": ["North America", "South America", "Africa", "Europe", "Asia", "Oceania"], "conservation_status": "Vulnerable"},

    # === MORE AMPHIBIANS (5+) ===
    {"common_name": "Red-eyed Tree Frog", "scientific_name": "Agalychnis callidryas", "category": "Amphibian", "order": "Anura", "family": "Hylidae", "genus": "Agalychnis", "description": "Bright green frog with red eyes, blue and yellow sides.", "habitat": "Tropical rainforests near water", "diet": "Insectivore — crickets, flies, moths", "continents": ["Central America"], "conservation_status": "Least Concern"},
    {"common_name": "Poison Dart Frog", "scientific_name": "Dendrobates tinctorius", "category": "Amphibian", "order": "Anura", "family": "Dendrobatidae", "genus": "Dendrobates", "description": "Vibrantly colored frog with toxic skin secretions, often blue or yellow.", "habitat": "Rainforest floors", "diet": "Insectivore — ants, termites, small insects", "continents": ["South America"], "conservation_status": "Least Concern"},
    {"common_name": "Axolotl", "scientific_name": "Ambystoma mexicanum", "category": "Amphibian", "order": "Caudata", "family": "Ambystomatidae", "genus": "Ambystoma", "description": "Neotenic salamander that retains gills throughout life, remarkable regenerator.", "habitat": "Lakes of Xochimilco, Mexico", "diet": "Carnivore — worms, insects, small fish", "continents": ["North America"], "conservation_status": "Critically Endangered"},
    {"common_name": "Fire Salamander", "scientific_name": "Salamandra salamandra", "category": "Amphibian", "order": "Caudata", "family": "Salamandridae", "genus": "Salamandra", "description": "Striking black-and-yellow salamander, lives in damp forests.", "habitat": "Damp deciduous forests", "diet": "Carnivore — insects, worms, slugs", "continents": ["Europe", "Asia", "Africa"], "conservation_status": "Least Concern"},
]

# ── Descriptive templates for generating habitat/diet when missing ──
HABITAT_MAP = {
    "Bird": "Widespread — forests, grasslands, wetlands, urban areas",
    "Mammal": "Varied — forests, grasslands, mountains, urban areas",
    "Insect": "Widespread — gardens, forests, meadows, wetlands",
    "Plant": "Woodlands, forests, meadows, wetlands, disturbed areas",
    "Fungi": "Woodlands, forests, decomposing wood, soil",
    "Amphibian": "Wetlands, ponds, streams, damp forests",
    "Reptile": "Varied — forests, grasslands, deserts, wetlands, rocky areas",
    "Fish": "Freshwater lakes, rivers, ponds, streams; coastal marine waters",
    "Mollusk": "Varied — gardens, forests, fresh and salt water",
    "Arachnid": "Gardens, forests, fields, urban areas",
    "Crustacean": "Aquatic — freshwater and marine environments",
}

DIET_MAP = {
    "Bird": "Insectivore / granivore / omnivore (varies by species)",
    "Mammal": "Omnivore / herbivore / carnivore (varies by species)",
    "Insect": "Herbivore / predator / detritivore / nectarivore (varies)",
    "Plant": "Photosynthetic — obtains energy from sunlight",
    "Fungi": "Saprotrophic / parasitic / mycorrhizal — decomposes organic matter",
    "Amphibian": "Carnivore — insects, worms, small invertebrates",
    "Reptile": "Carnivore / omnivore — insects, small mammals, plants (varies)",
    "Fish": "Carnivore / omnivore / herbivore (varies by species)",
    "Mollusk": "Herbivore / detritivore / carnivore (varies by species)",
    "Arachnid": "Carnivore — insects and small invertebrates",
    "Crustacean": "Omnivore / detritivore / filter feeder (varies by species)",
}


def fix_existing_species(catalog):
    """Apply taxonomic fixes and add continents to existing species."""
    fixed = 0
    for sp in catalog:
        cn = sp.get("common_name", "")
        # Fix taxonomy
        if cn in TAXONOMY_FIXES:
            fixes = TAXONOMY_FIXES[cn]
            changed = False
            for key, val in fixes.items():
                if not sp.get(key, "") or sp[key] == "Charadriiformes" or sp[key] == "Laridae" or (
                        key == "order" and sp.get(key, "") in ("Rodentia", "Diptera", "Hymenoptera", "Charadriiformes", "Lepisosteiformes", "Carnivora", "Passeriformes", "Anseriformes")):
                    sp[key] = val
                    changed = True
            if changed:
                fixed += 1

        # Add continents if missing
        if not sp.get("continents"):
            sp["continents"] = KNOWN_DISTRIBUTIONS.get(cn, ["North America"])

        # Fix blank taxonomy fields using known data
        if not sp.get("phylum", ""):
            if sp.get("category") in ("Bird", "Mammal", "Amphibian", "Reptile", "Fish"):
                sp["phylum"] = "Chordata"
            elif sp.get("category") in ("Insect", "Arachnid", "Crustacean"):
                sp["phylum"] = "Arthropoda"
            elif sp.get("category") == "Plant":
                sp["phylum"] = "Angiospermae"
            elif sp.get("category") == "Fungi":
                sp["phylum"] = "Basidiomycota"
            elif sp.get("category") == "Mollusk":
                sp["phylum"] = "Mollusca"

        if not sp.get("kingdom", ""):
            if sp.get("category") in ("Fungi",):
                sp["kingdom"] = "Fungi"
            elif sp.get("category") in ("Plant",):
                sp["kingdom"] = "Plantae"
            elif sp.get("category") in ("Mollusk",):
                sp["kingdom"] = "Animalia"
            else:
                sp["kingdom"] = "Animalia"

        # Fill blank habitat/diet
        cat = sp.get("category", "Other")
        if not sp.get("habitat", ""):
            sp["habitat"] = HABITAT_MAP.get(cat, "Varied habitats")
        if not sp.get("diet", ""):
            sp["diet"] = DIET_MAP.get(cat, "Varies by species")

        # Fill tags if missing or minimal
        if len(sp.get("tags", [])) <= 1:
            sp["tags"] = build_tags(cn, cat)

    print(f"  Fixed taxonomy for {fixed} species")
    return catalog


def fetch_from_inaturalist(target=100):
    """Fetch species from iNaturalist API across categories and continents."""
    fetched = []
    seen_ids = set()

    # Fetch popular species by categories
    for cat in ["Bird", "Mammal", "Insect", "Plant", "Fungi", "Amphibian", "Reptile", "Fish", "Mollusk", "Arachnid"]:
        print(f"  Fetching {cat}...")
        results = fetch_species_batch(cat, page=1, per_page=50)
        if not results:
            continue
        for t in results:
            if len(fetched) >= target:
                break
            record = parse_inat_taxon(t)
            if record and record["common_name"] not in seen_ids:
                seen_ids.add(record["common_name"])
                fetched.append(record)
        time.sleep(1.1)  # Rate limit

    # Fetch species by continent for regional diversity
    for cname, cinfo in CONTINENTS.items():
        if len(fetched) >= target * 2:
            break
        print(f"  Fetching {cname}...")
        results = fetch_species_batch("Plant", place_id=cinfo["place_id"], page=1, per_page=20)
        for t in (results or []):
            if len(fetched) >= target * 2:
                break
            record = parse_inat_taxon(t)
            if record and record["common_name"] not in seen_ids:
                seen_ids.add(record["common_name"])
                record["continents"] = [cname]
                fetched.append(record)
        time.sleep(1.1)

    print(f"  Fetched {len(fetched)} species from iNaturalist")
    return fetched


def add_local_species(catalog, existing_names):
    """Add locally defined species to reach target."""
    added = 0
    for sp in LOCAL_SPECIES:
        if sp["common_name"] not in existing_names:
            record = {
                "id": "gen_" + sp["scientific_name"].lower().replace(" ", "_"),
                "common_name": sp["common_name"],
                "scientific_name": sp["scientific_name"],
                "category": sp["category"],
                "description": sp.get("description", ""),
                "habitat": sp.get("habitat", HABITAT_MAP.get(sp["category"], "")),
                "diet": sp.get("diet", DIET_MAP.get(sp["category"], "")),
                "conservation_status": sp.get("conservation_status", "Not Evaluated"),
                "image_url": "",
                "thumbnail_url": "",
                "tags": build_tags(sp["common_name"], sp["category"]),
                "key_features": [],
                "similar_species": [],
                "kingdom": "Animalia" if sp["category"] not in ("Plant", "Fungi") else ("Plantae" if sp["category"] == "Plant" else "Fungi"),
                "phylum": {"Bird": "Chordata", "Mammal": "Chordata", "Insect": "Arthropoda", "Plant": "Angiospermae",
                           "Fungi": "Basidiomycota", "Amphibian": "Chordata", "Reptile": "Chordata", "Fish": "Chordata",
                           "Mollusk": "Mollusca", "Arachnid": "Arthropoda", "Crustacean": "Arthropoda"}.get(sp["category"], ""),
                "order": sp.get("order", ""),
                "family": sp.get("family", ""),
                "genus": sp.get("genus", ""),
                "continents": sp.get("continents", ["North America"]),
            }
            catalog.append(record)
            existing_names.add(sp["common_name"])
            added += 1
    print(f"  Added {added} locally generated species")
    return catalog


def main():
    # Load existing catalog
    print("Loading existing catalog...")
    with open(CATALOG_PATH, "r") as f:
        catalog = json.load(f)
    print(f"  Existing count: {len(catalog)}")

    # Fix existing species taxonomy and fill blanks
    print("Fixing existing species...")
    catalog = fix_existing_species(catalog)

    # Track existing names
    existing_names = set(sp["common_name"] for sp in catalog)

    # Fetch from iNaturalist
    print("Fetching from iNaturalist API...")
    inat_species = fetch_from_inaturalist(target=100)
    for sp in inat_species:
        if sp["common_name"] not in existing_names:
            catalog.append(sp)
            existing_names.add(sp["common_name"])

    # Add locally generated species to fill gaps
    print("Adding locally generated species...")
    catalog = add_local_species(catalog, existing_names)

    # Ensure unique IDs
    seen_ids = set()
    unique_catalog = []
    for sp in catalog:
        sp_id = sp.get("id", "")
        if sp_id in seen_ids:
            # Generate unique id
            sp["id"] = sp_id + "_" + str(random.randint(1000, 9999))
        seen_ids.add(sp["id"])
        unique_catalog.append(sp)

    # Sort by category then common name
    unique_catalog.sort(key=lambda x: (x.get("category", ""), x.get("common_name", "")))

    # Write output
    print(f"\nTotal species: {len(unique_catalog)}")
    with open(CATALOG_PATH, "w") as f:
        json.dump(unique_catalog, f, indent=2, ensure_ascii=False)
    print(f"Written to {CATALOG_PATH}")

    # Summary by category
    from collections import Counter
    cat_counts = Counter(sp.get("category", "Other") for sp in unique_catalog)
    print("\nCatalog by category:")
    for cat, count in sorted(cat_counts.items(), key=lambda x: -x[1]):
        print(f"  {cat}: {count}")

    # Stats on taxonomic completeness
    with_taxonomy = sum(1 for sp in unique_catalog if sp.get("order") and sp.get("family") and sp.get("genus"))
    with_continents = sum(1 for sp in unique_catalog if sp.get("continents"))
    print(f"\n  Species with full taxonomy: {with_taxonomy}/{len(unique_catalog)}")
    print(f"  Species with continent data: {with_continents}/{len(unique_catalog)}")
    print(f"\nDone!")


if __name__ == "__main__":
    main()
