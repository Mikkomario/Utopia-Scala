package utopia.logos.database.reader.text

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.text.StatementDbModel
import utopia.logos.model.partial.text.StatementData
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading statement data from the DB
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object StatementDbReader 
	extends DbRowReader[StoredStatement] with ParseTableModel[StoredStatement] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = StatementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(StoredStatement(valid(this.model.id.name).getInt, 
			StatementData(valid(this.model.delimiterId.name).int, valid(this.model.created.name).getInstant)))
}

