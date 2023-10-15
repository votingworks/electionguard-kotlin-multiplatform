package electionguard.json

import kotlinx.serialization.Serializable

/* election_manifest.jcon
{
  "label": "General Election - The United Realms of Imaginaria",
  "contests": [
    {
      "label": "For President and Vice President of The United Realms of Imaginaria",
      "selection_limit": 1,
      "options": [
        {
          "label": "Thündéroak, Vâlêriana D.\nËverbright, Ålistair R. Jr.\n(Ætherwïng)"
        },
        {
          "label": "Stârførge, Cássánder A.\nMøonfire, Célestïa L.\n(Crystâlheärt)"
        }
      ]
    },
    {
      "label": "Minister of Arcane Sciences",
      "selection_limit": 1,
      "options": [
        {
          "label": "Élyria Moonshadow\n(Crystâlheärt)"
        },
        {
          "label": "Archímedes Darkstone\n(Ætherwïng)"
        },
        {
          "label": "Seraphína Stormbinder\n(Independent)"
        },
        {
          "label": "Gávrïel Runëbørne\n(Stärsky)"
        }
      ]
    },
    ],
  "ballot_styles": [
    {
      "label": "Smoothstone County Ballot",
      "contests": [
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10
      ]
    },
    {
      "label": "Silvërspîre County Ballot",
      "contests": [
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        11
      ]
    }
  ]
}
 */

@Serializable
data class ElectionManifestJsonR(
    val label : String,
    val contests : List<ManifestContestJsonR>,
    val ballot_styles : List<BallotStyleJsonR>,
) {
    override fun toString(): String {
        return "ElectionManifestJsonR(label='$label'\n contests=$contests\n ballot_styles=$ballot_styles)"
    }
}

@Serializable
data class ManifestContestJsonR(
    val label : String,
    val selection_limit: Int,
    val options : List<ManifestOptionJsonR>,
) {
    override fun toString(): String {
        return "\n  ManifestContestJsonR(label='$label', selection_limit=$selection_limit, options=$options)\n"
    }
}

@Serializable
data class ManifestOptionJsonR(
    val label : String,
) {
    override fun toString(): String {
        return "\n    ManifestOptionJsonR(label='$label')"
    }
}

@Serializable
data class BallotStyleJsonR(
    val label : String,
    val contests : List<Int>,
) {
    override fun toString(): String {
        return "\n  BallotStyleJsonR(label='$label', contests=$contests)\n"
    }
}