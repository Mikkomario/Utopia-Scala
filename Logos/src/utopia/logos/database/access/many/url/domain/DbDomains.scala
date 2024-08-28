package utopia.logos.database.access.many.url.domain

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.logos.model.partial.url.DomainData
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
}

