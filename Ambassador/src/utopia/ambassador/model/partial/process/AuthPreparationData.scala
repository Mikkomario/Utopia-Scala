package utopia.ambassador.model.partial.process

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

import java.time.Instant

/**
  * Contains infromation about an authentication preparation
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param userId Id of the user for whom this authentication is prepared
  * @param token Authentication token created during this preparation
  * @param expiration Expiration time of this preparation token
  * @param clientState State specified by the client service (optional)
  * @param created Creation time of this preparation (default = now)
  */
case class AuthPreparationData(userId: Int, token: String, expiration: Instant, clientState: Option[String] = None,
                               created: Instant = Now)
	extends StyledModelConvertible
{
	override def toSimpleModel = Model(Vector("token" -> token, "expiration" -> expiration))
	
	override def toModel = Model(Vector("user_id" -> userId, "token" -> token,
		"state" -> clientState, "created" -> created, "expiration" -> expiration))
}