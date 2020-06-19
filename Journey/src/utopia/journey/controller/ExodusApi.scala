package utopia.journey.controller

import utopia.annex.controller.Api
import utopia.disciple.http.request.StringBody
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.journey.model.UserCredentials
import utopia.journey.model.enumeration.AuthorizationReason
import utopia.metropolis.model.post.NewUser

/**
  * An interface used for accessing the Exodus API
  * @author Mikko Hilpinen
  * @since 20.6.2020, v1
  */
class ExodusApi(override val rootPath: String, initialDeviceKey: Option[String] = None)(
	getUserCredentials: AuthorizationReason => Either[NewUser, UserCredentials]) extends Api
{
	// ATTRIBUTES	---------------------------
	
	private var sessionKey: Option[String] = None
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def headers = ???
	
	override protected def makeRequestBody(bodyContent: Value) = StringBody.json(bodyContent.toJson)
}
