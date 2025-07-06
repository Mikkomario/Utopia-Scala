package utopia.vault.test.model.stored.item

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.vault.model.template.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}
import utopia.vault.test.database.access.item.versioned.AccessVersionedTestItem
import utopia.vault.test.model.factory.item.VersionedTestItemFactoryWrapper
import utopia.vault.test.model.partial.item.VersionedTestItemData

object VersionedTestItem extends StoredFromModelFactory[VersionedTestItemData, VersionedTestItem]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = VersionedTestItemData
	
	override protected def complete(model: AnyModel, data: VersionedTestItemData) = 
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a versioned test item that has already been stored in the database
  * @param id   id of this versioned test item in the database
  * @param data Wrapped versioned test item data
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class VersionedTestItem(id: Int, data: VersionedTestItemData) 
	extends StoredModelConvertible[VersionedTestItemData] with FromIdFactory[Int, VersionedTestItem] 
		with VersionedTestItemFactoryWrapper[VersionedTestItemData, VersionedTestItem]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this versioned test item in the database
	  */
	def access = AccessVersionedTestItem(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: VersionedTestItemData) = copy(data = data)
}

