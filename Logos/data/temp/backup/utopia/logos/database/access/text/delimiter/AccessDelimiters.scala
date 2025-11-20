package utopia.logos.database.access.text.delimiter

import utopia.logos.database.factory.text.DelimiterDbFactory
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessDelimiters extends AccessManyRoot[AccessDelimiters[Delimiter]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(DelimiterDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessDelimiters[_]): AccessDelimiterValues = access.values
}

/**
  * Used for accessing multiple delimiters from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessDelimiters[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessDelimiters[A], AccessDelimiter[A]] 
		with FilterDelimiters[AccessDelimiters[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible delimiters
	  */
	lazy val values = AccessDelimiterValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessDelimiters(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessDelimiter(target)
}

