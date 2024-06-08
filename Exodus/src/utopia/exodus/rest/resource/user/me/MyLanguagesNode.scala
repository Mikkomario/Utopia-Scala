package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method
import utopia.access.http.Method.{Delete, Get, Post, Put}
import utopia.access.http.Status.{BadRequest, Forbidden, Unauthorized}
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.many.language.DbLanguages
import utopia.citadel.database.access.single.user.DbUser
import utopia.citadel.database.model.user.UserLanguageLinkModel
import utopia.exodus.model.enumeration.ExodusScope.{PersonalActions, ReadPersonalData}
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.collection.CollectionExtensions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.post.NewLanguageProficiency
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
 * Used for interacting with the languages known to the authorized user
 * @author Mikko Hilpinen
 * @since 16.5.2020, v1
 */
object MyLanguagesNode extends LeafResource[AuthorizedContext]
{
	// ATTRIBUTES   -------------------------
	
	override val name = "languages"
	override val allowedMethods = Vector(Get, Post, Put, Delete)
	
	
	// IMPLEMENTED	-------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		val method = context.request.method
		// May require write access
		context.authorizedForScope(if (method == Get) ReadPersonalData else PersonalActions) { (token, connection) =>
			// Token must be tied to a user
			token.ownerId match {
				case Some(userId) =>
					implicit val c: Connection = connection
					// GET simply returns existing user language links (with appropriate descriptions)
					if (method == Get)
						handleGet(token, userId)
					// DELETE removes known languages
					else if (method == Delete)
						handleDelete(userId)
					// PUT and POST expect a request body with a language proficiency array
					else
						handlePostAndPut(method, userId)
				case None => Result.Failure(Unauthorized, "Your current session doesn't specify who you are")
			}
		}
	}
	
	
	// OTHER    ----------------------------
	
	private def handleGet(token: Token, userId: Int)
	                     (implicit connection: Connection, context: AuthorizedContext) =
	{
		// Reads language descriptions from the database, uses languages in the accept-language header
		implicit val acceptedLanguageIds: LanguageIds = token.languageIds
		val languages = DbUser(userId).languageLinks.detailed.sortBy { _.familiarity.wrapped.orderIndex }
		// Supports simple model style
		Result.Success(token.modelStyle match {
			case Full => languages.map { _.toModel }.toVector
			case Simple =>
				val roles = DbDescriptionRoles.pull
				languages.map { _.toSimpleModelUsing(roles) }.toVector
		})
	}
	
	private def handleDelete(userId: Int)(implicit connection: Connection, context: AuthorizedContext) = {
		context.handleArrayPost { values =>
			val userAccess = DbUser(userId)
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
	
	private def handlePostAndPut(method: Method, userId: Int)
	                            (implicit connection: Connection, context: AuthorizedContext) =
	{
		context.handleModelArrayPost(NewLanguageProficiency) { proficiencies =>
			// Validates the proposed languages first
			DbLanguages.validateProposedProficiencies(proficiencies) match {
				case Success(proficiencies) =>
					// on POST, adds new language proficiencies (may overwrite existing)
					// on PUT, replaces languages
					if (proficiencies.isEmpty && method == Put)
						Result.Failure(Forbidden, "You must keep at least one language")
					else {
						val userAccess = DbUser(userId)
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
								oldFamiliarityId == newFamiliarityId
							}
							.toTuple
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
									UserLanguageLinkData(userId, languageId, familiarityId) }
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
