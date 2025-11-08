package utopia.access.model

import utopia.access.model.ContentType._any
import utopia.access.model.enumeration.ContentCategory
import utopia.flow.operator.equality.{ApproxSelfEquals, EqualsFunction}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.StringExtensions._

import java.net.URLConnection
import java.nio.charset.{Charset, StandardCharsets}
import scala.io.Codec
import scala.util.Try

object ContentType
{
	// ATTRIBUTES   -------------------------
	
	private val _any = "*"
	
	/**
	 * @return An equals function which yields true as long as the two types point to the same category & subtype.
	 *         E.g. text/plain with some charset would match text/plain without a charset,
	 *         but not text/html or application/json.
	 */
	implicit val haveSameType: EqualsFunction[ContentType] =
		(a, b) => a.category == b.category && (a.subType ~== b.subType)
	
	
	// OTHER METHODS    ---------------------
	
	/**
	  * Parses a string into a content type. Returns None if the content type couldn't be parsed.
	  * Only includes parameters which have a specified value
	  */
	def parse(typeString: String) = {
		val (category, afterCategory) = typeString.splitAtFirst("/").toTuple
		if (afterCategory.isEmpty)
			None
		else {
			val subTypeAndParams = afterCategory.split(";")
			val params = {
				if (subTypeAndParams.length < 2)
					Map[String, String]()
				else
					subTypeAndParams.view.tail.map { _.splitAtFirst("=").toTuple }.toMap
			}
			Some(apply(ContentCategory(category), subTypeAndParams.head, params))
		}
	}
	
	/**
	  * Guesses a content type from a fileName
	  */
	def guessFrom(fileName: String) = {
		val cType = URLConnection.guessContentTypeFromName(fileName)
		if (cType == null) None else parse(cType)
	}
}

/**
  * A content type is used for describing requested / returned content transferred over http
  * @param category The category in which this content type belongs (e.g. "application" or "image")
  * @param subType The name of this specific content type within 'category' (e.g. "json" or "png")
  * @param parameters Additional parameters, such as the character set used
  * @author Mikko Hilpinen
  * @since 20.8.2017
  */
// TODO: Parameters should be a Model
// TODO: Add support & modeling for * subtype
case class ContentType(category: ContentCategory, subType: String, parameters: Map[String, String] = Map())
	extends ApproxSelfEquals[ContentType]
{
	// ATTRIBUTES   --------------------
	
	/**
	  * The character set specified in this content type
	  */
	// TODO: Add support for case-insensitivity
	// TODO: Also, add support for default character sets (?)
	lazy val charset = parameters.get("charset").flatMap { c => Try { Charset.forName(c) }.toOption }
	
	
	// COMPUTED --------------------
	
	/**
	 * @return The character-set specified in this content type, or UTF-8
	 */
	def charsetOrUtf8 = charset.getOrElse(StandardCharsets.UTF_8)
	
	/**
	 * @param codec Implicit character encoding information to use by default
	 *              (i.e. when no other character set has been explicitly set)
	 * @return A copy of this content type with a character set value specified
	 */
	def withCharsetSpecified(implicit codec: Codec) = withDefaultCharset(codec.charSet)
	
	
	// IMPLEMENTED    --------------
	
	override def self: ContentType = this
	override def approxEqualsFunction: EqualsFunction[ContentType] = ContentType.haveSameType
	
	override def toString = {
		val parametersPart = parameters.view.map { case (key, value) => s";$key=$value" }.mkString
		s"$category/$subType$parametersPart"
	}
	
	
	// OTHER    -----------------------
	
	/**
	 * @param other Another content type
	 * @return Whether this content type covers the specified type.
	 *         Doesn't consider parameter differences.
	 */
	def contains(other: ContentType) =
		category == other.category && (subType == _any || (subType ~== other.subType))
	/**
	 * @param other Another content type
	 * @return Whether this content type covers the specified type, or the specified type covers this type.
	 *         Doesn't consider parameter differences.
	 */
	def overlapsWith(other: ContentType) =
		category == other.category && ((subType ~== other.subType) || subType == _any || other.subType == _any)
	
	/**
	  * Creates a new content type with the assigned parameter
	  */
	def +(param: (String, String)) = copy(parameters = parameters + param)
	/**
	  * Creates a new content type with the assigned parameters
	  */
	def ++(parameters: IterableOnce[(String, String)]) = copy(parameters = this.parameters ++ parameters)
	
	/**
	  * @param charset Character set to assign
	  * @return Copy of this content type with the specified character set
	  */
	def withCharset(charset: Charset) = this + ("charset" -> charset.name())
	/**
	 * @param charset Character set to specify, unless one has already been specified (call-by-name)
	 * @return A copy of this content type with either the current character set,
	 *         or the specified character set, if no character set was specified on this instance.
	 */
	def withDefaultCharset(charset: => Charset) =
		if (this.charset.isDefined) this else withCharset(charset)
}