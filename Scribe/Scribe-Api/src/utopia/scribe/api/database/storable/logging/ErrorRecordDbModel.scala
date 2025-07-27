package utopia.scribe.api.database.storable.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.logging.ErrorRecordFactory
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

/**
  * Used for constructing ErrorRecordDbModel instances and for inserting error records to the 
  * database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object ErrorRecordDbModel 
	extends StorableFactory[ErrorRecordDbModel, ErrorRecord, ErrorRecordData] 
		with FromIdFactory[Int, ErrorRecordDbModel] with HasIdProperty 
		with ErrorRecordFactory[ErrorRecordDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with exception types
	  */
	lazy val exceptionType = property("exceptionType")
	
	/**
	  * Database property used for interacting with stack trace ids
	  */
	lazy val stackTraceId = property("stackTraceId")
	
	/**
	  * Database property used for interacting with cause ids
	  */
	lazy val causeId = property("causeId")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.errorRecord
	
	override def apply(data: ErrorRecordData): ErrorRecordDbModel = 
		apply(None, data.exceptionType, Some(data.stackTraceId), data.causeId)
	
	/**
	  * @param causeId Id of the underlying error that caused this error/failure. None if this error 
	  *                represents the root problem.
	  * @return A model containing only the specified cause id
	  */
	override def withCauseId(causeId: Int) = apply(causeId = Some(causeId))
	
	/**
	  * @param exceptionType The name of this exception type. Typically the exception class name.
	  * @return A model containing only the specified exception type
	  */
	override def withExceptionType(exceptionType: String) = apply(exceptionType = exceptionType)
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param stackTraceId Id of the topmost stack trace element that corresponds to this error 
	  *                     record
	  * @return A model containing only the specified stack trace id
	  */
	override def withStackTraceId(stackTraceId: Int) = apply(stackTraceId = Some(stackTraceId))
	
	override protected def complete(id: Value, data: ErrorRecordData) = ErrorRecord(id.getInt, data)
}

/**
  * Used for interacting with ErrorRecords in the database
  * @param id error record database id
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
case class ErrorRecordDbModel(id: Option[Int] = None, exceptionType: String = "", 
	stackTraceId: Option[Int] = None, causeId: Option[Int] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, ErrorRecordDbModel] 
		with ErrorRecordFactory[ErrorRecordDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(ErrorRecordDbModel.id.name -> id, ErrorRecordDbModel.exceptionType.name -> exceptionType, 
			ErrorRecordDbModel.stackTraceId.name -> stackTraceId, ErrorRecordDbModel.causeId.name -> causeId)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ErrorRecordDbModel.table
	
	/**
	  * @param causeId Id of the underlying error that caused this error/failure. None if this error 
	  *                represents the root problem.
	  * @return A new copy of this model with the specified cause id
	  */
	override def withCauseId(causeId: Int) = copy(causeId = Some(causeId))
	
	/**
	  * @param exceptionType The name of this exception type. Typically the exception class name.
	  * @return A new copy of this model with the specified exception type
	  */
	override def withExceptionType(exceptionType: String) = copy(exceptionType = exceptionType)
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param stackTraceId Id of the topmost stack trace element that corresponds to this error 
	  *                     record
	  * @return A new copy of this model with the specified stack trace id
	  */
	override def withStackTraceId(stackTraceId: Int) = copy(stackTraceId = Some(stackTraceId))
}

