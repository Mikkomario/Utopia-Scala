package utopia.logos.model.partial.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, StringType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.url.DomainFactory

import java.time.Instant

object DomainData extends FromModelFactoryWithSchema[DomainData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("url", StringType), PropertyDeclaration("created", 
			InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		DomainData(valid("url").getString, valid("created").getInstant)
}

/**
  * Represents the address of an internet service
  * @param url Full http(s) address of this domain in string format. Includes protocol, 
  * domain name and possible port number.
  * @param created Time when this domain was added to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DomainData(url: String, created: Instant = Now) 
	extends DomainFactory[DomainData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("url" -> url, "created" -> created))
	
	override def toString = url
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withUrl(url: String) = copy(url = url)
}

