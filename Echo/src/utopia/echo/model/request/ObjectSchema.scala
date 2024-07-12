package utopia.echo.model.request

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.MaybeEmpty

object ObjectSchema
{
	/**
	  * A schema for an empty object
	  */
	val empty = apply(Empty)
}

/**
  * Describes a json object an LLM model should produce
  * @author Mikko Hilpinen
  * @since 11.07.2024, v0.1
  */
case class ObjectSchema(properties: Seq[PropertySchema]) extends MaybeEmpty[ObjectSchema]
{
	override def self: ObjectSchema = this
	override def isEmpty: Boolean = properties.isEmpty
	
	override def toString = s"{${ properties.mkString(", ") }"
}
