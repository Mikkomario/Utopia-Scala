package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.Get
import utopia.access.http.Status.{NotFound, Unauthorized}
import utopia.exodus.model.enumeration.ExodusScope.ReadPersonalData
import utopia.exodus.rest.resource.scalable.{ExtendableSessionResource, SessionUseCaseImplementation}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.nexus.rest.scalable.FollowImplementation
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * This rest-resource represents the logged user
 * @author Mikko Hilpinen
 * @since 6.5.2020, v1
 */
object MeNode extends ExtendableSessionResource
{
	override val name = "me"
	
	private val defaultGet = SessionUseCaseImplementation.default { (session, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val cntx: AuthorizedContext = context
		// Reads user data and adds linked data
		session.userAccess match {
			case Some(userAccess) =>
				// Makes sure the client is authorized to read user data
				if (session.access.hasScope(ReadPersonalData)) {
					implicit val languageIds: LanguageIds = session.languageIds
					userAccess.detailed match {
						case Some(user) => Result.Success(user.toModelWith(session.modelStyle))
						case None =>
							Result.Failure(NotFound, "User no longer exists")
					}
				}
				else
					Result.Failure(Unauthorized, "You're not allowed to read this user's personal data")
			case None => Result.Failure(NotFound, "Your current session doesn't specify who 'me' is")
		}
	}
	
	override protected val defaultUseCaseImplementations = Map(Get -> defaultGet)
	override protected val defaultFollowImplementations =
		Vector(MySettingsNode, MyOrganizationsNode, MyInvitationsNode, MyLanguagesNode, MyPasswordNode, MySessionsNode)
			.map { FollowImplementation.withChild(_) }
}
