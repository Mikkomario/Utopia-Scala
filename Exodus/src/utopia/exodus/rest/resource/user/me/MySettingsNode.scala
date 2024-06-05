package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.{Get, Patch, Put}
import utopia.access.http.Status.{BadRequest, Forbidden, NotFound, Unauthorized}
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.model.enumeration.ExodusScope.{ChangeEmail, PersonalActions, ReadPersonalData}
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.resource.scalable.{ExtendableSessionResource, SessionUseCaseImplementation}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.post.UserSettingsUpdate
import utopia.metropolis.model.stored.user.UserSettings
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
 * Used for interacting with the authorized user's settings
 * @author Mikko Hilpinen
 * @since 20.5.2020, v1
 */
object MySettingsNode extends ExtendableSessionResource
{
	// ATTRIBUTES   -----------------------
	
	// GET: Reads current user settings
	private val defaultGet = SessionUseCaseImplementation.default { (token, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val ctx: AuthorizedContext = context
		if (token.access.hasScope(ReadPersonalData))
			token.userAccess match {
				case Some(userAccess) =>
					userAccess.settings.pull match {
						// Supports simple styling also
						case Some(readSettings) => Result.Success(readSettings.toModelWith(token.modelStyle))
						case None => Result.Failure(NotFound, "No current settings found")
					}
				case None => Result.Failure(Unauthorized, "Your current session doesn't specify who you are")
			}
		else
			Result.Failure(Unauthorized, "Your current session doesn't have access to this information")
	}
	// PUT: Replaces user's settings
	private val defaultPut = SessionUseCaseImplementation.default { (token, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val cnx: AuthorizedContext = context
		tryUpdate(token, isPut = true)
	}
	// PATCH: Updates user's settings (only those which are specified)
	private val defaultPatch = SessionUseCaseImplementation.default { (token, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val cnx: AuthorizedContext = context
		tryUpdate(token)
	}
	
	override val name = "settings"
	override protected val defaultUseCaseImplementations = Map(
		Get -> defaultGet, Patch -> defaultPatch, Put -> defaultPut)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def defaultFollowImplementations = Empty
	
	
	// OTHER    ---------------------------------------
	
	private def tryUpdate(token: Token, isPut: Boolean = false)
	                  (implicit context: AuthorizedContext, connection: Connection) =
	{
		// Makes sure the request is authorized
		token.ownerId match {
			case Some(userId) =>
				context.handlePost(UserSettingsUpdate) { proposedUpdate =>
					val oldSettings = DbUser(userId).settings.pull
					// The required scope varies
					val requiredScope = {
						if (proposedUpdate.definesDifferentEmailThan(oldSettings.flatMap { _.email }))
							ChangeEmail
						else
							PersonalActions
					}
					if (token.access.hasScope(requiredScope))
						update(userId, proposedUpdate, oldSettings, isPut)
					else
						Result.Failure(Unauthorized, "Your current session doesn't allow this change")
				}
			case None => Result.Failure(Unauthorized, "Your current session doesn't specify who you are")
		}
	}
	
	private def update(userId: Int, update: UserSettingsUpdate, oldSettings: Option[UserSettings], isPut: Boolean)
	                  (implicit connection: Connection) =
	{
		
		if (oldSettings.forall { update.isDifferentFrom(_) }) {
			// Makes sure name property is provided correctly (if required)
			(if (isPut) update.name else update.name.orElse { oldSettings.map { _.name } }) match {
				case Some(newName) =>
					val newEmail = {
						if (isPut)
							update.email
						else
							update.email.orElse { oldSettings.flatMap { _.email } }
					}
					// Some server settings allow email address to be empty while some require it
					if (newEmail.isEmpty && ExodusContext.userEmailIsRequired)
						Result.Failure(BadRequest, "'email' property is required")
					else {
						DbUser(userId).settings.tryUpdate(newName, newEmail,
							ExodusContext.uniqueUserNamesAreRequired) match
						{
							case Success(inserted) => Result.Success(inserted.toModel)
							case Failure(error) => Result.Failure(Forbidden, error.getMessage)
						}
					}
				case None => Result.Failure(BadRequest, "Property 'name' is required")
			}
		}
		// Case: Settings are not modified => returns existing settings
		else
			Result.Success(oldSettings.get.toModel)
	}
}
