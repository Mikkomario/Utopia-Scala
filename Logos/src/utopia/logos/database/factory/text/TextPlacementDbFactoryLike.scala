package utopia.logos.database.factory.text

import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Common trait for factories which parse text placement data from database-originated models
  * @tparam A Type of read instances
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDbFactoryLike[+A] extends FromValidatedRowModelFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * Database properties used when parsing column data
	  */
	def dbProps: TextPlacementDbProps
	
	/**
	  * @param model Model from which additional data may be read
	  * @param id Id to assign to the read/parsed text placement
	  * @param parentId parent id to assign to the new text placement
	  * @param placedId placed id to assign to the new text placement
	  * @param orderIndex order index to assign to the new text placement
	  * @return A text placement with the specified data
	  */
	protected def apply(model: AnyModel, id: Int, parentId: Int, placedId: Int, orderIndex: Int): A
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		apply(valid, valid(dbProps.id.name).getInt, valid(dbProps.parentId.name).getInt, 
			valid(dbProps.placedId.name).getInt, valid(dbProps.orderIndex.name).getInt)
}

