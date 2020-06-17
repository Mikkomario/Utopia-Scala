package utopia.metropolis.model.stored.language

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Represents a language stored in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  */
case class Language(id: Int, isoCode: String) extends ModelConvertible
{
	override def toModel = Model(Vector("id" -> id, "iso_code" -> isoCode))
}
