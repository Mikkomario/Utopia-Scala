package utopia.logos.database.access.many.url.link

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.logging.Logger
import utopia.logos.database.access.many.url.path.DbRequestPaths
import utopia.logos.model.cached.Link
import utopia.logos.model.combined.url.DetailedLink
import utopia.logos.model.partial.url.LinkData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

import scala.collection.View

/**
  * The root access point when targeting multiple links at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbLinks extends ManyLinksAccess with UnconditionalView with ViewManyByIntIds[ManyLinksAccess]
{
	// ATTRIBUTES	--------------------
	
	private val maximumModelLength = 2048
	private val storeLock = new AnyRef
	
	
	// OTHER	--------------------
	
	/**
	  * Stores all the specified links to the database.
	  * Avoids inserting duplicate information.
	  * @param links Links to store
	  * @param connection Implicit DB connection
	  * @return A map where the specified links are keys and values are the stored links,
	 *         which include domain and request path information.
	 *         Each value is coupled with a boolean that's set to true if the link was newly inserted.
	  */
	def store(links: Set[Link])(implicit connection: Connection, log: Logger) = {
		if (links.nonEmpty) {
			storeLock.synchronized {
				// Stores the domains & request paths and prepares new links for storing and inserting
				val (linksToInsert, linksToStore) = DbRequestPaths.storeFromLinks(links)
					.divideWith { case (path, link, wasInserted) =>
						if (wasInserted)
							Left(link -> path)
						else
							Right(link -> path)
					}
				// Checks for duplicates in certain cases
				val (preparedInserts, existingMatches) = {
					if (linksToStore.nonEmpty) {
						val existingPerPath = withPaths(linksToStore.view.map { _._2.id }.toIntSet).pull
							.groupBy { _.pathId }.withDefaultValue(Empty)
						val (newLinks, duplicates) = linksToStore
							.divideWith { case (link, path) =>
								existingPerPath.get(path.id)
									.flatMap { _.find { _.queryParameters ~== link.params } } match
								{
									case Some(existingMatch) => Right(link -> (DetailedLink(existingMatch, path) -> false))
									case None => Left(link -> path)
								}
							}
						
						(linksToInsert ++ newLinks) -> duplicates
					}
					else
						linksToInsert -> Empty
				}
				// Inserts link data
				val inserted = model
					.insertFrom(preparedInserts) {
						case (link, path) => LinkData(path.id, ensureModelMaxLength(link.params)) } {
						case (inserted, (original, path)) => original -> (DetailedLink(inserted, path) -> true) }
				
				View.concat(inserted, existingMatches).toMap
			}
		}
		else
			Map[Link, (DetailedLink, Boolean)]()
	}
	
	private def ensureModelMaxLength(model: Model) = {
		var m = model
		while (m.toJson.length > maximumModelLength) {
			val maxProp = m.properties.maxBy { _.value.toJson.length }
			m = m + maxProp.withValue("...")
		}
		m
	}
}

