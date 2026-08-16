package com.paolonata.shoppinglist.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Legge il testo stampato su una foto di scontrino.
 *
 * Il riconoscimento gira **sul telefono**: il modello viaggia dentro
 * l'APK, quindi funziona in aereo, senza account e senza che la foto di
 * uno scontrino esca mai dal dispositivo — che è il punto di tutta
 * l'app.
 *
 * Qui c'è solo il ponte verso ML Kit. Il lavoro difficile — capire quale
 * numero è il totale e quale una data — sta nel modulo `:receipts`, dove
 * si può provare senza un telefono in mano.
 */
class ReceiptScanner(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Il testo grezzo della foto, o null se non si è riusciti a leggerla.
     * Un fallimento non è un problema: lo scontrino resta salvato con i
     * campi vuoti, che è sempre meglio di non averlo.
     */
    suspend fun read(file: File): String? = runCatching {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resume(null) }
        }
    }.getOrNull()
}
