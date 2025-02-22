package utopia.logos.database.access.many.url.domain

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.operator.enumeration.End.First
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple domains at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbDomains extends ManyDomainsAccess with UnconditionalView with ViewManyByIntIds[ManyDomainsAccess]
{
	// OTHER	--------------------
	
	/**
	  * Stores the specified domains in the DB. Avoids inserting duplicates.
	  * @param domains Domains to store
	  * @param connection Implicit DB connection
	  * @return Inserted domains (first) and existing domain entries (second).
	  * Contains separate groups for inserted items (first) and items that already existed in the DB (second)
	  */
	def store(domains: Set[String])(implicit connection: Connection) = {
		if (domains.nonEmpty) {
			val lowerCaseDomains = domains.map { _.toLowerCase }
			// Checks for existing entries
			val existing = matching(lowerCaseDomains).pull
			val existingDomainNames = existing.map { _.url.toLowerCase }.toSet
			// Inserts missing entries
			val inserted = model.insert(
				domains.filterNot { url => existingDomainNames.contains(url.toLowerCase) }
					.toVector.map { DomainData(_) }
			)
			
			// Returns the values in two groups: Inserted & existing
			Pair(inserted, existing)
		}
		else
			Pair.twice(Empty)
	}
	
	/**
	 * Stores the specified domains in the DB. Avoids inserting duplicates.
	 * @param values Values from which domains are extracted
	 * @param extractDomain A function that extracts a domain from a value
	 * @param merge A function that merges 3 values:
	 *                  1. The inserted domain
	 *                  1. The original value
	 *                  1. Whether the domain was inserted
	 * @param connection Implicit DB connection
	 * @tparam A Type of original values
	 * @tparam R Type of merge results
	 * @return Merge results
	 */
	def storeFrom[A, R](values: Iterable[A])(extractDomain: A => String)(merge: (Domain, A, Boolean) => R)
	                   (implicit connection: Connection) =
	{
		// Extracts the domain information
		val valuesWithDomains = values.map { v => v -> extractDomain(v) }
		// Stores the distinct domains and forms a map of the inserted data
		val domainMap = store(valuesWithDomains.view.map { _._2 }.toSet).zipWithSide
			.flatMap { case (domains, side) =>
				val wasInserted = side == First
				domains.map { d => d.url.toLowerCase -> (d, wasInserted) }
			}
			.toMap
		// Merges the inserted data with the original data
		valuesWithDomains.flatMap { case (value, domainStr) =>
			domainMap.get(domainStr.toLowerCase).map { case (domain, wasInserted) => merge(domain, value, wasInserted) }
		}
	}
}

