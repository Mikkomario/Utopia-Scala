package utopia.logos.model.partial.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, IntType, StringType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.url.RequestPathFactory

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
  * @since 20.03.2024, v0.2
  */
case class RequestPathData(domainId: Int, path: String = "", created: Instant = Now) 
	extends RequestPathFactory[RequestPathData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("domainId" -> domainId, "path" -> path, "created" -> created))
	
	override def withCreated(created: Instant) = copy(created = created)
	override def withDomainId(domainId: Int) = copy(domainId = domainId)
	override def withPath(path: String) = copy(path = path)
}

