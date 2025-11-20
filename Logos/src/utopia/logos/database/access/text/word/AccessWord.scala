package utopia.logos.database.access.text.word

import placement.{AccessWordPlacementValue, FilterByWordPlacement}
import utopia.logos.database.LogosTables
import utopia.logos.model.stored.text.StoredWord
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessWord extends AccessOneRoot[AccessWord[StoredWord]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessWords.root.head
	
	/**
	  * Access to individual words in the DB, also including word placement information
	  */
	lazy val withUseCase = AccessWords.withUseCases.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessWord[_]): AccessWordValue = access.values
}

/**
  * Used for accessing individual words from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessWord[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessWord[A]] with FilterWords[AccessWord[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible word
	  */
	lazy val values = AccessWordValue(wrapped)
	
	/**
	  * A copy of this access which also targets word_placement
	  */
	lazy val joinedToUseCase = join(LogosTables.wordPlacement)
	
	/**
	  * Access to the values of linked word placement
	  */
	lazy val useCase = AccessWordPlacementValue(joinedToUseCase)
	
	/**
	  * Access to word placement -based filtering functions
	  */
	lazy val whereUseCase = FilterByWordPlacement(joinedToUseCase)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessWord(newTarget)
}

