package org.jetbrains.realworld

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime

typealias OffsetDateTimeAsString = @Serializable(OffsetDateTimeAsStringSerializer::class) OffsetDateTime

object OffsetDateTimeAsStringSerializer : KSerializer<OffsetDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = OffsetDateTime.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: OffsetDateTime) = encoder.encodeString(value.toString())
}
