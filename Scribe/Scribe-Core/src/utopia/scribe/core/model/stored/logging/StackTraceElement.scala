package utopia.scribe.core.model.stored.logging

import utopia.scribe.core.model.stored.{StoredFromModelFactory, StoredModelConvertible}
import utopia.scribe.core.model.partial.logging.StackTraceElementData

object StackTraceElement extends StoredFromModelFactory[StackTraceElement, StackTraceElementData]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = StackTraceElementData
}

/**
  * Represents a stack trace element that has already been stored in the database
  * @param id id of this stack trace element in the database
  * @param data Wrapped stack trace element data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class StackTraceElement(id: Int, data: StackTraceElementData) 
	extends StoredModelConvertible[StackTraceElementData]

