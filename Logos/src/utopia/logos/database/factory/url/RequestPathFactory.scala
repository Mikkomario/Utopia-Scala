package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.EmissaryTables
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath

/**
  * Used for reading request path data from the DB
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object RequestPathFactory extends FromValidatedRowModelFactory[RequestPath]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = EmissaryTables.requestPath
	
	override protected def fromValidatedModel(valid: Model) = 
		RequestPath(valid("id").getInt, RequestPathData(valid("domainId").getInt, valid("path").getString, 
			valid("created").getInstant))
}

