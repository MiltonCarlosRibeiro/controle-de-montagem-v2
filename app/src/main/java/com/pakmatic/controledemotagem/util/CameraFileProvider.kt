package com.pakmatic.controledemotagem.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.pakmatic.controledemotagem.R
import java.io.File

// Esta classe ajuda a criar um URI seguro para a câmera salvar a foto.
object CameraFileProvider {
    fun getUri(context: Context): Uri {
        val directory = File(context.cacheDir, "images")
        directory.mkdirs()
        val file = File.createTempFile(
            "img_${System.currentTimeMillis()}_",
            ".jpg",
            directory
        )
        val authority = context.packageName + ".provider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file
        )
    }
}