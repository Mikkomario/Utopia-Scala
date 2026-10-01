package utopia.flow.parse.string

import utopia.flow.parse.BufferInput

/**
 * Common trait for interfaces that read and buffer from streamed sources.
 * @tparam I The type of accepted preprocessed input
 * @tparam A The type of buffered output
 * @author Mikko Hilpinen
 * @since 28.09.2025, v2.7
 */
@deprecated("Deprecated for removal. Replaced with BufferInput.", "v2.9")
trait FromSource[I, +A] extends BufferInput[I, A]