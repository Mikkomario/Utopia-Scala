package utopia.logos.model.partial.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, IntType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

import java.time.Instant

object StatementData extends FromModelFactoryWithSchema[StatementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("delimiterId", IntType, Vector("delimiter_id"), 
			isOptional = true), PropertyDeclaration("created", InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		StatementData(valid("delimiterId").int, valid("created").getInstant)
}

/**
  * Represents an individual statement made within some text. Consecutive statements form whole texts.
  * 
	@param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated 
  * with any character.
  * @param created Time when this statement was first made
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class StatementData(delimiterId: Option[Int] = None, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("delimiterId" -> delimiterId, "created" -> created))
}

