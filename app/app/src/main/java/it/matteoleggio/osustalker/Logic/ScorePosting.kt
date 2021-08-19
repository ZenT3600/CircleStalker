package it.matteoleggio.osustalker.Logic

class ScorePosting {
    fun generateScorePostTitle(userJson: Map<String, Any>, scoreJson: Map<String, Any>, beatmapJson: Map<String, Any>): String {
        // Example:
        // Lifeline | Various Artists - Songs Compilation [Marathon] +HDDT 99.34% FC | 890pp

        return "${userJson["username"]} | ${beatmapJson["artist"]} - ${beatmapJson["title"]} [${beatmapJson["version"]}] " +
                "+${parseMods(scoreJson["enabled_mods"].toString())} ${scoreJson["rank"]} Rank${if (scoreJson["perfect"].toString() == "1") " FC" else ""}"
    }
}