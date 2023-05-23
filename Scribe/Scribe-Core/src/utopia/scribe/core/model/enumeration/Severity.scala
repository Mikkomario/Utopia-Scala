package utopia.scribe.core.model.enumeration

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible

/**
  * Represents the level of severity associated with some problem or error situation
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
sealed trait Severity extends ValueConvertible
{
	// ABSTRACT	--------------------
	
	/**
	  * level used to represent this severity in database and json
	  */
	def level: Int
	
	
	// IMPLEMENTED	--------------------
	
	override def toValue = level
}

object Severity
{
	// ATTRIBUTES	--------------------
	
	/**
	  * All available severity values
	  */
	val values: Vector[Severity] = Vector(Debug, Info, Warning, Recoverable, Unrecoverable, Critical)
	
	
	// COMPUTED	--------------------
	
	/**
	  * The default severity (i.e. unrecoverable)
	  */
	def default = Unrecoverable
	
	
	// OTHER	--------------------
	
	/**
	  * @param level level representing a severity
	  * @return severity matching the specified level. None if the level didn't match any severity
	  */
	def findForLevel(level: Int) = values.find { _.level == level }
	
	/**
	  * @param level level matching a severity
	  * @return severity matching that level, or the default severity (unrecoverable)
	  */
	def forLevel(level: Int) = findForLevel(level).getOrElse(default)
	
	/**
	  * @param value A value representing an severity level
	  * @return severity matching the specified value, when the value is interpreted as an severity level, 
	  * or the default severity (unrecoverable)
	  */
	def fromValue(value: Value) = forLevel(value.getInt)
	
	
	// NESTED	--------------------
	
	/**
	  * Represents a failure that severely or entirely disables the program's intended behavior.
	  * Should be resolved as soon as possible.
	  * @since 22.05.2023
	  */
	case object Critical extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val level = 6
	}
	
	/**
	  * An entry used for debugging purposes only. Practically insignificant.
	  * @since 22.05.2023
	  */
	case object Debug extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val level = 1
	}
	
	/**
	  * 
		Information about the application's state and/or behavior which may be of use. Doesn't necessarily indicate
	  *  a real problem.
	  * @since 22.05.2023
	  */
	case object Info extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val level = 2
	}
	
	/**
	  * 
		Indicates a process failure which is either partial or which may possibly be recovered from automatically.
	  * Doesn't require immediate action, but may be important to review and fix eventually.
	  * @since 22.05.2023
	  */
	case object Recoverable extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val level = 4
	}
	
	/**
	  * Represents a failure that prematurely terminated some process in a way that progress or data was lost
	  *  or halted.
	  * Typically the program performance is immediately affected by these kinds of problems.
	  * @since 22.05.2023
	  */
	case object Unrecoverable extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val level = 5
	}
	
	/**
	  * 
		Information about the application's state and/or behavior which probably indicates a presence of a problem.
	  * Doesn't necessarily require action.
	  * @since 22.05.2023
	  */
	case object Warning extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val level = 3
	}
}

