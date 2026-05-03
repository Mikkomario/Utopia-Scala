package utopia.vigil.model.partial.scope

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.BooleanType
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.time.Now

import java.time.Instant

object ScopeRightData extends FromModelFactoryWithSchema[ScopeRightData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("scopeId", IntType, Single("scope_id")), 
			PropertyDeclaration("created", InstantType, isOptional = true), PropertyDeclaration("usable", 
			BooleanType, Empty, false)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		ScopeRightData(valid("scopeId").getInt, valid("created").getInstant, valid("usable").getBoolean)
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new scope right
	  * @param scopeId ID of the granted or accessible scope
	  * @param created Time when this scope right was added to the database
	  * @param usable  Whether the linked scope is directly accessible. 
	  *                False if the scope is only applied when granting access for other 
	  *                authentication methods.
	  * @return scope right with the specified properties
	  */
	def apply(scopeId: Int, created: Instant = Now, usable: Boolean = false): ScopeRightData = 
		_ScopeRightData(scopeId, created, usable)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the scope right data trait
	  * @param scopeId ID of the granted or accessible scope
	  * @param created Time when this scope right was added to the database
	  * @param usable  Whether the linked scope is directly accessible. 
	  *                False if the scope is only applied when granting access for other 
	  *                authentication methods.
	  * @author Mikko Hilpinen
	  * @since 01.05.2026
	  */
	private case class _ScopeRightData(scopeId: Int, created: Instant = Now, usable: Boolean = false) 
		extends ScopeRightData
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param scopeId ID of the granted or accessible scope
		  * @param created Time when this scope right was added to the database
		  * @param usable  Whether the linked scope is directly accessible. 
		  *                False if the scope is only applied when granting access for other 
		  *                authentication methods.
		  */
		override def copyScopeRight(scopeId: Int, created: Instant = Now, usable: Boolean = false) = 
			_ScopeRightData(scopeId, created, usable)
	}
}

/**
  * Links a scope to an authentication method that grants that scope
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightData extends ScopeRightDataLike[ScopeRightData]

