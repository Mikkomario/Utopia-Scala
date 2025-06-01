package utopia.logos.database.access.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.DomainDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on domain properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterDomains[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines domain database properties
	  */
	def domainModel = DomainDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param url url to target
	  * @return Copy of this access point that only includes domains with the specified url
	  */
	def withUrl(url: String) = filter(domainModel.url.column <=> url)
	
	/**
	  * @param urls Targeted urls
	  * @return Copy of this access point that only includes domains where url is within the specified value 
	  * set
	  */
	def withUrls(urls: Iterable[String]) = filter(domainModel.url.column.in(urls))
}

