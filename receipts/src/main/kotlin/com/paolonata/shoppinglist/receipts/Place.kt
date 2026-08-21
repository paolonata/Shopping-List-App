package com.paolonata.shoppinglist.receipts

/**
 * Dove hai comprato: il nome del posto e il suo indirizzo, con le
 * coordinate per ritrovarlo su una mappa.
 *
 * Il nome è separato dall'indirizzo di proposito: «Esselunga» accanto a
 * uno scontrino dice più di «Viale Papiniano 44», e chi cerca il negozio
 * cerca il nome. L'indirizzo serve dopo, quando il nome non basta a
 * distinguere due filiali.
 */
data class Place(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** Come si scrive per esteso: «Esselunga · Viale Papiniano 44, Milano». */
    fun oneLine(): String = if (address.isBlank()) name else "$name · $address"
}

/**
 * Mette in fila i pezzi di un indirizzo come li scriverebbe una persona.
 *
 * I servizi di mappe restituiscono i pezzi separati e non tutti sempre
 * presenti: fuori dai centri storici manca il numero civico, in campagna
 * manca la via, e in certi paesi manca il CAP. Concatenare alla cieca
 * produce «, , Milano» oppure «Milano, Milano»; qui i pezzi vuoti
 * spariscono e i doppioni non si ripetono.
 */
object AddressFormatter {

    fun compose(
        road: String? = null,
        houseNumber: String? = null,
        suburb: String? = null,
        city: String? = null,
        postcode: String? = null,
        country: String? = null,
    ): String {
        val street = listOfNotNull(road?.trimOrNull(), houseNumber?.trimOrNull())
            .joinToString(" ")
            .trimOrNull()

        val town = listOfNotNull(postcode?.trimOrNull(), city?.trimOrNull())
            .joinToString(" ")
            .trimOrNull()

        // Il quartiere si scrive solo se aggiunge qualcosa: in molte
        // risposte ripete il nome della città.
        val area = suburb?.trimOrNull()?.takeIf { !it.equals(city?.trim(), ignoreCase = true) }

        return listOfNotNull(street, area, town, country?.trimOrNull())
            .distinctBy { it.lowercase() }
            .joinToString(", ")
    }

    /**
     * Il nome da mostrare quando il servizio non ne dà uno: si ripiega
     * sulla via, che è comunque meglio di «Senza nome».
     */
    fun fallbackName(displayName: String?, road: String?, city: String?): String {
        displayName?.trimOrNull()?.let { return it.substringBefore(',').trim() }
        road?.trimOrNull()?.let { return it }
        city?.trimOrNull()?.let { return it }
        return "Luogo"
    }

    private fun String.trimOrNull(): String? = trim().ifEmpty { null }
}
