package utopia.logos.database.storable.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.word.WordPlacementDbFactory
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.factory.word.WordPlacementFactory
import utopia.logos.model.partial.word.WordPlacementData
import utopia.logos.model.stored.word.WordPlacement
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

/**
  * Used for constructing WordPlacementModel instances and for inserting word placements to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with WordPlacementDbModel", "v0.3")
object WordPlacementModel 
	extends StorableFactory[WordPlacementModel, WordPlacement, WordPlacementData] 
		with WordPlacementFactory[WordPlacementModel] with FromIdFactory[Int, WordPlacementModel]
		with StatementLinkedModel
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val statementId = property("statementId")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val wordId = property("wordId")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val orderIndex = property("orderIndex")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val style = property("styleId")
	
	/**
	  * Name of the property that contains word placement word id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val wordIdAttName = "wordId"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = WordPlacementDbFactory
	
	/**
	  * Column that contains word placement word id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def wordIdColumn = table(wordIdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	/**
	  * Name of the property that contains word placement statement id
	  */
	override val statementIdAttName = statementId.name
	/**
	  * Name of the property that contains word placement order index
	  */
	def orderIndexAttName = orderIndex.name
	
	override def apply(data: WordPlacementData) = 
		apply(None, Some(data.statementId), Some(data.wordId), Some(data.orderIndex), Some(data.style.id))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
	override protected def complete(id: Value, data: WordPlacementData) = WordPlacement(id.getInt, data)
	
	
	// OTHER	--------------------
	
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
	  * @param style Style in which this word is used in this context
	  * @return A model containing only the specified style
	  */
	def withStyle(style: DisplayStyle) = apply(style = Some(style.id))
	
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
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with WordPlacementDbModel", "v0.3")
case class WordPlacementModel(id: Option[Int] = None, statementId: Option[Int] = None, 
	wordId: Option[Int] = None, orderIndex: Option[Int] = None, style: Option[Int] = None) 
	extends StorableWithFactory[WordPlacement] with WordPlacementFactory[WordPlacementModel]
		with FromIdFactory[Int, WordPlacementModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = WordPlacementModel.factory
	
	override def valueProperties = {
		Vector("id" -> id, WordPlacementModel.statementId.name -> statementId, 
			WordPlacementModel.wordId.name -> wordId, WordPlacementModel.orderIndex.name -> orderIndex, 
			WordPlacementModel.style.name -> style)
	}
	
	override def withId(id: Int): WordPlacementModel = copy(id = Some(id))
	
	
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
	  * @param style Style in which this word is used in this context
	  * @return A model containing only the specified style
	  */
	def withStyle(style: DisplayStyle) = copy(style = Some(style.id))
	
	/**
	  * @param wordId Id of the word that appears in the described statement
	  * @return A new copy of this model with the specified word id
	  */
	def withWordId(wordId: Int) = copy(wordId = Some(wordId))
}

