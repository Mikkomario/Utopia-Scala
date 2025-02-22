package utopia.logos.database.access.many.url.link

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.parse.string.Regex
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.NotEmpty
import utopia.logos.database.access.many.url.path.DbRequestPaths
import utopia.logos.model.cached.Link
import utopia.logos.model.combined.url.DetailedLink
import utopia.logos.model.partial.url.LinkData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple links at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbLinks extends ManyLinksAccess with UnconditionalView with ViewManyByIntIds[ManyLinksAccess]
{
	// ATTRIBUTES	--------------------
	
	private val maximumModelLength = 2048
	
	private lazy val parameterSeparatorRegex = Regex.escape('&').ignoringQuotations
	private lazy val parameterAssignmentRegex = Regex.escape('=').ignoringQuotations
	
	
	// OTHER	--------------------
	
	/**
	  * Stores all the specified links to the database.
	  * Avoids inserting duplicate information.
	  * @param links Links to store
	  * @param connection Implicit DB connection
	  * @return All inserted links (first) and links that already existed in the database (second).
	  * Each link contains domain and path information, also.
	  */
	def store(links: Set[String])(implicit connection: Connection) = {
		if (links.nonEmpty) {
			// Extracts the domain and parameter parts from each link
			// Ignores links without domain part (not expected as input)
			val separated = links.flatMap(Link.apply)
			// Stores the domains & request paths and prepares new links for storing and inserting
			val (linksToInsert2, linksToStore) = DbRequestPaths
				.storeFrom(separated) { _.domain } { _.path } { (path, link, wasInserted) =>
					if (wasInserted) Left(path -> link.params) else Right(path -> link.params)
				}
				.divided
			// Checks for duplicates in certain cases
			val (preparedInserts, existingMatches) = {
				if (linksToStore.nonEmpty) {
					val existingPerPath = withPaths(linksToStore.view.map { _._1.id }.toIntSet).pull
						.groupBy { _.pathId }.withDefaultValue(Empty)
					val (newLinks, duplicates) = linksToStore
						.divideWith { case (path, params) =>
							existingPerPath.get(path.id).flatMap { _.find { _.queryParameters ~== params } } match {
								case Some(existingMatch) => Right(DetailedLink(existingMatch, path))
								case None => Left(path -> params)
							}
						}
					
					(linksToInsert2 ++ newLinks) -> duplicates
				}
				else
					linksToInsert2 -> Empty
			}
			// Inserts link data
			val inserted = model.insertFrom(preparedInserts) { case (path, params) => LinkData(path.id, params) } {
				case (link, (path, _)) => DetailedLink(link, path) }
			
			Pair(inserted, existingMatches)
		}
		else
			Pair.twice(Empty)
	}
	
	private def ensureModelMaxLength(model: Model) = {
		var m = model
		while (m.toJson.length > maximumModelLength) {
			val maxProp = m.properties.maxBy { _.value.toJson.length }
			m = m + maxProp.withValue("...")
		}
		m
	}
	
	// Converts a parameters-string into a model
	// E.g. "foo=3&bar=test" would become {"foo": "3", "bar": "test"}
	private def paramsStringToModel(paramsString: String) = {
		// Splits to individual assignments
		val model = Model.withConstants(parameterSeparatorRegex.split(paramsString)
			.filter { _.nonEmpty }.flatMap { assignment =>
			// Splits into parameter name and value
			parameterAssignmentRegex.firstRangeFrom(assignment) match {
				case Some(assignRange) =>
					NotEmpty(assignment.take(assignRange.start)).map { paramName =>
						Constant(paramName, assignment.drop(assignRange.last + 1))
					}
				// Case: No assignment => Treats as null value
				case None => Some(Constant(assignment, Value.empty))
			}
		})
		// Limits maximum output length
		ensureModelMaxLength(model)
	}
}

