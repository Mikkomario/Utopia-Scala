package utopia.genesis.event

/**
 * These events are fired whenever the user types something on the keyboard. Unlike KeyState events,
 * these events focus less on the keyboard state changes and more on the characters typed by the user.
 * @author Mikko Hilpinen
 * @since 23.2.2017
  * @param typedChar The character typed by the user
  * @param index The key index associated with this event
  * @param keyStatus The keyboard status during this event
 */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
case class KeyTypedEvent(typedChar: Char, index: Int, keyStatus: KeyStatus)
{
	/**
	  * @return The digit typed, if the typed character was a digit. None otherwise.
	  */
	def digit = if (typedChar.isDigit) Some(typedChar.asDigit) else None
}
