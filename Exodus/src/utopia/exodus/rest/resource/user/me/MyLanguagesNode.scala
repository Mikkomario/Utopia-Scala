package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.{Delete, Get, Post, Put}
import utopia.access.http.Status.{BadRequest, Forbidden}
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.single.DbUser
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.metropolis.model.post.NewLanguageProficiency
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
 * Used for interacting with the languages known to the authorized user
 * @author Mikko Hilpinen
 * @since 16.5.2020, v1
 */
object MyLanguagesNode extends Resource[AuthorizedContext]
{
	// IMPLEMENTED	-------------------------
	
	override val name = "languages"
	
	override val allowedMethods = Vector(Get, Post, Put, Delete)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			val method = context.request.method
			val user = DbUser(session.userId)
			
			// GET simply returns existing user language links (with appropriate descriptions)
			if (method == Get) {
				// Reads language descriptions from the database, uses languages in the accept-language header
				val languages = user.languages.withDescriptionsInLanguages(context.languageIdListFor(session.userId))
					.sortBy { _.familiarity.orderIndex }
				// Supports simple model style also
				Result.Success(session.modelStyle match {
					case Full => languages.map { _.toModelWithoutUser }
					case Simple =>
						val roles = DbDescriptionRoles.pull
						languages.map { _.toSimpleModelUsing(roles) }
				})
			}
			// DELETE removes known languages
			else if (method == Delete) {
				context.handleArrayPost { values =>
					val existingLanguageIds = user.languageIds.toSet
					val languageIdsToDelete = values.flatMap { _.int }.toSet
					// Makes sure at leas one language remains
					if (existingLanguageIds.forall(languageIdsToDelete.contains))
						Result.Failure(Forbidden, "You must keep at least one language")
					else {
						user.languages.remove(languageIdsToDelete)
						// Returns a list of remaining language ids
						Result.Success((existingLanguageIds -- languageIdsToDelete).toVector)
					}
				}
			}
			else {
				context.handleModelArrayPost(NewLanguageProficiency) { proficiencies =>
					// Validates the proposed languages first
					DbLanguage.validateProposedProficiencies(proficiencies) match {
						case Success(proficiencies) =>
							// on POST, adds new language proficiencies (may overwrite existing)
							// on PUT, replaces languages
							if (proficiencies.isEmpty && method == Put)
								Result.Failure(Forbidden, "You must keep at least one language")
							else {
								val existingLanguages = user.languages.withFamiliarityLevels.toMap
								
								// Groups the changes
								val changesMap = proficiencies.map { _.toTuple }.toMap
								
								val changesInExisting = existingLanguages.flatMap { case (languageId, familiarity) =>
									changesMap.get(languageId).map { newFamiliarity =>
										languageId -> (familiarity -> newFamiliarity)
									}
								}
								val modifications = changesInExisting
									.filterNot { case (_, change) => change._1.id == change._2 }
								val duplicateLanguageIds = changesInExisting.filter { case (_, change) =>
									change._1.id == change._2
								}.keySet
								val newLanguages = changesMap.filterNot { case (languageId, _) =>
									existingLanguages.contains(languageId)
								}
								
								// Removes some languages (those not listed in PUT and those being modified)
								val languageIdsToRemove = {
									val base = modifications.keySet
									if (method == Put)
										base ++ existingLanguages.filterNot { case (languageId, _) =>
											changesMap.contains(languageId)
										}.keySet
									else
										base
								}
								user.languages.remove(languageIdsToRemove)
								
								// Adds new language links
								val inserted = (modifications.map { case (languageId, change) =>
									languageId -> change._2
								} ++ newLanguages).map { case (languageId, familiarity) =>
									user.languages.insert(languageId, familiarity)
								}
								
								// Returns the new languages list
								val duplicates = existingLanguages.filter { case (languageId, _) =>
									duplicateLanguageIds.contains(languageId)
								}
								val duplicateModels = duplicates.map { case (languageId, familiarity) =>
									Model(Vector("language_id" -> languageId, "familiarity_id" -> familiarity.id))
								}.toVector
								val insertedModels = inserted.map { l =>
									Model(Vector("language_id" -> l.languageId,
										"familiarity_id" -> l.familiarityId))
								}.toVector
								
								if (method == Put)
									Result.Success(duplicateModels ++ insertedModels)
								else {
									val previousModels = existingLanguages.filterNot { case (languageId, _) =>
										changesMap.contains(languageId)
									}.map { case (languageId, familiarity) =>
										Model(Vector("language_id" -> languageId, "familiarity_id" -> familiarity.id))
									}.toVector
									Result.Success(previousModels ++ duplicateModels ++ insertedModels)
								}
							}
						case Failure(error) => Result.Failure(BadRequest, error.getMessage)
					}
				}
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(
		message = Some("languages doesn't have any sub-resources at this time"))
}
