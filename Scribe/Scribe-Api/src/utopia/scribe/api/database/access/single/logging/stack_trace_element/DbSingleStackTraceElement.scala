package utopia.scribe.api.database.access.single.logging.stack_trace_element

import utopia.scribe.core.model.stored.logging.StackTraceElement
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual stack trace elements, based on their id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class DbSingleStackTraceElement(id: Int) 
	extends UniqueStackTraceElementAccess with SingleIntIdModelAccess[StackTraceElement]

