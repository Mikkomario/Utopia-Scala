package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.logos.database.EmissaryTables
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain

/**
  * Used for reading domain data from the DB
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DomainFactory extends FromValidatedRowModelFactory[Domain]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = EmissaryTables.domain
	
	override protected def fromValidatedModel(valid: Model) = 
		Domain(valid("id").getInt, DomainData(valid("url").getString, valid("created").getInstant))
}

