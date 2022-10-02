package utopia.scribe.model.partial.logging

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Represents a specific setting where a problem occurred
  * @param problemId Id of the problem that occurred
  * @param details Details about this problem case, like the error message, for example
  * @param created Time when this case first occurred
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemCaseData(problemId: Int, details: Option[String] = None, stack: Option[String] = None, 
	created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("problem_id" -> problemId, "details" -> details, "stack" -> stack, "created" -> created))
}

