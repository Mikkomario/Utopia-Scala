package utopia.logos.database.access.text.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on delimiter properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterDelimiters[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines delimiter database properties
	  */
	def delimiterModel = DelimiterDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param text text to target
	  * @return Copy of this access point that only includes delimiters with the specified text
	  */
	def matching(text: String) = filter(delimiterModel.text.column <=> text)
	
	/**
	  * @param text Targeted text
	  * @return Copy of this access point that only includes delimiters where text is within the specified 
	  * value set
	  */
	def matchingDelimiters(text: Iterable[String]) = filter(delimiterModel.text.column.in(text))
}

