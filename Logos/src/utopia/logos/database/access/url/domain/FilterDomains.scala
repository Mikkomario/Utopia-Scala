package utopia.logos.database.access.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.stored.url.Domain
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on domain properties
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
trait FilterDomains[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines domain database properties
	  */
	def model = DomainDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param url URL to target (may include the http part, but it is ignored)
	  * @return Copy of this access point that only includes domains with the specified URL
	  */
	def withUrl(url: String) = filter(model.url.column <=> Domain.removeHttp(url))
	/**
	  * @param urls Targeted URLs (may include the http parts, but these are ignored)
	  * @return Copy of this access point that only includes domains where url is within the specified value set
	  */
	def withUrls(urls: IterableOnce[String]) =
		filter(model.url.column.in(urls.iterator.map(Domain.removeHttp).toSet))
}

