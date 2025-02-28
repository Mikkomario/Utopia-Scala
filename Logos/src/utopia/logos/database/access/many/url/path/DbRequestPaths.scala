package utopia.logos.database.access.many.url.path

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.logging.Logger
import utopia.logos.database.CachingVolatileMapStore
import utopia.logos.database.access.many.url.domain.DbDomains
import utopia.logos.model.cached.Link
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.Domain
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple request paths at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbRequestPaths 
	extends ManyRequestPathsAccess with UnconditionalView with ViewManyByIntIds[ManyRequestPathsAccess]
{
	// ATTRIBUTES   -----------------------
	
	private val storeLock = new AnyRef
	
	
	// OTHER    ---------------------------
	
	/**
	 * Stores the request paths from the specified links
	 * @param links Links from which request paths are to be stored
	 * @param connection Implicit DB connection
	 * @param log Implicit logging implementation for logging warnings about mapping failures
	 * @return Stored links, where each entry contains 3 values:
	 *              1. Stored request path, including domain information
	 *              1. Specified link
	 *              1. Whether this represents a newly inserted request path
	 */
	def storeFromLinks(links: Iterable[Link])(implicit connection: Connection, log: Logger) =
		storeFrom(links) { _.domain } { _.path } { (path, link, wasInserted) => (path, link, wasInserted) }
	/**
	 * Stores a group of request paths
	 * @param values Values from which request path data is extracted
	 * @param extractDomain A function which extracts a domain string from a value
	 * @param extractPath A function which extracts a request path string from a value.
	 *                    Note: These must not include the domain part.
	 * @param merge A function that merges the following 3 values:
	 *                  1. A request path, including domain information
	 *                  1. Original value
	 *                  1. Whether the path was inserted to the database
	 * @param connection Implicit DB connection
	 * @tparam A Type of original values
	 * @tparam R Type of merge results
	 * @return Merge results
	 */
	def storeFrom[A, R](values: Iterable[A])
	                   (extractDomain: A => String)(extractPath: A => String)
	                   (merge: (DetailedRequestPath, A, Boolean) => R)
	                   (implicit connection: Connection, log: Logger) =
	{
		if (values.isEmpty)
			Empty
		else
			storeLock.synchronized {
				// Stores the domains first
				val domainValues = DbDomains
					.storeFrom(values)(extractDomain) { (domain, value, wasInserted) => (domain, value, wasInserted) }
				
				// Next, stores the request paths
				val (pathsToInsert, pathsToStore) = domainValues
					.divideWith { case (domain, value, wasInserted) =>
						val path = extractPath(value)
						if (wasInserted) Left((domain, path, value)) else Right((domain, path, value))
					}
				// Paths on new domains may be inserted without duplicate checks
				val insertResults = model.insertFrom(pathsToInsert) {
					case (domain, path, _) => RequestPathData(domain.id, path) } {
					case (path, (domain, _, value)) => merge(DetailedRequestPath(path, domain), value, true) }
				// Paths on existing domains must be stored more carefully
				val storeResults = Store.storeFrom(pathsToStore) { case (domain, path, _) => domain -> path } {
					case (path, (_, _, original), wasInserted) => merge(path, original, wasInserted) }
				
				insertResults ++ storeResults
			}
	}
	/**
	  * Stores the specified request paths to the database.
	  * Avoids inserting duplicates
	  * @param paths Paths to store. Each entry consists of a domain id, plus the path part as a string.
	  * @param connection Implicit DB connection
	  * @return A map (view) which contains the specified paths as keys and detailed request paths as values.
	 *         Each value is coupled with a boolean that's set to true if it represents a newly inserted entity.
	  */
	def store(paths: Set[(Domain, String)])(implicit connection: Connection) =
		Store.store(paths)
	
	
	// NESTED   ----------------------------
	
	private object Store extends CachingVolatileMapStore[(Domain, String), (Int, String), DetailedRequestPath]
	{
		override protected def standardize(value: (Domain, String)): (Int, String) = value._1.id -> value._2.toLowerCase
		override protected def diff(proposed: Set[(Domain, String)], existing: Set[(Int, String)]): Set[(Domain, String)] = {
			val existingByDomain = existing.groupMap { _._1 } { _._2 }
			proposed.filterNot { case (domain, path) =>
				existingByDomain.get(domain.id).exists { _.contains(path.toLowerCase) }
			}
		}
		
		override protected def pullMatchMap(values: Set[(Domain, String)])
		                                   (implicit connection: Connection): Map[(Int, String), DetailedRequestPath] =
		{
			val domainMap = values.view.map { case (domain, _) => domain.id -> domain }.toMap
			withinDomains(values.map { _._1.id }).pull.view
				.map { p =>
					val domain = domainMap(p.domainId)
					(domain.id -> p.path.toLowerCase) -> domain(p)
				}
				.toMap
		}
		
		override protected def insertAndMap(values: Seq[(Domain, String)])
		                                   (implicit connection: Connection): Map[(Int, String), DetailedRequestPath] =
		{
			// Inserts the request paths and forms the final map
			model.insertFrom(values) { case (domain, path) => RequestPathData(domain.id, path) } {
				case (path, (domain, pathStr)) => (domain.id -> pathStr.toLowerCase) -> domain(path) }
				.toMap
		}
	}
}