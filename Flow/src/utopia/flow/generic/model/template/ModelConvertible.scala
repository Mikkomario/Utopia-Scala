package utopia.flow.generic.model.template

import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonConvertible

/**
  * ModelConvertible instances can be represented as model data when necessary
  * @author Mikko Hilpinen
  * @since 23.6.2017
  */
trait ModelConvertible extends JsonConvertible with ValueConvertible
{
	// ABSTRACT METHODS & PROPERTIES    -----------------
	
	/**
	  * A model representation of this instance
	  */
	def toModel: Model
	
	
	// IMPLEMENTED    ---------------------------
	
	override implicit def toValue: Value = toModel
	
	override def toJson = toModel.toJson
	
	override def toString = toModel.toString
	
	override def appendToJson(jsonBuilder: StringBuilder) = toModel.appendToJson(jsonBuilder)
}
