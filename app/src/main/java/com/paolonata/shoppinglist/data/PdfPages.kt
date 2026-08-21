package com.paolonata.shoppinglist.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Trasforma un PDF nelle sue pagine, come immagini.
 *
 * Scontrini e ricevute arrivano in PDF quasi sempre per email — bollette,
 * ordini online, fatture dell'hotel — e a quel punto non c'è niente da
 * fotografare. Renderizzarle in JPEG le fa entrare nell'archivio come
 * tutto il resto: stessa lista, stesso zoom, stessa lettura automatica
 * del testo, stessa condivisione. Il PDF originale non viene conservato:
 * quello che conta è la prova leggibile, e tenere due copie della stessa
 * cosa raddoppierebbe lo spazio per niente.
 */
object PdfPages {

    /** Oltre questo numero di pagine è un documento, non uno scontrino. */
    private const val MAX_PAGES = 20

    private const val TARGET_EDGE = 1800

    /**
     * Renderizza [uri] e restituisce i file JPEG prodotti, in ordine.
     * Lista vuota se il PDF non si apre: capita con i file protetti da
     * password, e un errore lì non deve buttare giù il salvataggio.
     */
    fun render(context: Context, uri: Uri, into: File): List<File> = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            renderPages(descriptor, into)
        }.orEmpty()
    }.getOrElse { emptyList() }

    private fun renderPages(descriptor: ParcelFileDescriptor, into: File): List<File> {
        val out = mutableListOf<File>()
        PdfRenderer(descriptor).use { renderer ->
            val pages = minOf(renderer.pageCount, MAX_PAGES)
            for (index in 0 until pages) {
                renderer.openPage(index).use { page ->
                    val scale = TARGET_EDGE.toFloat() / maxOf(page.width, page.height).coerceAtLeast(1)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // Un PDF ha lo sfondo trasparente, e trasparente in JPEG
                    // diventa nero: senza questo, la pagina arriva tutta scura.
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val file = File(into, "pdf-${System.currentTimeMillis()}-$index.jpg")
                    file.outputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    }
                    bitmap.recycle()
                    out += file
                }
            }
        }
        return out
    }
}
