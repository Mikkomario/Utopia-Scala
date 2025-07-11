package utopia.logos.model.partial.text

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.text.WordFactory

import java.time.Instant

object WordData extends FromModelFactoryWithSchema[WordData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Pair(PropertyDeclaration("text", StringType), PropertyDeclaration("created", 
			InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		WordData(valid("text").getString, valid("created").getInstant)
}

/**
  * Represents an individual word used in a text document. Case-sensitive.
  * @param text Text representation of this word
  * @param created Time when this word was added to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class WordData(text: String, created: Instant = Now) extends WordFactory[WordData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Pair("text" -> text, "created" -> created))
	override def toString = text
	
	override def withCreated(created: Instant) = copy(created = created)
	override def withText(text: String) = copy(text = text)
}

