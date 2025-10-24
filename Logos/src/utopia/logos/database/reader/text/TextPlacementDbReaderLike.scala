package utopia.logos.database.reader.text

import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.logos.model.partial.text.TextPlacementData
import utopia.logos.model.stored.text.TextPlacement
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Common trait for factories which parse text placement data from database-originated models
  * @tparam A Type of read instances
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
trait TextPlacementDbReaderLike[+A] extends DbRowReader[A] with ParseTableModel[A] with HasTableAsTarget
{
	// ABSTRACT	--------------------
	
	/**
	  * Database properties used when parsing column data
	  */
	def dbProps: TextPlacementDbProps
	
	/**
	  * @param model      Model from which additional data may be read
	  * @param id         Id to assign to the read/parsed text placement
	  * @param parentId   parent id to assign to the new text placement
	  * @param placedId   placed id to assign to the new text placement
	  * @param orderIndex order index to assign to the new text placement
	  * @return A text placement with the specified data
	  */
	protected def apply(model: HasProperties, id: Int, parentId: Int, placedId: Int, orderIndex: Int): A
	
	
	// IMPLEMENTED	--------------------
	
	override def fromValid(valid: Model) = 
		Success(apply(valid, valid(dbProps.id.name).getInt, valid(dbProps.parentId.name).getInt,
			valid(dbProps.placedId.name).getInt, valid(dbProps.orderIndex.name).getInt))
}

