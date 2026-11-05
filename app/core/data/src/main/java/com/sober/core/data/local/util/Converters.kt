package com.sober.core.data.local.util

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class Converters {
    private val moshi = Moshi.Builder().build()
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val intListType = Types.newParameterizedType(List::class.java, Integer::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    private val intListAdapter = moshi.adapter<List<Int>>(intListType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { stringListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { raw ->
            // A single malformed value must not terminate the Flow that reads
            // every limit: Room would surface it as "no limits at all".
            runCatching { stringListAdapter.fromJson(raw) }.getOrNull() ?: emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { intListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return value?.let { raw ->
            runCatching { intListAdapter.fromJson(raw) }.getOrNull() ?: emptyList()
        }
    }
}
