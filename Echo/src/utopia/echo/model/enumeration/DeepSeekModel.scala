package utopia.echo.model.enumeration

import utopia.echo.model.llm.LlmDesignator
import utopia.flow.collection.immutable.Pair

/**
 * An enumeration for the different models used by DeepSeek
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.4.1
 */
sealed trait DeepSeekModel extends LlmDesignator

object DeepSeekModel
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * Both DeepSeek models (chat & reasoning)
	 */
	val values = Pair[DeepSeekModel](DeepSeekChat, DeepSeekReasoner)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param think Whether reasoning / thinking should be enabled
	 * @return A model for the specified use-case
	 */
	def apply(think: Boolean): DeepSeekModel = if (think) DeepSeekReasoner else DeepSeekChat
	
	
	// VALUES   --------------------------
	
	/**
	 * A DeepSeek model without reasoning capabilities
	 */
	case object DeepSeekChat extends DeepSeekModel
	{
		override val llmName: String = "deepseek-chat"
		override val thinks: Boolean = false
		
		override def thinking = DeepSeekReasoner
	}
	/**
	 * A reasoning-trained DeepSeek model
	 */
	case object DeepSeekReasoner extends DeepSeekModel
	{
		override val llmName: String = "deepseek-reasoner"
		override val thinks: Boolean = true
		
		override def thinking: LlmDesignator = this
	}
}
