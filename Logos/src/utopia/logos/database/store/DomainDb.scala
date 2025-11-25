package utopia.logos.database.store

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.CertainBoolean
import utopia.logos.database.CachingVolatileMapStore
import utopia.logos.database.access.url.domain.AccessDomains
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.database.{Connection, References}
import utopia.vault.sql.{OrderBy, Update, Where}

/**
 * An interface for interacting with domain information in the DB
 * @author Mikko Hilpinen
 * @since 25.11.2025, v0.6.1
 */
object DomainDb extends CachingVolatileMapStore[String, String, Domain]
{
	// ATTRIBUTES   -------------------------
	
	private val access = AccessDomains.root
	private val model = DomainDbModel
	
	
	// IMPLEMENTED  -------------------------
	
	// Removes the http:// and https://, and maps the remainder in lower-case
	override protected def standardize(value: String): String = removeHttp(value).toLowerCase
	
	override protected def diff(proposed: Set[String], existing: Set[String]): Set[String] =
		proposed.filterNot { p => existing.contains(standardize(p)) }
	
	override protected def pullMatchMap(values: Set[String])(implicit connection: Connection): Map[String, Domain] =
		access.withUrls(values).toMapBy { _.url.toLowerCase }
	
	override protected def insertAndMap(values: Seq[String])(implicit connection: Connection): Map[String, Domain] =
		model.insertFrom(values.map { d =>
			if (d.startsWith("http")) {
				val isHttps = d.lift(4).contains('s')
				removeHttp(d) -> CertainBoolean(isHttps)
			}
			else
				d -> UncertainBoolean
		}) { case (url, isHttps) => DomainData(url, isHttps = isHttps) } {
			case (inserted, (url, _)) => url.toLowerCase -> inserted }.toMap
		
	
	// OTHER    ------------------------------
	
	/**
	 * Cleans all domain entries, which include http:// or https:// in their URLs in the DB.
	 * This function should be called when updating from Logos v0.6 to v0.7
	 * @param connection Implicit DB connection
	 */
	def cleanHttpPrefixes()(implicit connection: Connection) = {
		// Updates all domain entries that include the protocol in their URL to no longer do so
		access.filter(model.url.like("http%")).stream { domainsIter =>
			domainsIter.foreach { domain =>
				val isHttps = domain.url.lift(4).contains('s')
				model.withId(domain.id).withUrl(removeHttp(domain.url)).withIsHttps(isHttps).update()
			}
		}
		// Removes all duplicate entries from the DB
		lazy val referencingColumns = References.to(model.id).map { _.from }
		access.withOrdering(OrderBy.ascending(model.url)).stream { domainsIter =>
			domainsIter.groupConsecutiveBy { _.url }.filter { _._2.hasSize > 1 }
				.foreach { case (_, identicalDomains) =>
					val primary = identicalDomains.head
					val duplicates = identicalDomains.tail
					
					// Determines the combined isHttp -value
					val areHttps = duplicates.foldLeft(primary.isHttps) { (areHttps, domain) =>
						areHttps.exact match {
							case Some(areHttps) =>
								if (domain.isHttps.isCertainly(areHttps))
									CertainBoolean(areHttps)
								else
									UncertainBoolean
							
							case None => areHttps
						}
					}
					// Updates the primary entry, if necessary
					if (primary.isHttps != areHttps)
						primary.access.isHttps.set(areHttps)
						
					// Replaces all references to the duplicate entries
					val duplicateIds = duplicates.view.map { _.id }.toIntSet
					referencingColumns.foreach { col =>
						(Update(col, primary.id) + Where(col.in(duplicateIds))).execute()
					}
					// Deletes the duplicate entries
					access(duplicateIds).delete()
				}
		}
	}
	
	private def removeHttp(url: String) = Domain.httpRegex.endIndexIteratorIn(url).nextOption() match {
		case Some(startIndex) => url.drop(startIndex)
		case None => url
	}
}
