package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.{Delete, Get, Post, Put}
import utopia.access.http.Status.{BadRequest, Forbidden}
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.many.language.DbLanguages
import utopia.citadel.database.access.single.user.DbUser
import utopia.citadel.database.model.user.UserLanguageLinkModel
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.metropolis.model.partial.user.UserLanguageLinkData
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
		context.sessionTokenAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			val method = context.request.method
			val userAccess = DbUser(session.userId)
			
			// GET simply returns existing user language links (with appropriate descriptions)
			if (method == Get)
			{
				// Reads language descriptions from the database, uses languages in the accept-language header
				implicit val acceptedLanguageIds: LanguageIds = session.languageIds
				val languages = userAccess.languageLinks.detailed.sortBy { _.familiarity.wrapped.orderIndex }
				// Supports simple model style
				Result.Success(session.modelStyle match {
					case Full => languages.map { _.toModel }
					case Simple =>
						val roles = DbDescriptionRoles.pull
						languages.map { _.toSimpleModelUsing(roles) }
				})
			}
			// DELETE removes known languages
			else if (method == Delete) {
				context.handleArrayPost { values =>
					val existingLanguageIds = userAccess.knownLanguageIds
					val languageIdsToDelete = values.flatMap { _.int }.toSet
					// Makes sure at leas one language remains
					if (existingLanguageIds.forall(languageIdsToDelete.contains))
						Result.Failure(Forbidden, "You must keep at least one language")
					else
					{
						userAccess.languageLinks.withAnyOfLanguages(languageIdsToDelete).delete()
						// Returns a list of remaining language ids
						Result.Success((existingLanguageIds -- languageIdsToDelete).toVector.sorted)
					}
				}
			}
			// PUT and POST expect a request body with a language proficiency array
			else {
				context.handleModelArrayPost(NewLanguageProficiency) { proficiencies =>
					// Validates the proposed languages first
					DbLanguages.validateProposedProficiencies(proficiencies) match {
						case Success(proficiencies) =>
							// on POST, adds new language proficiencies (may overwrite existing)
							// on PUT, replaces languages
							if (proficiencies.isEmpty && method == Put)
								Result.Failure(Forbidden, "You must keep at least one language")
							else {
								val existingLanguages = userAccess.languageLinks.withFamiliarities.pull
								// Language id -> Familiarity id
								val existingMap = existingLanguages.map { l => l.languageId -> l.familiarityId }.toMap
								// Groups the changes
								// Language id -> Familiarity id
								val changesMap = proficiencies
									.map { case (language, familiarity) => language.id -> familiarity.id }.toMap
								val changesInExisting = existingMap.flatMap { case (languageId, familiarityId) =>
									changesMap.get(languageId).map { newFamiliarityId =>
										languageId -> (familiarityId -> newFamiliarityId)
									}
								}
								val (modifications, duplicates) = changesInExisting
									.divideBy { case (_, (oldFamiliarityId, newFamiliarityId)) =>
										oldFamiliarityId == newFamiliarityId }
								val newLanguages = changesMap
									.filterNot { case (languageId, _) => existingMap.contains(languageId) }
								
								// Removes some languages (those not listed in PUT and those being modified)
								val languageIdsToRemove = {
									val base = modifications.keySet
									if (method == Put)
										base ++ existingMap
											.filterNot { case (languageId, _) => changesMap.contains(languageId) }
											.keySet
									else
										base
								}
								if (languageIdsToRemove.nonEmpty)
									userAccess.languageLinks.withAnyOfLanguages(languageIdsToRemove).delete()
								
								// Adds new language and modified links
								val inserted = UserLanguageLinkModel.insert(
									(modifications.map { case (languageId, (_, newFamiliarityId)) =>
										languageId -> newFamiliarityId } ++ newLanguages)
									.map { case (languageId, familiarityId) =>
										UserLanguageLinkData(session.userId, languageId, familiarityId) }
										.toVector)
								
								// Returns the new languages list
								val duplicateModels = duplicates.map { case (languageId, (familiarityId, _)) =>
									Model(Vector("language_id" -> languageId, "familiarity_id" -> familiarityId))
								}.toVector
								val insertedModels = inserted
									.map { l => Model(Vector("language_id" -> l.languageId,
										"familiarity_id" -> l.familiarityId)) }
								
								if (method == Put)
									Result.Success(duplicateModels ++ insertedModels)
								else
								{
									val previousModels = existingMap
										.filterNot { case (languageId, _) => changesMap.contains(languageId) }
										.map { case (languageId, familiarityId) =>
											Model(Vector("language_id" -> languageId,
												"familiarity_id" -> familiarityId)) }
										.toVector
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
