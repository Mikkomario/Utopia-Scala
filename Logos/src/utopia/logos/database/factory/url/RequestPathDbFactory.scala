package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosTables
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading request path data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object RequestPathDbFactory extends FromValidatedRowModelFactory[RequestPath]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = LogosTables.requestPath
	
	override protected def fromValidatedModel(valid: Model) = 
		RequestPath(valid("id").getInt, RequestPathData(valid("domainId").getInt, valid("path").getString, 
			valid("created").getInstant))
}

