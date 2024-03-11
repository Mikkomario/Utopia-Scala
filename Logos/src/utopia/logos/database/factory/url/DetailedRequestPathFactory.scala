package utopia.logos.database.factory.url

import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.logos.model.stored.url.{Domain, RequestPath}

/**
  * Used for reading detailed request paths from the database
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DetailedRequestPathFactory extends CombiningFactory[DetailedRequestPath, RequestPath, Domain]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = DomainFactory
	
	override def parentFactory = RequestPathFactory
	
	override def apply(requestPath: RequestPath, domain: Domain) = DetailedRequestPath(requestPath, domain)
}

