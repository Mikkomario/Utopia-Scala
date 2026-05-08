package utopia.scribe.core.model.stored.logging

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.scribe.core.model.factory.logging.ErrorRecordFactoryWrapper
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredFromModelFactory, StoredModelConvertible}

object ErrorRecord 
	extends StandardStoredFactory[ErrorRecordData, ErrorRecord] 
		with StoredFromModelFactory[ErrorRecordData, ErrorRecord]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = ErrorRecordData
	
	
	// IMPLEMENTED	--------------------
	
	override protected def complete(model: HasProperties, data: ErrorRecordData) = 
		model("id").tryInt.map { apply(_, data) }
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new error record
	  * @param id   ID of this error record in the database
	  * @param data Wrapped error record data
	  * @return error record with the specified id and wrapped data
	  */
	def apply(id: Int, data: ErrorRecordData): ErrorRecord = _ErrorRecord(id, data)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the error record trait
	  * @param id   ID of this error record in the database
	  * @param data Wrapped error record data
	  * @author Mikko Hilpinen
	  * @since 08.05.2026
	  */
	private case class _ErrorRecord(id: Int, data: ErrorRecordData) extends ErrorRecord
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(data: ErrorRecordData) = copy(data = data)
	}
}

/**
  * Represents a error record that has already been stored in the database. 
  * Represents a single error or exception thrown during program runtime
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ErrorRecord 
	extends StoredModelConvertible[ErrorRecordData] with FromIdFactory[Int, ErrorRecord] 
		with ErrorRecordFactoryWrapper[ErrorRecordData, ErrorRecord]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
}
