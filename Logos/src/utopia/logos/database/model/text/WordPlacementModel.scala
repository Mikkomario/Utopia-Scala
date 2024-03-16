package utopia.logos.database.model.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.text.WordPlacementFactory
import utopia.logos.model.partial.text.WordPlacementData
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing WordPlacementModel instances and for inserting word placements to the database
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object WordPlacementModel
	extends DataInserter[WordPlacementModel, WordPlacement, WordPlacementData] with StatementLinkedModel
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains word placement statement id
	  */
	override val statementIdAttName = "statementId"
	/**
	  * Name of the property that contains word placement word id
	  */
	val wordIdAttName = "wordId"
	/**
	  * Name of the property that contains word placement order index
	  */
	override val orderIndexAttName = "orderIndex"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains word placement word id
	  */
	def wordIdColumn = table(wordIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = WordPlacementFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: WordPlacementData) = 
		apply(None, Some(data.statementId), Some(data.wordId), Some(data.orderIndex))
	
	override protected def complete(id: Value, data: WordPlacementData) = WordPlacement(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param id A word placement id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param orderIndex Index at which the specified word appears within the referenced statement (0-based)
	  * @return A model containing only the specified order index
	  */
	def withOrderIndex(orderIndex: Int) = apply(orderIndex = Some(orderIndex))
	
	/**
	  * @param statementId Id of the statement where the referenced word appears
	  * @return A model containing only the specified statement id
	  */
	def withStatementId(statementId: Int) = apply(statementId = Some(statementId))
	
	/**
	  * @param wordId Id of the word that appears in the described statement
	  * @return A model containing only the specified word id
	  */
	def withWordId(wordId: Int) = apply(wordId = Some(wordId))
}

/**
  * Used for interacting with WordPlacements in the database
  * @param id word placement database id
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class WordPlacementModel(id: Option[Int] = None, statementId: Option[Int] = None, 
	wordId: Option[Int] = None, orderIndex: Option[Int] = None) 
	extends StorableWithFactory[WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def factory = WordPlacementModel.factory
	
	override def valueProperties = {
		import WordPlacementModel._
		Vector("id" -> id, statementIdAttName -> statementId, wordIdAttName -> wordId, 
			orderIndexAttName -> orderIndex)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param orderIndex Index at which the specified word appears within the referenced statement (0-based)
	  * @return A new copy of this model with the specified order index
	  */
	def withOrderIndex(orderIndex: Int) = copy(orderIndex = Some(orderIndex))
	
	/**
	  * @param statementId Id of the statement where the referenced word appears
	  * @return A new copy of this model with the specified statement id
	  */
	def withStatementId(statementId: Int) = copy(statementId = Some(statementId))
	
	/**
	  * @param wordId Id of the word that appears in the described statement
	  * @return A new copy of this model with the specified word id
	  */
	def withWordId(wordId: Int) = copy(wordId = Some(wordId))
}

