package io.github.t45k.bgJobTrial

/**
 * @param args all of them must be serializable
 */
internal data class Job(
    val classFqn: String,
    val methodName: String,
    val args: List<Any?>,
)
