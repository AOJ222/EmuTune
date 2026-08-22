package com.emutune.data.db

import kotlinx.serialization.json.Json

/** Shared JSON codec for persisting value objects as single columns. */
object JsonCodec {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
}
