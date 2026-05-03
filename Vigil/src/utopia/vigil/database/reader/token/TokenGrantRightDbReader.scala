package utopia.vigil.database.reader.token

import utopia.flow.generic.model.immutable.Model
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel
import utopia.vigil.database.storable.token.TokenGrantRightDbModel
import utopia.vigil.model.partial.token.TokenGrantRightData
import utopia.vigil.model.stored.token.TokenGrantRight

import scala.util.Success

/**
  * Used for reading token grant right data from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenGrantRightDbReader 
	extends DbRowReader[TokenGrantRight] with ParseTableModel[TokenGrantRight] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = TokenGrantRightDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(TokenGrantRight(valid(this.model.id.name).getInt, 
			TokenGrantRightData(ownerTemplateId = valid(this.model.ownerTemplateId.name).getInt, 
			grantedTemplateId = valid(this.model.grantedTemplateId.name).getInt, 
			revokes = valid(this.model.revokes.name).getBoolean)))
}

