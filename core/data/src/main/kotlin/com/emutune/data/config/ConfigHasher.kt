package com.emutune.data.config

import com.emutune.model.config.ConfigKey
import com.emutune.model.config.ConfigValue
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Deterministic hashing of a configuration field map. The hash is used to detect a
 * corrupted backup and to verify that a rollback restored the exact pre-mutation state.
 */
object ConfigHasher {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun hash(fields: Map<ConfigKey, ConfigValue>): String {
        val canonical = json.encodeToString(fields)
        return sha256(canonical)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
