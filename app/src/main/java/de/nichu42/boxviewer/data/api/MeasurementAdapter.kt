package de.nichu42.boxviewer.data.api

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.JsonAdapter

class MeasurementAdapter : JsonAdapter<Measurement>() {
    private fun isHexId(str: String?): Boolean {
        if (str == null) return false
        return str.length == 24 && str.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }

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
            } else if (isHexId(str)) {
                Measurement(value = null, createdAt = null)
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
                    "value" -> {
                        val parsed = readNextAsString(reader)
                        value = if (isHexId(parsed)) null else parsed
                    }
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
