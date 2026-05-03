package utopia.vigil.database.reader.token

import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.TimeExtensions._
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel
import utopia.vigil.database.storable.token.TokenTemplateDbModel
import utopia.vigil.model.enumeration.ScopeGrantType
import utopia.vigil.model.partial.token.TokenTemplateData
import utopia.vigil.model.stored.token.TokenTemplate

/**
  * Used for reading token template data from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenTemplateDbReader 
	extends DbRowReader[TokenTemplate] with ParseTableModel[TokenTemplate] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = TokenTemplateDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = {
		ScopeGrantType.fromValue(valid(this.model.scopeGrantType.name)).map { scopeGrantType => 
			TokenTemplate(valid(this.model.id.name).getInt, 
				TokenTemplateData(name = valid(this.model.name.name).getString, 
				scopeGrantType = scopeGrantType, 
				duration = valid(this.model.duration.name).long.map { _.millis }, 
				created = valid(this.model.created.name).getInstant))
		}
	}
}

