package com.aistudio.futureagent.agxjyz.data.room

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, Float::class.javaObjectType)
    private val adapter = moshi.adapter<List<Float>>(type)

    @TypeConverter
    fun fromFloatList(value: List<Float>): String {
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toFloatList(value: String): List<Float> {
        return adapter.fromJson(value) ?: emptyList()
    }
}
