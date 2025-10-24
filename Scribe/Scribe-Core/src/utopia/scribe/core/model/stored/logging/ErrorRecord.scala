package utopia.scribe.core.model.stored.logging

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.scribe.core.model.factory.logging.ErrorRecordFactoryWrapper
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.vault.store.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object ErrorRecord extends StoredFromModelFactory[ErrorRecordData, ErrorRecord]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = ErrorRecordData
	
	override protected def complete(model: HasProperties, data: ErrorRecordData) =
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a error record that has already been stored in the database
  * @param id   id of this error record in the database
  * @param data Wrapped error record data
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class ErrorRecord(id: Int, data: ErrorRecordData) 
	extends StoredModelConvertible[ErrorRecordData] with FromIdFactory[Int, ErrorRecord] 
		with ErrorRecordFactoryWrapper[ErrorRecordData, ErrorRecord]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: ErrorRecordData) = copy(data = data)
}

