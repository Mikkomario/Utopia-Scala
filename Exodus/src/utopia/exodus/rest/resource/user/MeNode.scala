package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Get
import utopia.access.http.Status.Unauthorized
import utopia.exodus.database.access.single.DbUser
import utopia.exodus.rest.resource.scalable.{ExtendableSessionResource, SessionUseCaseImplementation}
import utopia.flow.generic.ValueConversions._
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
	
	private val defaultGet = SessionUseCaseImplementation.default(Get) { (session, connection, _, _) =>
			implicit val c: Connection = connection
			// Reads user data and adds linked data
			DbUser(session.userId).withLinks match
			{
				case Some(user) => Result.Success(user.toModel)
				case None =>
					// Log.warning(s"User id ${session.userId} was authorized but couldn't be found from the database")
					Result.Failure(Unauthorized, "User no longer exists")
			}
		}
	
	override protected val defaultUseCaseImplementations = Vector(defaultGet)
	override protected val defaultFollowImplementations =
		Vector(MySettingsNode, MyOrganizationsNode, MyInvitationsNode, MyLanguagesNode, MyPasswordNode)
			.map { FollowImplementation.withChild(_) }
}
