package utopia.logos.model.enumeration

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.equality.EqualsExtensions._

/**
  * Represents a style chosen for displaying words, such as underlining or bold text. An open enumeration, 
  * meaning that sub-modules may introduce their own values.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
trait DisplayStyle extends ValueConvertible
{
	// ABSTRACT	--------------------
	
	/**
	  * id used to represent this display style in database and json
	  */
	def id: Int
	
	
	// IMPLEMENTED	--------------------
	
	override def toValue = id
}

object DisplayStyle
{
	// ATTRIBUTES	--------------------
	
	private var _values: Vector[DisplayStyle] = Vector(Default)
	
	
	// COMPUTED	--------------------
	
	/**
	  * All currently available display style values
	  */
	def values = _values
	
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
	  * Introduces a new possible value for this enumeration
	  * @param value New value to introduce
	  */
	def introduce(value: DisplayStyle) = {
		if (!_values.contains(value))
			_values :+= value
	}
	/**
	  * Introduces 0-n new values to this enumeration
	  * @param values New values to introduce
	  */
	def introcude(values: IterableOnce[DisplayStyle]) = {
		val newValues = Set.from(values) -- _values
		if (newValues.nonEmpty)
			_values ++= newValues
	}
	
	
	// NESTED	--------------------
	
	/**
	  * The default style with no modifications on how the text should be displayed.
	  * @since 20.03.2024
	  */
	case object Default extends DisplayStyle
	{
		// ATTRIBUTES	--------------------
		
		override val id = 1
	}
}

