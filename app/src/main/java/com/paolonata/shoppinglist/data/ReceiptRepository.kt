package com.paolonata.shoppinglist.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.paolonata.shoppinglist.receipts.Deadlines
import com.paolonata.shoppinglist.receipts.ParsedReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * The archive of receipts: rows in Room, images as JPEG files in the app's
 * private storage.
 *
 * Photos are downscaled and re-compressed on the way in. A phone camera
 * hands over 3–5 MB per shot; a receipt only has to stay *readable*, and at
 * 1800 px on the long side it does, for around 150–250 KB. Over a few
 * hundred receipts that is the difference between a few tens of megabytes
 * and a gigabyte.
 */
class ReceiptRepository(
    private val context: Context,
    private val dao: ReceiptDao,
) {

    fun observeAll(): Flow<List<ReceiptWithPhotos>> = dao.observeAll()

    fun observeTrashed(): Flow<List<ReceiptWithPhotos>> = dao.observeTrashed()

    fun observeById(id: Long): Flow<ReceiptWithPhotos?> = dao.observeById(id)

    suspend fun getById(id: Long): ReceiptWithPhotos? = dao.getById(id)

    /**
     * Saves a freshly taken picture as a new receipt, filling in whatever
     * [parsed] managed to read. Nothing is mandatory: an unreadable receipt
     * is still worth keeping — that is the whole point of the app.
     */
    suspend fun createFrom(sources: List<Uri>, parsed: ParsedReceipt?): Long = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val purchase = parsed?.date ?: today
        val receipt = Receipt(
            title = parsed?.merchant.orEmpty(),
            amountCents = Receipt.centsOf(parsed?.amount),
            date = purchase.toString(),
            categoryId = parsed?.category?.id ?: "altro",
        )
        val id = dao.insert(receipt)
        val photos = sources.mapIndexedNotNull { index, uri ->
            storePhoto(uri, id, index)
        }
        if (photos.isNotEmpty()) dao.insertPhotos(photos)
        id
    }

    /** Adds pages to a receipt that already exists. */
    suspend fun addPhotos(receiptId: Long, sources: List<Uri>) = withContext(Dispatchers.IO) {
        val from = dao.photosOf(receiptId).size
        val photos = sources.mapIndexedNotNull { index, uri -> storePhoto(uri, receiptId, from + index) }
        if (photos.isNotEmpty()) dao.insertPhotos(photos)
    }

    /**
     * Saves the edited fields. Deadlines that came from a shortcut are
     * recomputed against the (possibly corrected) purchase date — otherwise
     * the countdown keeps running from the day the date was first typed.
     */
    suspend fun save(receipt: Receipt) = withContext(Dispatchers.IO) {
        val purchase = runCatching { LocalDate.parse(receipt.date) }.getOrNull()
        val (ret, war) = if (purchase != null) {
            Deadlines.realign(purchase, receipt.returnDays, receipt.warrantyYears)
        } else {
            null to null
        }
        dao.update(
            receipt.copy(
                returnUntil = ret?.toString() ?: receipt.returnUntil,
                warrantyUntil = war?.toString() ?: receipt.warrantyUntil,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setReturnDone(id: Long, done: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.setReturnDone(id, if (done) now else null, now)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(id, favorite, System.currentTimeMillis())
    }

    suspend fun moveToTrash(id: Long) = withContext(Dispatchers.IO) {
        dao.moveToTrash(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Long) = withContext(Dispatchers.IO) {
        dao.restoreFromTrash(id, System.currentTimeMillis())
    }

    /** Empties the bin of what has been in there longer than [days]. */
    suspend fun purgeExpiredTrash(days: Int = TRASH_DAYS) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        dao.expiredTrash(cutoff).forEach { entry ->
            entry.photos.forEach { runCatching { File(it.path).delete() } }
            dao.purge(entry.receipt.id)
        }
    }

    /** Deletes a receipt and its image files for good. */
    suspend fun purge(id: Long) = withContext(Dispatchers.IO) {
        dao.getById(id)?.photos?.forEach { runCatching { File(it.path).delete() } }
        dao.purge(id)
    }

    private fun photoDir(): File = File(context.filesDir, "receipts").apply { mkdirs() }

    /**
     * Reads [uri], turns it the right way up and writes a downscaled JPEG
     * into private storage. Returns null when the image cannot be read at
     * all, so one bad page does not sink the whole save.
     */
    private fun storePhoto(uri: Uri, receiptId: Long, position: Int): ReceiptPhoto? {
        val bitmap = decodeDownscaled(uri) ?: return null
        val target = File(photoDir(), "${UUID.randomUUID()}.jpg")
        return runCatching {
            target.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            ReceiptPhoto(
                receiptId = receiptId,
                path = target.absolutePath,
                position = position,
                sizeBytes = target.length(),
            )
        }.getOrNull().also { bitmap.recycle() }
    }

    private fun decodeDownscaled(uri: Uri): Bitmap? = runCatching {
        // First pass: read the size only, to work out how much to subsample.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / (sample * 2) >= MAX_EDGE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val rotated = applyExifRotation(uri, decoded)
        val edge = maxOf(rotated.width, rotated.height)
        if (edge <= MAX_EDGE) return rotated

        val scale = MAX_EDGE.toFloat() / edge
        val scaled = Bitmap.createScaledBitmap(
            rotated,
            (rotated.width * scale).toInt().coerceAtLeast(1),
            (rotated.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled != rotated) rotated.recycle()
        scaled
    }.getOrNull()

    /** Phones record the orientation instead of rotating the pixels. */
    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        if (degrees == 0f) return bitmap

        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    companion object {
        private const val MAX_EDGE = 1800
        private const val JPEG_QUALITY = 82
        const val TRASH_DAYS = 30
    }
}
