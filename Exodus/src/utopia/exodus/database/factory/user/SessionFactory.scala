package utopia.exodus.database.factory.user

import java.time.Instant

import utopia.exodus.database.Tables
import utopia.exodus.database.model.user.SessionModel
import utopia.exodus.model.partial.UserSessionData
import utopia.exodus.model.stored.UserSession
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.model.enumeration.ComparisonOperator.Larger
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading user session data from the DB
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
object SessionFactory extends FromValidatedRowModelFactory[UserSession] with Deprecatable
{
	// IMPLEMENTED	-------------------------------
	
	// Non-deprecated keys must not be logged out or expired in the past
	override def nonDeprecatedCondition = table("logoutTime").isNull &&
		model.expiringIn(Instant.now()).toConditionWithOperator(Larger)
	
	override protected def fromValidatedModel(model: Model[Constant]) = UserSession(model("id").getInt,
		UserSessionData(model("userId").getInt, model("deviceId").getInt, model("key").getString,
			model("expiresIn").getInstant))
	
	override def table = Tables.userSession
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Model referenced by this factory
	  */
	def model = SessionModel
}
