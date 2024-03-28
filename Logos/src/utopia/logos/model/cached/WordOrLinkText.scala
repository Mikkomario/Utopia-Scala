package utopia.logos.model.cached

import utopia.flow.util.Mutate

object WordOrLinkText
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
 * @since 11/03/2024, v1.0
 */
case class WordOrLinkText(text: String, isLink: Boolean)
{
	// COMPUTED ------------------------
	
	/**
	 * @return Whether this element represents a normal word and not a link
	 */
	def isWord = !isLink
	
	/**
	 * @return This element as a word. None if this is a link.
	 */
	def word = if (isWord) Some(text) else None
	/**
	 * @return This element as a link. None if this is a word.
	 */
	def link = if (isLink) Some(text) else None
	
	
	// IMPLEMENTED  --------------------
	
	override def toString = text
	
	
	// OTHER    ------------------------
	
	/**
	 * @param f A text-mutating function
	 * @return A mutated copy of this text with the [[isLink]] state preserved
	 */
	def mapText(f: Mutate[String]) = copy(text = f(text))
}
