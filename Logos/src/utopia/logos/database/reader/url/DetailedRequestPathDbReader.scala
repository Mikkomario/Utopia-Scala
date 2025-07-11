package utopia.logos.database.reader.url

import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.logos.model.stored.url.{Domain, RequestPath}
import utopia.vault.nosql.read.linked.CombiningDbRowReader

/**
  * Used for reading detailed request paths from the database
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object DetailedRequestPathDbReader 
	extends CombiningDbRowReader[RequestPath, Domain, DetailedRequestPath](RequestPathDbReader, DomainDbReader)
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param requestPath request path to wrap
	  * @param domain      domain to attach to this request path
	  */
	override def combine(requestPath: RequestPath, domain: Domain) = DetailedRequestPath(requestPath, domain)
}

