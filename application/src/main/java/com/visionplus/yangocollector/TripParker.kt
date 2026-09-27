package com.visionplus.yangocollector

import java.text.Normalizer

object TripParser {

    private val regexDuree = Regex("""(\d{1,3})\s*min""")
    private val regexDistance = Regex("""(\d{1,3}[.,]\d{1,2})\s*km""")
    private val regexRevenu = Regex("""(\d{2,6})\s*FCFA""")
    private val regexDate = Regex("""(?i)(lun|mar|mer|jeu|ven|sam|dim)[.,]{1,2}\s*\d{1,2}\s*\w+\.?\s*\d{4}""")
    private val regexHeure = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")

    fun parse(texteBrutOcr: String): Trip {
        val lignes = texteBrutOcr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        val dureeMin = regexDuree.find(texteBrutOcr)?.groupValues?.get(1)?.toIntOrNull()
        val distanceKm = regexDistance.find(texteBrutOcr)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
        val revenuFcfa = regexRevenu.find(texteBrutOcr)?.groupValues?.get(1)?.toIntOrNull()
        val dateCourse = regexDate.find(texteBrutOcr)?.value

        val indexDebut = lignes.indexOfFirst { regexDate.containsMatchIn(it) }
            .let { if (it >= 0) it else lignes.indexOfFirst { l -> normaliser(l).contains("course") } }
        val indexDuree = lignes.indexOfFirst { normaliser(it).contains("duree") || regexDuree.containsMatchIn(it) }

        val blocAdresses = if (indexDebut in 0 until indexDuree) {
            lignes.subList(indexDebut + 1, indexDuree)
                .filterNot { estLigneBruit(it) }
                .joinToString(" / ")
        } else ""

        val indexEvaluer = lignes.indexOfFirst { normaliser(it).contains("evaluer") && normaliser(it).contains("passager") }
        val indexRecu = lignes.indexOfFirst { normaliser(it).contains("recu") }
        val debutFenetrePassager = maxOf(indexDuree, indexRecu)
        val nomPassager = if (debutFenetrePassager in 0 until indexEvaluer) {
            lignes.subList(debutFenetrePassager + 1, indexEvaluer)
                .filterNot { estLigneBruit(it) }
                .firstOrNull { it.any { c -> c.isLetter() } }
        } else null

        return Trip(
            dateCourse = dateCourse,
            adressesBrutes = blocAdresses,
            distanceKm = distanceKm,
            dureeMin = dureeMin,
            revenuFcfa = revenuFcfa,
            nomPassager = nomPassager,
            texteBrutOcr = texteBrutOcr
        )
    }

    private fun normaliser(s: String): String {
        val sansAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}"), "")
        return sansAccents.lowercase().trim()
    }

    private fun estLigneBruit(l: String): Boolean {
        val n = normaliser(l)
        if (n.length <= 1) return true
        if (n.matches(Regex("^[0-9\\s.,]+$"))) return true
        if (n.contains("course") || n.contains("duree") || n.contains("distance")) return true
        if (n.contains("evaluer") || n == "assistance") return true
        if (n.contains("revenus") || n.contains("paiement") || n.contains("recu")) return true
        if (n == "prix" || n == "<") return true
        if (regexHeure.containsMatchIn(l) && l.length <= 8) return true
        if (regexDistance.containsMatchIn(l) || regexDuree.containsMatchIn(l)) return true
        if (regexRevenu.containsMatchIn(l) || regexDate.containsMatchIn(l)) return true
        return false
    }
}
