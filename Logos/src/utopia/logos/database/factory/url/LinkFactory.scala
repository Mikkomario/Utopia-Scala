package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.{EmissaryTables, LogosContext}
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.Link

/**
  * Used for reading link data from the DB
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object LinkFactory extends FromValidatedRowModelFactory[Link]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = EmissaryTables.link
	
	override protected def fromValidatedModel(valid: Model) = 
		Link(valid("id").getInt, LinkData(valid("requestPathId").getInt, 
			valid("queryParameters").notEmpty match {
				case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel
				case None => Model.empty
			},
			valid("created").getInstant))
}

