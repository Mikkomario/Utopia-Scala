package utopia.logos.model.partial.url

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.BooleanType
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.flow.util.UncertainBoolean
import utopia.logos.model.factory.url.DomainFactory

import java.time.Instant

object DomainData extends FromModelFactoryWithSchema[DomainData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("url", StringType), PropertyDeclaration("created", 
			InstantType, isOptional = true), PropertyDeclaration("isHttps", BooleanType, Single("is_https"), 
			isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		DomainData(valid("url").getString, valid("created").getInstant, 
			UncertainBoolean(valid("isHttps").boolean))
}

/**
  * Represents the address of an internet service
  * @param url     This domain as a string, excluding the protocol part. May include a port 
  *                number.
  * @param created Time when this domain was added to the database
  * @param isHttps Whether to connect using HTTPS instead of HTTP. Uncertain if both forms have 
  *                been encountered.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DomainData(url: String, created: Instant = Now, isHttps: UncertainBoolean = UncertainBoolean) 
	extends DomainFactory[DomainData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("url" -> url, "created" -> created, "isHttps" -> isHttps.exact))
	
	override def toString = {
		val httpPart = if (isHttps.mayBeTrue) "https://" else "http://"
		s"$httpPart$url"
	}
	
	override def withCreated(created: Instant) = copy(created = created)
	override def withIsHttps(isHttps: UncertainBoolean) = copy(isHttps = isHttps)
	override def withUrl(url: String) = copy(url = url)
}

