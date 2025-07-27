package utopia.vault.test.database.storable.item

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter
import utopia.vault.store.{FromIdFactory, HasId}
import utopia.vault.test.database.VaultTestTables
import utopia.vault.test.model.factory.item.VersionedTestItemFactory
import utopia.vault.test.model.partial.item.VersionedTestItemData
import utopia.vault.test.model.stored.item.VersionedTestItem

import java.time.Instant

/**
  * Used for constructing VersionedTestItemDbModel instances and for inserting test items to the 
  * database
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
object VersionedTestItemDbModel 
	extends StorableFactory[VersionedTestItemDbModel, VersionedTestItem, VersionedTestItemData] 
		with FromIdFactory[Int, VersionedTestItemDbModel] with HasIdProperty 
		with VersionedTestItemFactory[VersionedTestItemDbModel] 
		with DeprecatableAfter[VersionedTestItemDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with names
	  */
	lazy val name = property("name")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	/**
	  * Database property used for interacting with deprecation times
	  */
	lazy val deprecatedAfter = property("deprecatedAfter")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VaultTestTables.versionedTestItem
	
	override def apply(data: VersionedTestItemData): VersionedTestItemDbModel = 
		apply(None, data.name, Some(data.created), data.deprecatedAfter)
	
	/**
	  * @param created Time when this versioned test item was added to the database
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when this versioned test item became deprecated. None while this 
	  *                        versioned test item is still valid.
	  * @return A model containing only the specified deprecated after
	  */
	override def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = 
		Some(deprecatedAfter))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param name Name of this test item
	  * @return A model containing only the specified name
	  */
	override def withName(name: String) = apply(name = name)
	
	override protected def complete(id: Value, data: VersionedTestItemData) = VersionedTestItem(id.getInt, 
		data)
}

/**
  * Used for interacting with TestItems in the database
  * @param id versioned test item database id
  * @author Mikko Hilpinen
  * @since 04.07.2025, v1.22
  */
case class VersionedTestItemDbModel(id: Option[Int] = None, name: String = "", 
	created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, VersionedTestItemDbModel]
		with VersionedTestItemFactory[VersionedTestItemDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(VersionedTestItemDbModel.id.name -> id, VersionedTestItemDbModel.name.name -> name, 
			VersionedTestItemDbModel.created.name -> created, 
			VersionedTestItemDbModel.deprecatedAfter.name -> deprecatedAfter)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VersionedTestItemDbModel.table
	
	/**
	  * @param created Time when this versioned test item was added to the database
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when this versioned test item became deprecated. None while this 
	  *                        versioned test item is still valid.
	  * @return A new copy of this model with the specified deprecated after
	  */
	override def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param name Name of this test item
	  * @return A new copy of this model with the specified name
	  */
	override def withName(name: String) = copy(name = name)
}

