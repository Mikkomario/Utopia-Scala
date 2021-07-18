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
  * @param clientState State specified by the client service (optional)
  * @param created Creation time of this preparation
  */
case class AuthPreparationData(userId: Int, token: String, clientState: Option[String] = None, created: Instant = Now)
	extends StyledModelConvertible
{
	override def toSimpleModel = Model(Vector("token" -> token))
	
	override def toModel = Model(Vector("user_id" -> userId, "token" -> token,
		"state" -> clientState, "created" -> created))
}