package utopia.logos.database.access.text.word

import placement.{AccessWordPlacementValues, FilterByWordPlacement}
import utopia.logos.database.LogosTables
import utopia.logos.database.reader.text.{StatedWordDbReader, WordDbReader}
import utopia.logos.model.stored.text.StoredWord
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessWords extends AccessManyRoot[AccessWordRows[StoredWord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessWordRows(AccessManyRows(WordDbReader))
	
	/**
	  * Access to words in the DB, also including word placement information
	  */
	lazy val withUseCases = AccessWordRows(AccessManyRows(StatedWordDbReader))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessWords[_, _]): AccessWordValues = access.values
}

/**
  * Used for accessing multiple words from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessWords[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessWord[A]] with FilterWords[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible words
	  */
	lazy val values = AccessWordValues(wrapped)
	
	/**
	  * A copy of this access which also targets word_placement
	  */
	lazy val joinedToUseCases = join(LogosTables.wordPlacement)
	
	/**
	  * Access to the values of linked word placements
	  */
	lazy val useCases = AccessWordPlacementValues(joinedToUseCases)
	
	/**
	  * Access to word placement -based filtering functions
	  */
	lazy val whereUseCases = FilterByWordPlacement(joinedToUseCases)
}

/**
  * Provides access to row-specific word -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessWordRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessWords[A, AccessWordRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessWordRows[A], AccessWord[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessWordRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessWord(target)
}

/**
  * Used for accessing word items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedWords[A](wrapped: TargetingMany[A]) 
	extends AccessWords[A, AccessCombinedWords[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedWords[A], AccessWord[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedWords(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessWord(target)
}

