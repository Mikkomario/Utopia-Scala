package utopia.logos.database.storable.text

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.LogosTables
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.factory.text.WordPlacementFactory
import utopia.logos.model.partial.text.WordPlacementData
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.model.immutable.DbPropertyDeclaration

/**
  * Used for constructing WordPlacementDbModel instances and for inserting word placements to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object WordPlacementDbModel 
	extends TextPlacementDbModelFactoryLike[WordPlacementDbModel, WordPlacement, WordPlacementData] 
		with WordPlacementFactory[WordPlacementDbModel] with TextPlacementDbProps
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	/**
	  * Database property used for interacting with statement ids
	  */
	lazy val statementId = property("statementId")
	/**
	  * Database property used for interacting with word ids
	  */
	lazy val wordId = property("wordId")
	/**
	  * Database property used for interacting with order indices
	  */
	override lazy val orderIndex = property("orderIndex")
	/**
	  * Database property used for interacting with styles
	  */
	lazy val style = property("styleId")
	
	
	// IMPLEMENTED	--------------------
	
	override def parentId = statementId
	override def placedId = wordId
	
	override def table = LogosTables.wordPlacement
	
	override def apply(data: WordPlacementData) = 
		apply(None, Some(data.statementId), Some(data.wordId), Some(data.orderIndex), Some(data.style))
	
	override def withId(id: Int) = apply(id = Some(id))
	/**
	  * @param orderIndex 0-based index that indicates the specific location of the placed text
	  * @return A model containing only the specified order index
	  */
	override def withOrderIndex(orderIndex: Int) = apply(orderIndex = Some(orderIndex))
	/**
	  * @param statementId Id of the statement where the referenced word appears
	  * @return A model containing only the specified statement id
	  */
	override def withStatementId(statementId: Int) = apply(statementId = Some(statementId))
	/**
	  * @param style Style in which this word is used in this context
	  * @return A model containing only the specified style
	  */
	override def withStyle(style: DisplayStyle) = apply(style = Some(style))
	/**
	  * @param wordId Id of the word that appears in the described statement
	  * @return A model containing only the specified word id
	  */
	override def withWordId(wordId: Int) = apply(wordId = Some(wordId))
	
	override protected def complete(id: Value, data: WordPlacementData) = WordPlacement(id.getInt, data)
}

/**
  * Used for interacting with WordPlacements in the database
  * @param id word placement database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class WordPlacementDbModel(id: Option[Int] = None, statementId: Option[Int] = None, 
	wordId: Option[Int] = None, orderIndex: Option[Int] = None, style: Option[DisplayStyle] = None) 
	extends TextPlacementDbModel with TextPlacementDbModelLike[WordPlacementDbModel] 
		with WordPlacementFactory[WordPlacementDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def dbProps = WordPlacementDbModel
	
	override def parentId = statementId
	override def placedId = wordId
	
	override def table = WordPlacementDbModel.table
	
	override def valueProperties = 
		super[TextPlacementDbModelLike].valueProperties ++
			Single(WordPlacementDbModel.style.name -> style.map[Value] { e => e.id }.getOrElse(Value.empty))
	
	/**
	  * @param id Id to assign to the new model (default = currently assigned id)
	  * @param parentId parent id to assign to the new model (default = currently assigned value)
	  * @param placedId placed id to assign to the new model (default = currently assigned value)
	  * @param orderIndex order index to assign to the new model (default = currently assigned value)
	  */
	override def copyTextPlacement(id: Option[Int] = id, parentId: Option[Int] = parentId, 
		placedId: Option[Int] = placedId, orderIndex: Option[Int] = orderIndex) = 
		copy(id = id, statementId = parentId, wordId = placedId, orderIndex = orderIndex)
	/**
	  * @param statementId Id of the statement where the referenced word appears
	  * @return A new copy of this model with the specified statement id
	  */
	override def withStatementId(statementId: Int) = copy(statementId = Some(statementId))
	/**
	  * @param style Style in which this word is used in this context
	  * @return A new copy of this model with the specified style
	  */
	override def withStyle(style: DisplayStyle) = copy(style = Some(style))
	/**
	  * @param wordId Id of the word that appears in the described statement
	  * @return A new copy of this model with the specified word id
	  */
	override def withWordId(wordId: Int) = copy(wordId = Some(wordId))
}

