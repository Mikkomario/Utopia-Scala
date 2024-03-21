package utopia.logos.database.factory.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosTables
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading domain data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DomainDbFactory extends FromValidatedRowModelFactory[Domain]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = LogosTables.domain
	
	override protected def fromValidatedModel(valid: Model) = 
		Domain(valid("id").getInt, DomainData(valid("url").getString, valid("created").getInstant))
}

