package utopia.logos.model.partial.url

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, IntType, ModelType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.logos.model.factory.url.LinkFactory

import java.time.Instant

object LinkData extends FromModelFactoryWithSchema[LinkData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = ModelDeclaration(Vector(
		PropertyDeclaration("pathId", IntType, Single("path_id")),
		PropertyDeclaration("queryParameters", ModelType, Single("query_parameters"), isOptional = true),
		PropertyDeclaration("created", InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) =
		LinkData(valid("pathId").getInt, valid("queryParameters").getModel, valid("created").getInstant)
}

/**
  * Represents a link for a specific http(s) request
  * @param pathId Id of the targeted internet address, including the specific sub-path
  * @param queryParameters Specified request parameters in model format
  * @param created Time when this link was added to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class LinkData(pathId: Int, queryParameters: Model = Model.empty, created: Instant = Now) 
	extends LinkFactory[LinkData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("pathId" -> pathId, "queryParameters" -> queryParameters, "created" -> created))
	
	override def withCreated(created: Instant) = copy(created = created)
	override def withPathId(pathId: Int) = copy(pathId = pathId)
	override def withQueryParameters(queryParameters: Model) = copy(queryParameters = queryParameters)
}

