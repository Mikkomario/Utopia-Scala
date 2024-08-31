package utopia.logos.database.factory.text

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.text.StatementDbModel
import utopia.logos.model.partial.text.StatementData
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.nosql.factory.row.FromTimelineRowFactory
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading statement data from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object StatementDbFactory 
	extends FromValidatedRowModelFactory[StoredStatement] with FromTimelineRowFactory[StoredStatement]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	def model = StatementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def timestamp = model.created
	
	override protected def fromValidatedModel(valid: Model) = 
		StoredStatement(valid(this.model.id.name).getInt, StatementData(valid(this.model.delimiterId.name).int,
			valid(this.model.created.name).getInstant))
}

