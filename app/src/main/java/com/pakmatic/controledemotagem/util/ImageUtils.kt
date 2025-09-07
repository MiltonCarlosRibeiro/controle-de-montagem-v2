package com.pakmatic.controledemotagem.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Salva um Bitmap na galeria pública de imagens do dispositivo.
 *
 * Esta função lida com as diferenças entre as versões do Android:
 * - Em Android 10 (API 29) e superior, usa a API MediaStore, que é a abordagem moderna e não requer permissões de escrita.
 * - Em Android 9 (API 28) e inferior, usa o método legado, que requer a permissão WRITE_EXTERNAL_STORAGE.
 *
 * @param context O Context da aplicação.
 * @param bitmap O Bitmap da imagem a ser salva.
 * @param albumName O nome do álbum onde a imagem será salva dentro da galeria (ex: "ControleDeMontagem").
 * @return A Uri da imagem salva, ou null se ocorrer uma falha.
 */
fun saveBitmapToGallery(context: Context, bitmap: Bitmap, albumName: String): Uri? {
    val displayName = "IMG_${System.currentTimeMillis()}.jpg"
    val mimeType = "image/jpeg"
    val compressFormat = Bitmap.CompressFormat.JPEG

    // Lógica para Android 10 (API 29) e superior
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            // Cria um subdiretório dentro da pasta Pictures
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$albumName"
            )
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    if (!bitmap.compress(compressFormat, 95, outputStream)) {
                        // Se a compressão falhar, retorna null
                        return null
                    }
                }
                return it
            } catch (e: Exception) {
                // Se ocorrer um erro, remove a entrada criada
                resolver.delete(it, null, null)
                e.printStackTrace()
                return null
            }
        }
        return null

    } else {
        // Lógica para Android 9 (API 28) e inferior (método legado)
        // **IMPORTANTE**: Este trecho assume que a permissão WRITE_EXTERNAL_STORAGE já foi concedida!

        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val albumDir = File(picturesDir, albumName)
        if (!albumDir.exists()) {
            albumDir.mkdirs()
        }
        val imageFile = File(albumDir, displayName)

        try {
            FileOutputStream(imageFile).use { outputStream ->
                if (!bitmap.compress(compressFormat, 95, outputStream)) {
                    return null
                }
            }

            // Notifica a galeria sobre a nova imagem
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.absolutePath),
                arrayOf(mimeType),
                null
            )

            return Uri.fromFile(imageFile)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
