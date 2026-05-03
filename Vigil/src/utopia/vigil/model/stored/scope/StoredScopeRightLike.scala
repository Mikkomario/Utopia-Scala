package utopia.vigil.model.stored.scope

import utopia.vault.store.{FromIdFactory, Stored, StoredModelConvertible}
import utopia.vigil.model.factory.scope.ScopeRightFactoryWrapper
import utopia.vigil.model.partial.scope.ScopeRightDataLike

import java.time.Instant

/**
  * Common trait for scope rights which have been stored in the database
  * @tparam Data Type of the wrapped data
  * @tparam Repr Implementing type
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait StoredScopeRightLike[Data <: ScopeRightDataLike[Data], +Repr] 
	extends Stored[Data, Int] with StoredModelConvertible[Data] with FromIdFactory[Int, Repr] 
		with ScopeRightFactoryWrapper[Data, Repr] with ScopeRightDataLike[Repr]
{
	// IMPLEMENTED	--------------------
	
	override def created = data.created
	
	override def scopeId = data.scopeId
	
	override def usable = data.usable
	
	override protected def wrappedFactory = data
	
	override def copyScopeRight(scopeId: Int, created: Instant, usable: Boolean) = 
		wrap(data.copyScopeRight(scopeId, created, usable))
}

