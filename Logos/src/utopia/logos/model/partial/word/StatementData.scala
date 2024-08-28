package utopia.logos.model.partial.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, IntType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.word.StatementFactory

import java.time.Instant

@deprecated("Replaced with a new version", "v0.3")
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
  * @param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated
  * with any character.
  * @param created Time when this statement was first made
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class StatementData(delimiterId: Option[Int] = None, created: Instant = Now) 
	extends StatementFactory[StatementData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("delimiterId" -> delimiterId, "created" -> created))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withDelimiterId(delimiterId: Int) = copy(delimiterId = Some(delimiterId))
}

