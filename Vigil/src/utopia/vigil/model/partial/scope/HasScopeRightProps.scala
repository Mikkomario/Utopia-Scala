package utopia.vigil.model.partial.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible

import java.time.Instant

/**
  * Common trait for classes which provide access to scope right properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait HasScopeRightProps extends ModelConvertible
{
	// ABSTRACT	--------------------
	
	/**
	  * ID of the granted or accessible scope
	  */
	def scopeId: Int
	
	/**
	  * Time when this scope right was added to the database
	  */
	def created: Instant
	
	/**
	  * Whether the linked scope is directly accessible. 
	  * False if the scope is only applied when granting access for other authentication methods.
	  */
	def usable: Boolean
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("scopeId" -> scopeId, "created" -> created, "usable" -> usable))
}

