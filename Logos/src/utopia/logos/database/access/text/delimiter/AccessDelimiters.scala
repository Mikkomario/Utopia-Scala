package utopia.logos.database.access.text.delimiter

import utopia.logos.database.reader.text.DelimiterDbReader
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessDelimiters extends AccessManyRoot[AccessDelimiterRows[Delimiter]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessDelimiterRows(AccessManyRows(DelimiterDbReader))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessDelimiters[_, _]): AccessDelimiterValues = access.values
}

/**
  * Used for accessing multiple delimiters from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessDelimiters[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessDelimiter[A]] with FilterDelimiters[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible delimiters
	  */
	lazy val values = AccessDelimiterValues(wrapped)
}

/**
  * Provides access to row-specific delimiter -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessDelimiterRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessDelimiters[A, AccessDelimiterRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessDelimiterRows[A], AccessDelimiter[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessDelimiterRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessDelimiter(target)
}

/**
  * Used for accessing delimiter items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedDelimiters[A](wrapped: TargetingMany[A]) 
	extends AccessDelimiters[A, AccessCombinedDelimiters[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedDelimiters[A], AccessDelimiter[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedDelimiters(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessDelimiter(target)
}

