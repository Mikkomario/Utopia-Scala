package utopia.flow.error

/**
 * An error thrown by some illegal regular expressions.
 * Wraps another exception, adding more helpful information for debugging.
 * @param regex The regular expression pattern that caused this error
 * @param input The input that triggered this error. May be truncated.
 * @param cause The underlying, wrapped error
 * @author Mikko Hilpinen
 * @since 06.03.2026, v2.8
 */
class RegexError(val regex: String, val input: String, val cause: Throwable)
	extends Error(s"Regex `$regex` failed on input: `${
		if (input.length > 512) s"${ input.take(250) } ... ${ input.takeRight(250) }" else input }`", cause)
