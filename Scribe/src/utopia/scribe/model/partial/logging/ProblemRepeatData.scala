package utopia.scribe.model.partial.logging

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Represents a case where a previously occurred problem repeats again
  * @param caseId Id of the problem case that repeated
  * @param created Time when that case repeated itself
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemRepeatData(caseId: Int, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("case_id" -> caseId, "created" -> created))
}

