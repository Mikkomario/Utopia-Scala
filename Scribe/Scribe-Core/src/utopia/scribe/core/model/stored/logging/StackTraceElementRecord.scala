package utopia.scribe.core.model.stored.logging

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.scribe.core.model.factory.logging.StackTraceElementRecordFactoryWrapper
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData
import utopia.vault.store.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object StackTraceElementRecord 
	extends StoredFromModelFactory[StackTraceElementRecordData, StackTraceElementRecord]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = StackTraceElementRecordData
	
	override protected def complete(model: HasProperties, data: StackTraceElementRecordData) =
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a stack trace element record that has already been stored in the database
  * @param id   id of this stack trace element record in the database
  * @param data Wrapped stack trace element record data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class StackTraceElementRecord(id: Int, data: StackTraceElementRecordData) 
	extends StoredModelConvertible[StackTraceElementRecordData] 
		with FromIdFactory[Int, StackTraceElementRecord] 
		with StackTraceElementRecordFactoryWrapper[StackTraceElementRecordData, StackTraceElementRecord]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: StackTraceElementRecordData) = copy(data = data)
}

