package utopia.flow.generic.model.template

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.json.JsonConvertible
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.View

/**
  * Properties are named and contain a value in a certain data type
  * @author Mikko Hilpinen
  * @since 26.11.2016
  */
trait Property extends JsonConvertible with View[Value]
{
	// ABSTRACT    ---------------
	
	/**
	  * The name of the property
	  */
	def name: String
	/**
	  * The data type of this property and its contents
	  */
	def dataType: DataType
	
	
	// COMPUTED -----------
	
	/**
	  * @return Whether this property has no value
	  */
	def isEmpty = value.isEmpty
	/**
	  * @return Whether this property has a non-empty value
	  */
	def nonEmpty = value.isDefined
	
	
	// IMPLEMENTED    -----
	
	override def toString = s"$name: ${ value.description }"
	
	override def appendToJson(jsonBuilder: StringBuilder) = {
		jsonBuilder ++= name.quoted
		jsonBuilder ++= ": "
		value.appendToJson(jsonBuilder)
	}
}
