package utopia.logos.database.access.many.url.request_path

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.view.UnconditionalView
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath

/**
  * The root access point when targeting multiple request paths at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, v0.1
  */
object DbRequestPaths extends ManyRequestPathsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted request paths
	  * @return An access point to request paths with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbRequestPathsSubset(ids)
	
	/**
	 * Stores the specified request paths to the database.
	 * Avoids inserting duplicates
	 * @param paths Paths to store
	 * @param connection Implicit DB connection
	 * @return First inserted paths, then paths that already existed in the database.
	 *         Both are grouped by their domain ids.
	 */
	def store(paths: Map[Int, Set[String]])(implicit connection: Connection) = {
		if (paths.nonEmpty) {
			// Finds existing paths under the specified domains
			val existingPerDomain = withinDomains(paths.keys).pull.groupBy { _.domainId }
				// .view.mapValues { _.map { p => p.path.toLowerCase -> p.id }.toMap }.toMap
			// Inserts missing paths (case-insensitive)
			val inserted = model.insert(paths.flatMap { case (domainId, paths) =>
				val existing = existingPerDomain.getOrElse(domainId, Vector.empty)
				paths.distinctBy { _.toLowerCase }
					.filterNot { p => existing.exists { _.path ~== p } }
					.map { p => RequestPathData(domainId, p) }
			}.toVector)
			
			// Returns a domain-grouped list of request paths
			// Specifies separate groups for existing and inserted items
			Pair(inserted.groupBy { _.domainId }, existingPerDomain)
		}
		else
			Pair.twice(Map.empty[Int, Vector[RequestPath]])
	}
	
	
	// NESTED	--------------------
	
	class DbRequestPathsSubset(targetIds: Set[Int]) extends ManyRequestPathsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

