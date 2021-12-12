package utopia.keep.model.partial.logging

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.keep.model.enumeration.Severity

/**
  * Represents a type of problem that may occur during a program's run
  * @param context Program context where this problem occurred or was logged. Should be unique.
  * @param severity Severity of this problem
  * @param created Time when this problem first occurred
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemData(context: String, severity: Severity, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("context" -> context, "severity" -> severity.id, 
		"created" -> created))
}

