package utopia.exodus.model.partial.auth

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * An enumeration for different types of authentication tokens available
  * @param name Name of this token type for identification. Not localized.
  * @param duration Duration that determines how long these tokens remain valid after issuing. None if
  *  these tokens don't expire automatically.
  * @param refreshedTypeId Id of the type of token that may be acquired by using this token type as a refresh token,
  * if applicable
  * @param created Time when this token type was first created
  * @param isSingleUseOnly Whether tokens of this type may only be used once (successfully)
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenTypeData(name: String, duration: Option[FiniteDuration] = None, 
	refreshedTypeId: Option[Int] = None, created: Instant = Now, isSingleUseOnly: Boolean = false) 
	extends ModelConvertible
{
	// COMPUTED ------------------------
	
	/**
	  * @return Whether this token may be used as a refresh token
	  */
	def isRefreshToken = refreshedTypeId.isDefined
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("name" -> name, "duration_minutes" -> duration.map { _.toUnit(TimeUnit.MINUTES) },
			"refreshed_type_id" -> refreshedTypeId, "created" -> created, 
			"is_single_use_only" -> isSingleUseOnly))
}

