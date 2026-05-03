package utopia.vigil.database.storable.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.Storable
import utopia.vault.store.{FromIdFactory, HasId}
import utopia.vigil.database.props.scope.ScopeRightDbProps
import utopia.vigil.model.factory.scope.ScopeRightFactory

import java.time.Instant

/**
  * Common trait for database models used for interacting with scope right data in the database
  * @tparam Repr Type of this DB model
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbModelLike[+Repr] 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, Repr] with ScopeRightFactory[Repr]
{
	// ABSTRACT	--------------------
	
	def scopeId: Option[Int]
	
	def created: Option[Instant]
	
	def usable: Option[Boolean]
	
	/**
	  * Access to the database properties which are utilized in this model
	  */
	def dbProps: ScopeRightDbProps
	
	/**
	  * @param id      Id to assign to the new model (default = currently assigned id)
	  * @param scopeId scope id to assign to the new model (default = currently assigned value)
	  * @param created created to assign to the new model (default = currently assigned value)
	  * @param usable  usable to assign to the new model (default = currently assigned value)
	  * @return Copy of this model with the specified scope right properties
	  */
	protected def copyScopeRight(id: Option[Int] = id, scopeId: Option[Int] = scopeId, 
		created: Option[Instant] = created, usable: Option[Boolean] = usable): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def valueProperties: Seq[(String, Value)] = 
		Vector(dbProps.id.name -> id, dbProps.scopeId.name -> scopeId, dbProps.created.name -> created, 
			dbProps.usable.name -> usable)
	
	override def withCreated(created: Instant) = copyScopeRight(created = Some(created))
	
	override def withId(id: Int) = copyScopeRight(id = Some(id))
	
	override def withScopeId(scopeId: Int) = copyScopeRight(scopeId = Some(scopeId))
	
	override def withUsable(usable: Boolean) = copyScopeRight(usable = Some(usable))
}

