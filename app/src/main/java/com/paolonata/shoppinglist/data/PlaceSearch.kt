package com.paolonata.shoppinglist.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.paolonata.shoppinglist.receipts.AddressFormatter
import com.paolonata.shoppinglist.receipts.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cerca il posto dove hai comprato.
 *
 * Usa Nominatim, il servizio di ricerca di OpenStreetMap: niente chiave,
 * niente account, niente fatturazione. In cambio i luoghi sono quelli
 * mappati dalla comunità — le catene ci sono quasi tutte, la salumeria
 * sotto casa può mancare.
 *
 * **È l'unica cosa in tutta l'app che esce in rete**, e solo mentre la
 * si usa: le foto, gli importi e il riconoscimento del testo restano sul
 * telefono come prima. Nessuna ricerca viene registrata da qualche
 * parte, e senza connessione l'unica cosa che non funziona è questa.
 */
object PlaceSearch {

    private const val ENDPOINT = "https://nominatim.openstreetmap.org"

    /**
     * Nominatim chiede a chi lo interroga di identificarsi, e limita a una
     * richiesta al secondo. Qui si cerca a mano, un posto alla volta:
     * siamo lontanissimi da quel limite.
     */
    private const val USER_AGENT = "ListaSpesa-Scontrini/1.0 (github.com/paolonata/Shopping-List-App)"

    private const val TIMEOUT_MS = 12_000

    /**
     * Cerca [query]. Se sono note le coordinate di dove ci si trova, i
     * risultati vicini vengono prima: cercando «Esselunga» si intende
     * quasi sempre quella dove si è appena stati, non una a trecento
     * chilometri.
     */
    suspend fun search(query: String, near: Location? = null): List<Place> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val url = buildString {
            append(ENDPOINT).append("/search?format=jsonv2&addressdetails=1&limit=10")
            append("&q=").append(URLEncoder.encode(trimmed, "UTF-8"))
            if (near != null) {
                // Una finestra di circa mezzo grado attorno a dove siamo:
                // orienta i risultati senza escludere il resto del mondo.
                val d = 0.5
                append("&viewbox=")
                append(near.longitude - d).append(',').append(near.latitude + d).append(',')
                append(near.longitude + d).append(',').append(near.latitude - d)
            }
        }
        fetchJsonArray(url)?.let { parsePlaces(it) }.orEmpty()
    }

    /** Che posto è, date le coordinate: serve al pulsante «sono qui». */
    suspend fun whereAmI(location: Location): Place? = withContext(Dispatchers.IO) {
        val url = "$ENDPOINT/reverse?format=jsonv2&addressdetails=1&zoom=18" +
            "&lat=${location.latitude}&lon=${location.longitude}"
        fetchJsonObject(url)?.let { toPlace(it) }
    }

    /**
     * L'ultima posizione nota, senza accendere il GPS: quando si fotografa
     * uno scontrino si è appena entrati o usciti da un negozio, quindi
     * quella basta e non costa batteria né attesa.
     */
    fun lastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            manager.getProviders(true)
                .mapNotNull { provider -> manager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
        }.getOrNull()
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /* ── Rete ──────────────────────────────────────────────────── */

    private fun fetchJsonArray(url: String): JSONArray? =
        fetch(url)?.let { runCatching { JSONArray(it) }.getOrNull() }

    private fun fetchJsonObject(url: String): JSONObject? =
        fetch(url)?.let { runCatching { JSONObject(it) }.getOrNull() }

    private fun fetch(url: String): String? = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept-Language", "it,en")
        }
        connection.use { it.inputStream.bufferedReader().readText() }
    }.getOrNull()

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }

    /* ── Lettura della risposta ────────────────────────────────── */

    private fun parsePlaces(array: JSONArray): List<Place> =
        (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let(::toPlace)
        }

    private fun toPlace(json: JSONObject): Place? {
        val lat = json.optString("lat").toDoubleOrNull() ?: return null
        val lon = json.optString("lon").toDoubleOrNull() ?: return null
        val address = json.optJSONObject("address")

        val name = json.optString("name").takeIf { it.isNotBlank() }
            ?: AddressFormatter.fallbackName(
                displayName = json.optString("display_name").takeIf { it.isNotBlank() },
                road = address?.optString("road"),
                city = address?.cityLike(),
            )

        return Place(
            name = name,
            address = AddressFormatter.compose(
                road = address?.optString("road"),
                houseNumber = address?.optString("house_number"),
                suburb = address?.optString("suburb"),
                city = address?.cityLike(),
                postcode = address?.optString("postcode"),
                country = address?.optString("country"),
            ),
            latitude = lat,
            longitude = lon,
        )
    }

    /** Il nome del comune cambia casella a seconda di dove sei al mondo. */
    private fun JSONObject.cityLike(): String? = listOf("city", "town", "village", "municipality")
        .firstNotNullOfOrNull { key -> optString(key).takeIf { it.isNotBlank() } }
}
