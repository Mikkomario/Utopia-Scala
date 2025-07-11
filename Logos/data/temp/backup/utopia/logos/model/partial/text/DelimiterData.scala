package utopia.logos.model.partial.text

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.text.DelimiterFactory

import java.time.Instant

object DelimiterData extends FromModelFactoryWithSchema[DelimiterData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Pair(PropertyDeclaration("text", StringType), PropertyDeclaration("created", 
			InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		DelimiterData(valid("text").getString, valid("created").getInstant)
}

/**
  * Represents a character sequence used to separate two statements or parts of a statement
  * @param text The characters that form this delimiter
  * @param created Time when this delimiter was added to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DelimiterData(text: String, created: Instant = Now) 
	extends DelimiterFactory[DelimiterData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Pair("text" -> text, "created" -> created))
	override def toString = text
	
	override def withCreated(created: Instant) = copy(created = created)
	override def withText(text: String) = copy(text = text)
}

