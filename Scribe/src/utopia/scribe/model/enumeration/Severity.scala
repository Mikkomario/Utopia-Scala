package utopia.scribe.model.enumeration

import utopia.flow.operator.SelfComparable

import java.util.NoSuchElementException
import utopia.flow.util.CollectionExtensions._

/**
  * Common trait for all Severity values
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
sealed trait Severity extends SelfComparable[Severity]
{
	// ABSTRACT	--------------------
	
	/**
	  * Id used for this value in database / SQL
	  */
	def id: Int
	
	
	// IMPLEMENTED  ---------------
	
	override def repr = this
	
	override def compareTo(o: Severity) = id - o.id
}

object Severity
{
	// ATTRIBUTES	--------------------
	
	/**
	  * All available values of this enumeration
	  */
	val values: Vector[Severity] = Vector(Debug, Warning, Problem, Error, Critical)
	
	
	// COMPUTED ---------------------------------
	
	/**
	  * @return Smallest known severity
	  */
	def min = Debug
	/**
	  * @return Largest known severity
	  */
	def max = Critical
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Id representing a Severity
	  * @return Severity matching that id. None if the id didn't match any Severity
	  */
	def findForId(id: Int) = values.find { _.id == id }
	
	/**
	  * @param id Id matching a Severity
	  * @return Severity matching that id. Failure if no suitable value was found.
	  */
	def forId(id: Int) = 
		findForId(id).toTry { new NoSuchElementException(s"No value of Severity matches id '$id'") }
	
	
	// NESTED	--------------------
	
	/**
	  * A problem in the software that prevents the use thereof and must be fixed ASAP
	  */
	case object Critical extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val id = 5
	}
	
	/**
	  * Debugging logs that are purely informative and don't indicate a problem
	  */
	case object Debug extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val id = 1
	}
	
	/**
	  * A problem in the software that renders a portion of the service unavailable (high priority fix)
	  */
	case object Error extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val id = 4
	}
	
	/**
	  * A problem in the software that can be recovered from but which should be fixed when possible
	  */
	case object Problem extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val id = 3
	}
	
	/**
	  * Warnings that may indicate a problem but are often not necessary to act upon
	  */
	case object Warning extends Severity
	{
		// ATTRIBUTES	--------------------
		
		override val id = 2
	}
}

