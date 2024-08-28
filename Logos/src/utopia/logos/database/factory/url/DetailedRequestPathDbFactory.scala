package utopia.logos.database.factory.url

import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.logos.model.stored.url.{Domain, RequestPath}
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading detailed request paths from the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.3
  */
object DetailedRequestPathDbFactory extends CombiningFactory[DetailedRequestPath, RequestPath, Domain]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = DomainDbFactory
	override def parentFactory = RequestPathDbFactory
	
	/**
	  * @param requestPath request path to wrap
	  * @param domain domain to attach to this request path
	  */
	override def apply(requestPath: RequestPath, domain: Domain) = DetailedRequestPath(requestPath, domain)
}

