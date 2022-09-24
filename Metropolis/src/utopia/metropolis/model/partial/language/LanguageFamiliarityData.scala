package utopia.metropolis.model.partial.language

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Represents a language skill level
  * @param orderIndex Index used for ordering between language familiarities, 
	where lower values mean higher familiarity
  * @param created Time when this LanguageFamiliarity was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class LanguageFamiliarityData(orderIndex: Int, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("order_index" -> orderIndex, "created" -> created))
}

