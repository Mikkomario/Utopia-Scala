package utopia.genesis.event

/**
 * These events are fired whenever the user types something on the keyboard. Unlike Keystate events,
 * these events focus less on the keyboard state changes and more on the characters typed by the user.
 * @author Mikko Hilpinen
 * @since 23.2.2017
 */
case class KeyTypedEvent(typedChar: Char, keyStatus: KeyStatus)