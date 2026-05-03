package utopia.vigil.database.storable.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}
import utopia.vigil.database.VigilTables
import utopia.vigil.model.factory.scope.ScopeFactory
import utopia.vigil.model.partial.scope.ScopeData
import utopia.vigil.model.stored.scope.Scope

/**
  * Used for constructing ScopeDbModel instances and for inserting scopes to the database
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object ScopeDbModel 
	extends StorableFactory[ScopeDbModel, Scope, ScopeData] with FromIdFactory[Int, ScopeDbModel] 
		with HasIdProperty with ScopeFactory[ScopeDbModel]
{
	// ATTRIBUTES	--------------------
	
	override val id = DbPropertyDeclaration("id", index)
	/**
	  * Database property used for interacting with keys
	  */
	lazy val key = property("key")
	/**
	  * Database property used for interacting with parent ids
	  */
	lazy val parentId = property("parentId")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VigilTables.scope
	
	override def apply(data: ScopeData): ScopeDbModel = apply(None, data.key, data.parentId)
	
	override def withId(id: Int) = apply(id = Some(id))
	override def withKey(key: String) = apply(key = key)
	override def withParentId(parentId: Option[Int]): ScopeDbModel = apply(parentId = parentId)
	
	override protected def complete(id: Value, data: ScopeData) = Scope(id.getInt, data)
}

/**
  * Used for interacting with Scopes in the database
  * @param id scope database id
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class ScopeDbModel(id: Option[Int] = None, key: String = "", parentId: Option[Int] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, ScopeDbModel] 
		with ScopeFactory[ScopeDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(ScopeDbModel.id.name -> id, ScopeDbModel.key.name -> key, 
			ScopeDbModel.parentId.name -> parentId)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScopeDbModel.table
	
	override def withId(id: Int) = copy(id = Some(id))
	override def withKey(key: String) = copy(key = key)
	override def withParentId(parentId: Option[Int]): ScopeDbModel = copy(parentId = parentId)
}

