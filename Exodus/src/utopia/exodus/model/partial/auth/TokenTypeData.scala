package utopia.exodus.model.partial.auth

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * An enumeration for different types of authentication tokens available
  * @param name Name of this token type for identification. Not localized.
  * @param parentTypeId Id of the type of token used to acquire this token, if applicable
  * @param duration Duration that determines how long these tokens remain valid after issuing. None if
  *  these tokens don't expire automatically.
  * @param created Time when this token type was first created
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenTypeData(name: String, parentTypeId: Option[Int] = None, 
	duration: Option[FiniteDuration] = None, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("name" -> name, "parent_type_id" -> parentTypeId, 
			"duration" -> duration.map { _.toUnit(TimeUnit.MINUTES) }, "created" -> created))
}

