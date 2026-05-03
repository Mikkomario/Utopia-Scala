package utopia.vigil.database.reader.token

import utopia.flow.generic.model.immutable.Model
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel
import utopia.vigil.database.storable.token.TokenDbModel
import utopia.vigil.model.partial.token.TokenData
import utopia.vigil.model.stored.token.Token

import scala.util.Success

/**
  * Used for reading token data from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenDbReader extends DbRowReader[Token] with ParseTableModel[Token] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = TokenDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(Token(valid(this.model.id.name).getInt, 
			TokenData(templateId = valid(this.model.templateId.name).getInt, 
			hash = valid(this.model.hash.name).getString, parentId = valid(this.model.parentId.name).int, 
			name = valid(this.model.name.name).getString, 
			created = valid(this.model.created.name).getInstant, 
			expires = valid(this.model.expires.name).instant, 
			revoked = valid(this.model.revoked.name).instant)))
}

