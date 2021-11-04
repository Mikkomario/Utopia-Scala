package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * An enumeration for purposes an email validation may be used for
  * @param created Time when this EmailValidationPurpose was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class EmailValidationPurposeData(nameEn: String, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("name_en" -> nameEn, "created" -> created))
}

