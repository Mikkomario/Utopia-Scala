package utopia.firmament.model

import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.genesis.handling.event.keyboard.Key.{CharKey, Control}
import utopia.genesis.handling.event.keyboard.{Key, KeyboardState}

object HotKey
{
	/**
	  * @param key Key that triggers this hotkey
	  * @return A hotkey triggered by the specified key
	  */
	def apply(key: Key): HotKey = apply(Set(key))
	/**
	  * @param key1 First key that must be pressed
	  * @param key2 Second key that must also be pressed
	  * @param moreKeys Other keys that must also be pressed
	  * @return A hotkey triggered when all of the specified keys are pressed at once
	  */
	def apply(key1: Key, key2: Key, moreKeys: Key*): HotKey = apply(Set(key1, key2) ++ moreKeys)
	/**
	  * @param keyIndex Index of the triggering key
	  * @return A hot key that activates on specified key
	  */
	@deprecated("Please use .apply(Key) instead", "v1.3")
	def keyWithIndex(keyIndex: Int) = apply(Key(keyIndex))
	/**
	  * @param char Triggering character (key)
	  * @return A hot key that activates on specified character key press
	  */
	def character(char: Char) = apply(CharKey(char))
	
	/**
	  * @param char Triggering character (key)
	  * @return A hot key that activates on ctrl + specified character key
	  */
	def controlCharacter(char: Char) = apply(Control, CharKey(char))
	
	/**
	  * @param key Key that triggers this hotkey
	  * @param condition A function that must return true in order for this key to get triggered
	  * @return A new hotkey
	  */
	def conditional(key: Key)(condition: => Boolean) = apply(Set(key), condition = View(condition))
	/**
	  * @param keyIndex Index of the triggering key
	  * @param condition An additional trigger condition
	  * @return A hot key that activates on specified key, if the specified condition allows it.
	  */
	@deprecated("Please use .conditional(Key)(...) instead", "v1.3")
	def conditionalKeyWithIndex(keyIndex: Int)(condition: => Boolean) = conditional(Key(keyIndex))(condition)
	/**
	  * @param char Triggering character (key)
	  * @param condition An additional trigger condition
	  * @return A hot key that activates on specified character key, if the specified condition allows it.
	  */
	def conditionalCharacter(char: Char)(condition: => Boolean) = conditional(CharKey(char))(condition)
}

/**
  * Used for specifying keys that are used for triggering buttons / functions
  * @author Mikko Hilpinen
  * @since 1.3.2021, Reflection v1
  * @constructor Creates a new hotkey
  * @param keys Keys used for triggering this action.
  *                   If multiple indices or characters are included, all of them must be pressed simultaneously.
  * @param condition Additional condition that must be met in order for this hotkey to trigger
  * @param requiresWindowFocus Whether the applicable window should be the focused window (if possible)
  *                            in order for the hotkey action to be triggered.
  *                            Default = true.
  */
case class HotKey(keys: Set[Key] = Set(), condition: View[Boolean] = AlwaysTrue, requiresWindowFocus: Boolean = true)
{
	// COMPUTED -----------------------
	
	@deprecated("Please use .keys instead", "v1.3")
	def keyIndices = keys.map { _.index }
	
	/**
	  * @return Whether this hotkey may be triggered even when the applicable window doesn't have focus
	  */
	def triggersWithoutWindowFocus = !requiresWindowFocus
	/**
	  * @return Copy of this hotkey that may be triggered even when the applicable window doesn't have focus
	  */
	def triggeringWithoutWindowFocus = if (requiresWindowFocus) copy(requiresWindowFocus = false) else this
	
	
	// OTHER    -----------------------
	
	/**
	  * @param keys Currently active keys
	  * @return Whether this hot key (combination) is triggered with these keys.
	  *         Also evaluates the possible additional condition.
	  */
	def isTriggeredWith(keys: KeyboardState) = this.keys.forall(keys.apply) && condition.value
}
