package utopia.logos.model.enumeration

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.{OpenEnumeration, OpenEnumerationValue}

/**
  * Represents a style chosen for displaying words, such as underlining or bold text. An open enumeration, 
  * meaning that submodules may introduce their own values.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait DisplayStyle extends ValueConvertible with OpenEnumerationValue[Int]
{
	// ABSTRACT	--------------------
	
	/**
	  * id used to represent this display style in database and json
	  */
	def id: Int
	
	/**
	 * Applies this display style to the specified word
	 * @param word A word to style (in a standard format)
	 * @return A correctly styled copy of the specified word
	 */
	def apply(word: String): String
	
	
	// IMPLEMENTED	--------------------
	
	override def identifier: Int = id
	
	override def toValue = id
}

object DisplayStyle extends OpenEnumeration[DisplayStyle, Int]
{
	// ATTRIBUTES   ----------------
	
	private var identifyFunctions: Seq[String => Option[(DisplayStyle, String)]] = Empty
	
	
	// INITIAL CODE ----------------
	
	introduce(Vector(Default, Capitalized, AllCaps))
	
	
	// COMPUTED	--------------------
	
	/**
	  * The default display style (i.e. default)
	  */
	def default = Default
	
	
	// OTHER	--------------------
	
	/**
	  * @param id id representing a display style
	  * @return display style matching the specified id. None if the id didn't match any display style
	  */
	def findForId(id: Int) = values.find { _.id == id }
	/**
	  * @param value A value representing an display style id
	  * @return display style matching the specified value. None if the value didn't match any display style
	  */
	def findForValue(value: Value) = {
		value.castTo(IntType, StringType) match {
			case Left(idVal) => findForId(idVal.getInt)
			case Right(stringVal) =>
				val str = stringVal.getString
				values.find { _.toString ~== str }
		}
	}
	/**
	  * @param id id matching a display style
	  * @return display style matching that id, or the default display style (default)
	  */
	def forId(id: Int) = findForId(id).getOrElse(default)
	/**
	  * @param value A value representing an display style id
	  * @return display style matching the specified value, 
		when the value is interpreted as an display style id, 
	  * or the default display style (default)
	  */
	def fromValue(value: Value) = findForValue(value).getOrElse(default)
	
	/**
	 * @param word A word
	 * @return Display style of that word, plus a standard form version of that word
	 */
	def of(word: String) = identifyFunctions.findMap { _(word) }.getOrElse { _of(word) }
	/**
	 * @param word A word
	 * @return Display style of that word, from the 3 standard options,
	 *         plus a standardized version of the specified word
	 */
	private def _of(word: String): (DisplayStyle, String) = {
		val letters = word.filter { _.isLetter }
		if (letters.isEmpty)
			Default -> word
		else if (letters.forall { _.isUpper })
			AllCaps -> word.toLowerCase
		else if (letters.head.isUpper)
			Capitalized -> s"${ word.head.toLower }${ word.tail }"
		else
			Default -> word
	}
	
	/**
	 * Introduces logic for determining word display styles
	 * @param identifyStyle A function which accepts a word and yields it's style, plus a standardized version of it.
	 *                      Yields None in situations where other / default identify functions should be used instead.
	 */
	def addIdentifyLogic(identifyStyle: String => Option[(DisplayStyle, String)]) =
		identifyFunctions = identifyStyle +: identifyFunctions
	
	
	// NESTED	--------------------
	
	/**
	  * The default style with no modifications on how the text should be displayed.
	  * @since 20.03.2024
	  */
	case object Default extends DisplayStyle
	{
		// ATTRIBUTES	--------------------
		
		override val id = 1
		
		
		// IMPLEMENTED  --------------------
		
		override def apply(word: String): String = word
	}
	
	/**
	 * A display style where the starting letter is in upper-casing
	 */
	case object Capitalized extends DisplayStyle
	{
		// ATTRIBUTES   --------------------
		
		override val id: Int = 2
		
		
		// IMPLEMENTED  --------------------
		
		override def apply(word: String): String = word.capitalize
		
	}
	/**
	 * A display style where a word is written in upper-case letters only
	 */
	case object AllCaps extends DisplayStyle
	{
		// ATTRIBUTES   -------------------
		
		override val id: Int = 3
		
		
		// IMPLEMENTED  --------------------
		
		override def apply(word: String): String = word.toUpperCase
	}
}

