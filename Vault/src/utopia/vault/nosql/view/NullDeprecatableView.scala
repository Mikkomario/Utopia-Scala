package utopia.vault.nosql.view

import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.storable.deprecation.NullDeprecatable
import utopia.vault.sql.{Update, Where}

/**
  * Common trait for access points that target items that can be deprecated by specifying a non-null timestamp
  * @author Mikko Hilpinen
  * @since 3.4.2023, v1.15.1
  */
trait NullDeprecatableView[+Sub] extends TimeDeprecatableView[Sub]
{
	// ABSTRACT -----------------------
	
	override protected def model: NullDeprecatable[Storable]
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return Access to deprecated (historical) items
	  */
	def deprecated = filter(model.deprecatedCondition)
	
	
	// OTHER    ---------------------
	
	/**
	  * Deprecates all accessible items
	  * @param c Implicit DB Connection
	  * @return Whether any item was targeted
	  */
	def deprecate()(implicit c: Connection) =
		c(Update(target, model.deprecationColumn, Now.toValue) + accessCondition.map { Where(_) }).updatedRows
}
