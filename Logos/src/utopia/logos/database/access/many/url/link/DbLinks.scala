package utopia.logos.database.access.many.url.link

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.parse.string.Regex
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._
import utopia.logos.database.access.many.url.domain.DbDomains
import utopia.logos.database.access.many.url.path.DbRequestPaths
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.logos.model.cached.Link
import utopia.logos.model.combined.url.DetailedLink
import utopia.logos.model.partial.url.{LinkData, RequestPathData}
import utopia.logos.model.stored.url.{Domain, StoredLink}
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
			val separated = links.flatMap { link =>
				Domain.regex.firstRangeFrom(link).map { domainRange =>
					// Removes the / from domain, if present
					val domainPart = link.slice(domainRange).notEndingWith("/")
					val remainingPart = link.drop(domainRange.last + 1)
					Link.paramPartRegex.firstRangeFrom(remainingPart) match {
						// Case: Parameters are specified => Extracts them from the path
						case Some(paramsRange) =>
							val paramsPart = remainingPart.slice(paramsRange).drop(1)
							val pathPart = remainingPart.take(paramsRange.start)
							(domainPart, pathPart, paramsPart)
						// Case: No parameters specified
						case None => (domainPart, remainingPart, "")
					}
				}
			}
			val linksPerDomain = separated.groupMap { _._1 } { case (_, path, params) => path -> params }
			
			// Stores the domain names
			// Each entry also specifies whether it was newly inserted
			val domains = DbDomains.store(linksPerDomain.keySet)
			val domainMap = domains.merge { (inserted, existing) =>
				(inserted.map { d => d.url.toLowerCase -> (d, true) } ++
					existing.map { d => d.url.toLowerCase -> (d, false) }).toMap
			}
			val domainPerId = domains.merge { _ ++ _ }.map { d => d.id -> d }.toMap
			
			// Stores request paths next
			// Won't check for duplicates under new domains
			// Keys are domain ids; Values are maps where keys are request paths and values are links;
			// Each primary value also contains a boolean indicating whether the domain was newly inserted
			val linksPerDomainId = linksPerDomain.toVector
				.map { case (domainUrl, links) =>
					val (domain, wasInserted) = domainMap(domainUrl.toLowerCase)
					val groupedLinks = links.groupMap { _._1 } { _._2 }
					domain.id -> (groupedLinks -> wasInserted)
				}
				.groupMapReduce { _._1 } { _._2 } { case ((links1, wasInserted1), (links2, wasInserted2)) =>
					links1.mergeWith(links2) { _ ++ _ } -> (wasInserted1 || wasInserted2)
				}
			val (pathsToInsert, pathsToStore) = linksPerDomainId.divideWith { case (domainId, (links, 
				wasInserted)) =>
				val data = domainId -> links
				if (wasInserted) Left(data) else Right(data)
			}
			val firstInsertedPaths = RequestPathDbModel
				.insert(pathsToInsert.flatMap { case (domainId, paths) =>
					paths.keys.distinctBy { _.toLowerCase }.map { RequestPathData(domainId, _) }.toVector
				})
			val firstInsertedPathMap = firstInsertedPaths.groupBy { _.domainId }
			val storedPathMaps = DbRequestPaths.store(
				pathsToStore.map { case (domainId, paths) => domainId -> paths.keySet }.toMap)
			val insertedPathMap = firstInsertedPathMap.mergeWith(storedPathMaps.first) { _ ++ _ }
			// First keys are domain ids; Second keys are lower-case paths;
			// Values are path ids + whether they were inserted
			val pathsMap = insertedPathMap.view.mapValues { _.map { _ -> true } }.toMap
				.mergeWith(storedPathMaps.second.view.mapValues { _.map { _ -> false } }.toMap) { _ ++ _ }
				.view.mapValues { _.map { case (path, wasInserted) =>
					path.path.toLowerCase -> (path.id -> wasInserted) }.toMap
				}.toMap
			val pathPerId = (firstInsertedPaths.iterator ++ 
				storedPathMaps.iterator.flatMap { _.valuesIterator.flatten })
				.map { p => p.id -> p }.toMap
			
			// Next stores individual links
			// Won't check for duplicates under inserted paths
			val (linksToFreelyInsert, linksToStore) = linksPerDomainId.splitFlatMap { case (domainId, 
				(linksPerPath, _)) =>
				val pathIdMap = pathsMap(domainId)
				// Keys are path ids; Values are link parameter sets + whether that path was inserted
				val linksPerPathId = linksPerPath.iterator
					.map { case (path, paramSets) =>
						val (pathId, wasInserted) = pathIdMap(path.toLowerCase)
						pathId -> (paramSets, wasInserted)
					}
					.toVector.groupMapReduce { _._1 } { _._2 } { case ((params1, wasInserted1), (params2, 
						wasInserted2)) =>
						(params1 ++ params2) -> (wasInserted1 || wasInserted2)
					}
				linksPerPathId.flatDivideWith { case (pathId, (paramSets, pathWasInserted)) =>
					// Parses the parameter sets into models
					val paramModels = paramSets.toVector.map(paramsStringToModel)
					// Groups to cases where duplicate-checking is required (second) and where it is not (first)
					paramModels.map { model =>
						if (pathWasInserted) Left(LinkData(pathId, model)) else Right(pathId -> model)
					}
				}
			}
			// Checks for duplicates in certain cases
			val (linksToInsert, existingLinks) = {
				if (linksToStore.nonEmpty) {
					val existing = withPaths(linksToStore.map { _._1 }.toSet).pull
					val existingParamsPerPathId = existing.groupMap { _.pathId } { _.queryParameters }
					val newLinks = linksToStore.filter { case (pathId, params) =>
						existingParamsPerPathId.get(pathId).forall { _.forNone { _ ~== params } }
					}
					val linksToInsert = linksToFreelyInsert ++
						newLinks.map { case (pathId, params) => LinkData(pathId, params) }
					linksToInsert -> existing
				}
				else
					linksToFreelyInsert -> Empty
			}
			// Inserts link data
			val insertedLinks = model.insert(linksToInsert)
			
			// Combines domain, path and link information
			Pair(insertedLinks, existingLinks).map { _.map { link =>
				val path = pathPerId(link.pathId)
				val domain = domainPerId(path.domainId)
				DetailedLink(link, domain, path)
			} }
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

