package utopia.logos.model.cached

import utopia.flow.util.Mutate
import utopia.logos.model.enumeration.DisplayStyle

object WordOrLink
{
	/**
	 * @param word A word
	 * @return A word text element
	 */
	def word(word: String) = apply(word, isLink = false)
	/**
	 * @param url URL to wrap
	 * @return A link text element
	 */
	def link(url: String) = apply(url, isLink = true)
}

/**
 * Represents a word or a link in text format
 * @author Mikko Hilpinen
 * @since 11/03/2024, v0.2
 */
case class WordOrLink(text: String, isLink: Boolean)
{
	// ATTRIBUTES   --------------------
	
	lazy val (style, standardizedText) = if (isLink) DisplayStyle.default -> text else DisplayStyle.of(text)
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Whether this element represents a normal word and not a link
	 */
	def isWord = !isLink
	
	/**
	 * @return This element as a word. Empty string if this is a link.
	 */
	def word = if (isWord) text else ""
	/**
	 * @return This element as a link. Empty string if this is a word.
	 */
	def link = if (isLink) text else ""
	
	/**
	  * @return Either Left: a word or Right: a link
	  */
	def toEither = if (isLink) Right(text) else Left(text)
	
	
	// IMPLEMENTED  --------------------
	
	override def toString = text
	
	
	// OTHER    ------------------------
	
	/**
	 * @param f A text-mutating function
	 * @return A mutated copy of this text with the [[isLink]] state preserved
	 */
	def mapText(f: Mutate[String]) = copy(text = f(text))
}
