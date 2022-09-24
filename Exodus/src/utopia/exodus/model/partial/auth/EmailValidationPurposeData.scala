package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * An enumeration for purposes an email validation may be used for
  * @param name Name of this email validation purpose. For identification (not localized).
  * @param created Time when this email validation purpose was first created
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
case class EmailValidationPurposeData(name: String, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("name" -> name, "created" -> created))
}

