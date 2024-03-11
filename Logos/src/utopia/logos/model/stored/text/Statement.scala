package utopia.logos.model.stored.text

import utopia.logos.database.access.single.text.statement.DbSingleStatement
import utopia.logos.model.partial.text.StatementData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a statement that has already been stored in the database
  * @param id id of this statement in the database
  * @param data Wrapped statement data
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class Statement(id: Int, data: StatementData) extends StoredModelConvertible[StatementData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this statement in the database
	  */
	def access = DbSingleStatement(id)
}

