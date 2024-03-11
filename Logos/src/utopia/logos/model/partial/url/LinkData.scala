package utopia.logos.model.partial.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{InstantType, IntType, ModelType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

import java.time.Instant

object LinkData extends FromModelFactoryWithSchema[LinkData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("requestPathId", IntType, Vector("request_path_id")), 
			PropertyDeclaration("queryParameters", ModelType, Vector("query_parameters"), isOptional = true), 
			PropertyDeclaration("created", InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		LinkData(valid("requestPathId").getInt, valid("queryParameters").getModel, 
			valid("created").getInstant)
}

/**
  * Represents a link for a specific http(s) request
  * @param requestPathId Id of the targeted internet address, including the specific sub-path
  * @param queryParameters Specified request parameters in model format
  * @param created Time when this link was added to the database
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class LinkData(requestPathId: Int, queryParameters: Model = Model.empty, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("requestPathId" -> requestPathId, "queryParameters" -> queryParameters, 
			"created" -> created))
}

