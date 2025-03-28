package org.jetbrains.realworld.error

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val errors: Map<String, List<String>>) {
    constructor(field: String, message: String) : this(mapOf(field to listOf(message)))

    fun message(): String = buildString {
        errors.values.joinTo(this, separator = "; ") { it.joinTo(this) }
    }
}