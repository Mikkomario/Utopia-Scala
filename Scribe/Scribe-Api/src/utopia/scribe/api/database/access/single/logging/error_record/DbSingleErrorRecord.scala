package utopia.scribe.api.database.access.single.logging.error_record

import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual error records, based on their id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class DbSingleErrorRecord(id: Int) 
	extends UniqueErrorRecordAccess with SingleIntIdModelAccess[ErrorRecord]

