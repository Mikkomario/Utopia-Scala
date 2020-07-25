package utopia.metropolis.model.enumeration

import utopia.flow.util.RichComparable
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.NoSuchTypeException

/**
  * An enumeration for different levels of language familiarity
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
// TODO: Replace this class with a model read from the database (no more hard-coded options)
// TODO: Add an Exodus rest node for these as well
sealed trait LanguageFamiliarity extends RichComparable[LanguageFamiliarity]
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return Id of this familiarity level
	  */
	def id: Int
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return Ordering id of this level (lower order means stronger familiarity)
	  */
	def orderIndex = id
	
	
	// IMPLEMENTED	-----------------------------
	
	override def compareTo(o: LanguageFamiliarity) = o.orderIndex - orderIndex
}

object LanguageFamiliarity
{
	/**
	  * All introduced familiarity levels
	  */
	val values = Vector[LanguageFamiliarity](PrimaryLanguage, FluentPreferred, Fluent, OK, OKNotPreferred, Bad)
	
	/**
	  * @param familiarityId Language familiarity id
	  * @return Familiarity level matching specified id
	  */
	def forId(familiarityId: Int) = values.find { _.id == familiarityId }.toTry {
		new NoSuchTypeException(s"No language familiarity level matching id $familiarityId") }
	
	/**
	  * Used with the most preferred / native languages
	  */
	case object PrimaryLanguage extends LanguageFamiliarity
	{
		override val id = 1
	}
	
	/**
	  * Used with preferred fluent languages
	  */
	case object FluentPreferred extends LanguageFamiliarity
	{
		override val id = 2
	}
	
	/**
	  * Used with fluent languages
	  */
	case object Fluent extends LanguageFamiliarity
	{
		override val id = 3
	}
	
	/**
	  * Used when the user has some skill in specified language
	  */
	case object OK extends LanguageFamiliarity
	{
		override val id = 4
	}
	
	/**
	  * Used when the user is somewhat skilled in the specified language, but doesn't prefer to use it
	  */
	case object OKNotPreferred extends LanguageFamiliarity
	{
		override val id = 5
	}
	
	/**
	  * Used when the user is not skilled in a language, but is able to use it in some situations
	  */
	case object Bad extends LanguageFamiliarity
	{
		override val id = 6
	}
}
