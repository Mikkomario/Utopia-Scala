package utopia.logos.model.partial.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, StringType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.word.DelimiterFactory

import java.time.Instant

@deprecated("Replaced with a new version", "v0.3")
object DelimiterData extends FromModelFactoryWithSchema[DelimiterData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("text", StringType), PropertyDeclaration("created", 
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
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class DelimiterData(text: String, created: Instant = Now) 
	extends DelimiterFactory[DelimiterData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("text" -> text, "created" -> created))
	
	override def toString = text
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withText(text: String) = copy(text = text)
}

