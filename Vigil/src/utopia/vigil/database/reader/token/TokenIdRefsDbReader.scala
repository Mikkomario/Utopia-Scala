package utopia.vigil.database.reader.token

import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.enumeration.SelectTarget.Columns
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vigil.database.storable.token.TokenDbModel
import utopia.vigil.model.cached.token.TokenIdRefs

import scala.util.Try

/**
 * Used for reading token ID references from the token table
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
object TokenIdRefsDbReader extends DbRowReader[TokenIdRefs] with HasTableAsTarget
{
	// ATTRIBUTES   -----------------------
	
	private val model = TokenDbModel
	
	override val selectTarget: SelectTarget = Columns(model.id, model.templateId, model.parentId)
	
	
	// IMPLEMENTED  -----------------------
	
	override def table: Table = model.table
	
	override def shouldParse(row: Row): Boolean = row.contains(model.id)
	override def apply(row: Row): Try[TokenIdRefs] = {
		val data = row(table)
		data.tryGet(model.id.name) { _.tryInt }.flatMap { id =>
			data.tryGet(model.templateId.name) { _.tryInt }.map { templateId =>
				TokenIdRefs(id, templateId, data(model.parentId.name).int)
			}
		}
	}
}
