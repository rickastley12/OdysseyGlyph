package com.example.odysseyglyph

object LrcQueryCleaner {

    fun clean(query: String): String {
        var name = query
        // Remove file extension if present
        val dotIndex = name.lastIndexOf('.')
        if (dotIndex > 0) name = name.substring(0, dotIndex)
        
        // Remove track numbers (e.g. "01 - " or "1. ")
        name = name.replace(Regex("^\\d+\\s*-?\\s*"), "")
        
        // Replace underscores with spaces
        name = name.replace("_", " ")
        
        // Remove parentheticals like (Remastered 2011), (Radio Edit), [Official Video]
        name = name.replace(Regex("\\(.*?\\)"), "")
        name = name.replace(Regex("\\[.*?\\]"), "")
        
        // Remove common suffixes like "feat.", "ft."
        name = name.replace(Regex("(?i)\\b(feat\\.|ft\\.|featuring).*$"), "")
        
        // Remove extra spaces
        name = name.replace(Regex("\\s+"), " ").trim()
        
        return name
    }
}
