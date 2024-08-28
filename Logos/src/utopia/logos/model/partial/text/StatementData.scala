package utopia.logos.model.partial.text

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.text.StatementFactory

import java.time.Instant

object StatementData extends FromModelFactoryWithSchema[StatementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Pair(PropertyDeclaration("delimiterId", IntType, Single("delimiter_id"), 
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
  * @since 27.08.2024, v0.3
  */
case class StatementData(delimiterId: Option[Int] = None, created: Instant = Now) 
	extends StatementFactory[StatementData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Pair("delimiterId" -> delimiterId, "created" -> created))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withDelimiterId(delimiterId: Int) = copy(delimiterId = Some(delimiterId))
}

