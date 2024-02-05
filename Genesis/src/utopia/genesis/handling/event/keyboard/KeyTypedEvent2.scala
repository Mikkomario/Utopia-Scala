package utopia.genesis.handling.event.keyboard

/**
 * These events are fired whenever the user types something on the keyboard.
  * Unlike KeyState events, these events focus less on the keyboard state changes and more on the characters typed
  * by the user.
 * @author Mikko Hilpinen
 * @since 23.2.2017
  * @param typedChar The character typed by the user
  * @param index The key index associated with this event
  * @param keyboardState The state of the keyboard immediately after this event
 */
case class KeyTypedEvent2(typedChar: Char, index: Int, keyboardState: KeyboardState)
{
	/**
	  * @return The digit typed, if the typed character was a digit. None otherwise.
	  */
	def digit = if (typedChar.isDigit) Some(typedChar.asDigit) else None
	
	@deprecated("Replaced with keyBoardState")
	def keyStatus = keyboardState
}
