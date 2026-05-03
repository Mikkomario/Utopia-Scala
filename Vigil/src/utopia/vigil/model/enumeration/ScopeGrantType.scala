package utopia.vigil.model.enumeration

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.equality.EqualsExtensions._

/**
  * Enumeration for different ways scopes may be adjusted when generating new tokens
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
sealed trait ScopeGrantType extends ValueConvertible
{
	// ABSTRACT	--------------------
	
	/**
	  * id used to represent this scope grant type in database and json
	  */
	def id: Int
	
	
	// IMPLEMENTED	--------------------
	
	override def toValue = id
}

object ScopeGrantType
{
	// ATTRIBUTES	--------------------
	
	/**
	  * All available scope grant type values
	  */
	val values: Vector[ScopeGrantType] = Vector(Dictate, Grant, Restrict, Copy)
	
	
	// OTHER	--------------------
	
	/**
	  * @param id id representing a scope grant type
	  * @return scope grant type matching the specified id. None if the id didn't match any scope grant type
	  */
	def findForId(id: Int) = values.find { _.id == id }
	/**
	  * @param value A value representing an scope grant type id
	  * @return scope grant type matching the specified value. None if the value didn't match any scope grant 
	  * type
	  */
	def findForValue(value: Value) = value.intOrString.flatMap {
		 case Left(id) => findForId(id); case Right(name) => values.find { _.toString ~== name }
	}
	/**
	  * @param id id matching a scope grant type
	  * @return scope grant type matching that id. Failure if no matching value was found.
	  */
	def forId(id: Int) = 
		findForId(id).toTry { new NoSuchElementException(s"No value of ScopeGrantType matches id '$id'") }
	/**
	  * @param value A value representing an scope grant type id
	  * @return scope grant type matching the specified value, when the value is interpreted as an scope 
	  * grant type id. Failure if no matching value was found.
	  */
	def fromValue(value: Value) = 
		findForValue(value).toTry { new NoSuchElementException(
			s"No value of ScopeGrantType matches id '$value'") }
	
	
	// NESTED	--------------------
	
	/**
	  * The template copies the parent token's scope to the new token
	  * @since 01.05.2026
	  */
	case object Copy extends ScopeGrantType
	{
		// ATTRIBUTES	--------------------
		
		override val id = 4
	}
	
	/**
	  * The template dictates the scope of the new token
	  * @since 01.05.2026
	  */
	case object Dictate extends ScopeGrantType
	{
		// ATTRIBUTES	--------------------
		
		override val id = 1
	}
	
	/**
	  * The template adds scopes to the new token
	  * @since 01.05.2026
	  */
	case object Grant extends ScopeGrantType
	{
		// ATTRIBUTES	--------------------
		
		override val id = 2
	}
	
	/**
	  * The template restricts the parent token's scope when granting a new token
	  * @since 01.05.2026
	  */
	case object Restrict extends ScopeGrantType
	{
		// ATTRIBUTES	--------------------
		
		override val id = 3
	}
}

