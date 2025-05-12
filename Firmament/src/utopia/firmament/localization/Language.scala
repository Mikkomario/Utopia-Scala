package utopia.firmament.localization

import utopia.flow.collection.immutable.Single
import utopia.flow.operator.{MaybeEmpty, ScopeUsable}
import utopia.flow.operator.equality.EqualsBy

import scala.language.implicitConversions

object Language
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A language instance that represents ambiguous, unspecified or inapplicable language.
	  */
	lazy val none = apply("")
	
	/**
	  * English language (en)
	  */
	lazy val english = apply("en")
	
	
	// IMPLICIT    -----------------------
	
	/**
	  * @param code A language code to wrap
	  * @return A language wrapping the specified code
	  */
	implicit def apply(code: String): Language = new _Language(code)
	
	
	// NESTED   --------------------------
	
	private class _Language(override val code: String) extends Language
}

/**
  * Common trait for instances that represent languages
  * @author Mikko Hilpinen
  * @since 11.05.2025, v1.5
  */
trait Language extends EqualsBy with MaybeEmpty[Language] with ScopeUsable[Language]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A code representing this language.
	  *         Usually defined as a lower-cased two-letter ISO 639-1 or a three-letter ISO 639-3 code.
	  *
	  *         Empty if this represents an unknown or general language,
	  *         which may be applicable for digits, symbols, etc.
	  *
	  * @see [[https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes List of ISO 639 language codes]]
	  */
	def code: String
	
	
	// IMPLEMENTED  --------------------
	
	override def self: Language = this
	
	override def isEmpty: Boolean = code.isEmpty
	
	override protected def equalsProperties: Seq[Any] = Single(code.toLowerCase)
	
	override def toString = code
	
	
	// OTHER    -----------------------
	
	/**
	  * @param string A string in this language
	  * @return A local string in this language
	  */
	def apply(string: String) = LocalString.in(this)(string)
}