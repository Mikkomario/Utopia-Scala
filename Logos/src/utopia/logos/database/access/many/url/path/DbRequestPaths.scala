package utopia.logos.database.access.many.url.path

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair}
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.End.First
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.logos.database.access.many.url.domain.DbDomains
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.{Domain, RequestPath}
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

import scala.collection.View

/**
  * The root access point when targeting multiple request paths at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbRequestPaths 
	extends ManyRequestPathsAccess with UnconditionalView with ViewManyByIntIds[ManyRequestPathsAccess]
{
	/**
	  * Stores the specified request paths to the database.
	  * Avoids inserting duplicates
	  * @param paths Paths to store
	  * @param connection Implicit DB connection
	  * @return First inserted paths, then paths that already existed in the database.
	  * Both are grouped by their domain ids.
	  */
	def store(paths: Map[Int, Set[String]])(implicit connection: Connection) = {
		if (paths.nonEmpty) {
			// Finds existing paths under the specified domains
			val existingPerDomain = withinDomains(paths.keys).pull.groupBy { _.domainId }
			// .view.mapValues { _.map { p => p.path.toLowerCase -> p.id }.toMap }.toMap
			// Inserts missing paths (case-insensitive)
			val inserted = model.insert(paths.flatMap { case (domainId, paths) =>
				val existing = existingPerDomain.getOrElse(domainId, Empty)
				paths.distinctBy { _.toLowerCase }
					.filterNot { p => existing.exists { _.path ~== p } }
					.map { p => RequestPathData(domainId, p) }
			}.toVector)
			
			// Returns a domain-grouped list of request paths
			// Specifies separate groups for existing and inserted items
			Pair(inserted.groupBy { _.domainId }, existingPerDomain)
		}
		else
			Pair.twice(Map.empty[Int, Seq[RequestPath]])
	}
	
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
	                   (implicit connection: Connection) =
	{
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
		val insertedPaths = model.insertFrom(pathsToInsert) {
			case (domain, path, _) => RequestPathData(domain.id, path) } {
			case (path, (domain, _, value)) => (DetailedRequestPath(path, domain), value, true) }
		// Paths on existing domains must be stored more carefully
		val storedPathMap = store(
			pathsToStore.view.map { case (domain: Domain, path: String, _) => domain.id -> path }.toSet[(Int, String)]
				.groupMap { _._1 } { _._2 })
			.zipWithSide
			.map { case (paths, side) =>
				val wasInserted = side == First
				paths.view.mapValues { _.view.map { p => p.path.toLowerCase -> (p, wasInserted) }.toMap }.toMap
			}
			.merge { _.mergeWith(_) { _ ++ _ } }
		val storedPaths = pathsToStore.flatMap { case (domain, pathStr, value) =>
			storedPathMap.get(domain.id).flatMap { pathsMap =>
				pathsMap.get(pathStr.toLowerCase).map { case (path, wasInserted) =>
					(DetailedRequestPath(path, domain), value, wasInserted)
				}
			}
		}
		
		// Merges the inserted paths with the original values
		OptimizedIndexedSeq
			.from(View.concat(insertedPaths, storedPaths).map { case (p, v, inserted) => merge(p, v, inserted) })
	}
}