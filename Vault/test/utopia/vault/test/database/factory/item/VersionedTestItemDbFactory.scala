package utopia.vault.test.database.factory.item

import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.FromTimelineRowFactory
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.test.database.storable.item.VersionedTestItemDbModel
import utopia.vault.test.model.partial.item.VersionedTestItemData
import utopia.vault.test.model.stored.item.VersionedTestItem

/**
  * Used for reading versioned test item data from the DB
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
object VersionedTestItemDbFactory 
	extends FromValidatedRowModelFactory[VersionedTestItem] with FromTimelineRowFactory[VersionedTestItem] 
		with Deprecatable
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = VersionedTestItemDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override def table = model.table
	
	override def timestamp = model.created
	
	override protected def fromValidatedModel(valid: Model) = 
		VersionedTestItem(valid(this.model.id.name).getInt, 
			VersionedTestItemData(valid(this.model.name.name).getString, 
			valid(this.model.created.name).getInstant, valid(this.model.deprecatedAfter.name).instant))
}

