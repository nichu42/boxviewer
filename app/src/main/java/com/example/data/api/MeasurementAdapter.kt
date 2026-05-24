package com.example.data.api

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.JsonAdapter

class MeasurementAdapter : JsonAdapter<Measurement>() {
    override fun fromJson(reader: JsonReader): Measurement? {
        val peeked = reader.peek()
        if (peeked == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        if (peeked == JsonReader.Token.STRING) {
            val str = reader.nextString()
            // Return safe Measurement with string value or timestamp
            return if (str.contains("T") && str.contains("Z")) {
                Measurement(value = null, createdAt = str)
            } else {
                Measurement(value = str, createdAt = null)
            }
        }
        if (peeked == JsonReader.Token.NUMBER) {
            val numStr = readNextAsString(reader)
            return Measurement(value = numStr, createdAt = null)
        }
        if (peeked == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            var value: String? = null
            var createdAt: String? = null
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (reader.peek() == JsonReader.Token.NULL) {
                    reader.skipValue()
                    continue
                }
                when (name) {
                    "value" -> value = readNextAsString(reader)
                    "createdAt" -> createdAt = readNextAsString(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return Measurement(value = value, createdAt = createdAt)
        }
        reader.skipValue()
        return null
    }

    private fun readNextAsString(reader: JsonReader): String? {
        val peek = reader.peek()
        return when (peek) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                val d = reader.nextDouble()
                if (d == d.toLong().toDouble()) {
                    d.toLong().toString()
                } else {
                    d.toString()
                }
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            JsonReader.Token.NULL -> {
                reader.skipValue()
                null
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: Measurement?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("value").value(value.value)
        writer.name("createdAt").value(value.createdAt)
        writer.endObject()
    }
}
