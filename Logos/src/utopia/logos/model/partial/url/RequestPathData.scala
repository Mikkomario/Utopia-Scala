package utopia.logos.model.partial.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, IntType, StringType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

import java.time.Instant

object RequestPathData extends FromModelFactoryWithSchema[RequestPathData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("domainId", IntType, Vector("domain_id")), 
			PropertyDeclaration("path", StringType, isOptional = true), PropertyDeclaration("created", 
			InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		RequestPathData(valid("domainId").getInt, valid("path").getString, valid("created").getInstant)
}

/**
  * Represents a specific http(s) request url, not including any query parameters
  * @param domainId Id of the domain part of this url
  * @param path Part of this url that comes after the domain part. Doesn't include any query parameters, 
  * nor the initial forward slash.
  * @param created Time when this request path was added to the database
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class RequestPathData(domainId: Int, path: String = "", created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("domainId" -> domainId, "path" -> path, "created" -> created))
}

