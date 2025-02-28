package utopia.logos.model.cached

import utopia.flow.operator.MaybeEmpty
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.Mutate
import utopia.logos.model.enumeration.DisplayStyle

object WordOrLink
{
	/**
	 * @param word A word
	 * @return A word text element
	 */
	def word(word: String) = apply(Left(word))
	/**
	 * @param url URL to wrap
	 * @return A link text element. None if that url was not a link
	 */
	def link(url: String): Option[WordOrLink] = Link(url).map(link)
	/**
	 * @param link Link to wrap
	 * @return A link text element
	 */
	def link(link: Link) = apply(Right(link))
}

/**
 * Represents a word or a link in text format
 * @author Mikko Hilpinen
 * @since 11/03/2024, v0.2
 */
case class WordOrLink(data: Either[String, Link]) extends MaybeEmpty[WordOrLink]
{
	// ATTRIBUTES   --------------------
	
	lazy val (style, standardizedText) = data match {
		case Left(word) => DisplayStyle.of(word)
		case Right(link) => DisplayStyle.default -> link.toString
	}
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Whether this element represents a normal word and not a link
	 */
	def isWord = data.isLeft
	/**
	 * @return Whether this element represents a link
	 */
	def isLink = data.isRight
	
	/**
	 * @return This element as a word. Empty string if this is a link.
	 */
	def word = data match {
		case Left(word) => word
		case Right(_) => ""
	}
	/**
	 * @return This element as a link. None if this is a word.
	 */
	def link = data.toOption
	
	/**
	  * @return Either Left: a word or Right: a link
	  */
	def toEither = data
	
	
	// IMPLEMENTED  --------------------
	
	override def self: WordOrLink = this
	override def isEmpty: Boolean = data.leftOption.exists { _.isEmpty }
	
	override def toString = data match {
		case Left(word) => word
		case Right(link) => link.toString
	}
	
	
	// OTHER    ------------------------
	
	/**
	 * @param f A text-mutating function
	 * @return A mutated copy of this text with the [[isLink]] state preserved.
	 *         None if link format became malformed.
	 */
	def mapText(f: Mutate[String]) = data match {
		case Left(word) => Some(WordOrLink.word(f(word)))
		case Right(link) => WordOrLink.link(f(link.toString))
	}
	/**
	 * @param f A text-mutating function
	 * @return If this is a word, returns a mutated copy of this word. If this is a link, returns self.
	 */
	def mapIfWord(f: Mutate[String]) = data match {
		case Left(word) => WordOrLink.word(f(word))
		case Right(_) => this
	}
}
