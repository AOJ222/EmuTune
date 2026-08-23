package com.emutune.data.emulator

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.emutune.model.config.IniDocument
import com.emutune.model.config.IniParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Reads Dolphin's INI configuration through a user-granted SAF tree. This is the
 * confirmed path (see `docs/08-research/spikes/dolphin-saf-spike.md`): the user grants
 * Dolphin's root (or its `Config` folder) once, and this reader resolves the
 * `Dolphin.ini` document and parses it.
 *
 * It only reads — it never writes, and it never reaches Dolphin's private storage
 * directly. If no grant is held, it returns null (fail closed) rather than guessing.
 */
@Singleton
class DolphinConfigReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val grantStore: SafGrantStore,
) {

    suspend fun readDolphinIni(): IniDocument? {
        val uri = grantStore.dolphinUri.first() ?: return null
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(uri)) ?: return null

        val iniFile = resolveConfigIni(tree) ?: return null
        val text = runCatching {
            context.contentResolver.openInputStream(iniFile.uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return null

        return IniParser.parse(text)
    }

    /**
     * Locates `Dolphin.ini` under the granted tree. The grant may point at the Dolphin
     * root (`Config/Dolphin.ini`) or directly at the `Config` folder (`Dolphin.ini`).
     */
    private fun resolveConfigIni(tree: DocumentFile): DocumentFile? {
        // Grant is already the Config folder.
        tree.findFile(DOLPHIN_INI)?.let { return it }
        // Grant is the root; descend into Config.
        return tree.findFile(CONFIG_DIR)?.findFile(DOLPHIN_INI)
    }

    private companion object {
        const val CONFIG_DIR = "Config"
        const val DOLPHIN_INI = "Dolphin.ini"
    }
}
