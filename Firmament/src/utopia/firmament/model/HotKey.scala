package utopia.firmament.model

import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.genesis.event.KeyStatus

import java.awt.event.KeyEvent

object HotKey
{
	/**
	  * @param keyIndex Index of the triggering key
	  * @return A hot key that activates on specified key
	  */
	def keyWithIndex(keyIndex: Int) = apply(Set(keyIndex))
	
	/**
	  * @param char Triggering character (key)
	  * @return A hot key that activates on specified character key press
	  */
	def character(char: Char) = apply(characters = Set(char))
	
	/**
	  * @param char Triggering character (key)
	  * @return A hot key that activates on ctrl + specified character key
	  */
	def controlCharacter(char: Char) = apply(Set(KeyEvent.VK_CONTROL), Set(char))
	
	/**
	  * @param keyIndex Index of the triggering key
	  * @param condition An additional trigger condition
	  * @return A hot key that activates on specified key, if the specified condition allows it.
	  */
	def conditionalKeyWithIndex(keyIndex: Int)(condition: => Boolean) =
		apply(Set(keyIndex), condition = View(condition))
	
	/**
	  * @param char Triggering character (key)
	  * @param condition An additional trigger condition
	  * @return A hot key that activates on specified character key, if the specified condition allows it.
	  */
	def conditionalCharacter(char: Char)(condition: => Boolean) =
		apply(characters = Set(char), condition = View(condition))
}

/**
  * Used for specifying keys that are used for triggering buttons / functions
  * @author Mikko Hilpinen
  * @since 1.3.2021, Reflection v1
  */
case class HotKey(keyIndices: Set[Int] = Set(), characters: Set[Char] = Set(),
				  condition: View[Boolean] = AlwaysTrue)
{
	/**
	  * @param keys Currently active keys
	  * @return Whether this hot key (combination) is triggered with these keys.
	  *         Also evaluates the possible additional condition.
	  */
	def isTriggeredWith(keys: KeyStatus) = keyIndices.forall(keys.apply) && characters.forall(keys.apply) &&
		condition.value
}
