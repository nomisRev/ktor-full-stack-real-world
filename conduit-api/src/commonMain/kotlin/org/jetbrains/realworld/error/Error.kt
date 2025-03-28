package org.jetbrains.realworld.error

import kotlinx.serialization.Serializable

@Serializable data class GenericErrorModel(val errors: GenericErrorModelErrors) {
    constructor(message: String): this(GenericErrorModelErrors(listOf(message)))
    constructor(errors: List<String>): this(GenericErrorModelErrors(errors))

    fun message(): String =
        errors.body.joinToString(separator = "; ")
}

@Serializable data class GenericErrorModelErrors(val body: List<String>)