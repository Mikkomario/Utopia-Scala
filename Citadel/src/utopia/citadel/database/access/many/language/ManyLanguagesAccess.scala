package utopia.citadel.database.access.many.language

import utopia.citadel.database.access.many.description.{DbLanguageDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.language.LanguageFactory
import utopia.citadel.database.model.language.LanguageModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyLanguagesAccess extends ViewFactory[ManyLanguagesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyLanguagesAccess = new _ManyLanguagesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyLanguagesAccess(condition: Condition) extends ManyLanguagesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple Languages at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyLanguagesAccess 
	extends ManyRowModelAccess[Language] with ManyDescribedAccess[Language, DescribedLanguage] 
		with FilterableView[ManyLanguagesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * isoCodes of the accessible Languages
	  */
	def isoCodes(implicit connection: Connection) = 
		pullColumn(model.isoCodeColumn).flatMap { value => value.string }
	
	/**
	  * creationTimes of the accessible Languages
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LanguageModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageFactory
	
	override protected def describedFactory = DescribedLanguage
	
	override protected def manyDescriptionsAccess = DbLanguageDescriptions
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyLanguagesAccess = ManyLanguagesAccess(condition)
	
	override def idOf(item: Language) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Language instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Language instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the isoCode of the targeted Language instance(s)
	  * @param newIsoCode A new isoCode to assign
	  * @return Whether any Language instance was affected
	  */
	def isoCodes_=(newIsoCode: String)(implicit connection: Connection) = 
		putColumn(model.isoCodeColumn, newIsoCode)
	
	/**
	  * @param isoCodes Language ISO codes
	  * @param connection Implicit DB Connection
	  * @return Languages matching those iso-codes
	  */
	def withIsoCodes(isoCodes: Iterable[String])(implicit connection: Connection) = 
		find(model.isoCodeColumn in isoCodes)
}

