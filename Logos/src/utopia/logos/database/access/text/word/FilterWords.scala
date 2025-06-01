package utopia.logos.database.access.text.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.WordDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on word properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterWords[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines word database properties
	  */
	def wordModel = WordDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param text text to target
	  * @return Copy of this access point that only includes words with the specified text
	  */
	def matching(text: String) = filter(wordModel.text.column <=> text)
	
	/**
	  * @param text Targeted text
	  * @return Copy of this access point that only includes words where text is within the specified value 
	  * set
	  */
	def matchingWords(text: Iterable[String]) = filter(wordModel.text.column.in(text))
}

