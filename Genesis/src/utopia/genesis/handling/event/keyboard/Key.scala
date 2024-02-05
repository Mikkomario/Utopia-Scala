package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.enumeration.Direction2D.{Down, Up}

/**
  * An enumeration for keyboard keys
  * @author Mikko Hilpinen
  * @since 03/02/2024, v3.6
  */
trait Key extends EqualsBy
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The index of this key (from [[java.awt.event.KeyEvent]])
	  */
	def index: Int
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def equalsProperties: Iterable[Any] = Some(index)
}

object Key
{
	// OTHER    ------------------------
	
	/**
	  * @param index A key index
	  * @return A key wrapper for that index
	  */
	def apply(index: Int): Key = _Key(index)
	
	
	// NESTED   ------------------------
	
	case object Enter extends CharKey {
		override val char: Char = '\n'
		override val index: Int = super.index
	}
	case object BackSpace extends CharKey {
		override val char = '\b'
		override val index = super.index
	}
	case object Tab extends CharKey {
		override val char: Char = '\t'
		override val index: Int = super.index
	}
	case object Space extends CharKey {
		override val char: Char = ' '
		override val index: Int = super.index
	}
	
	case object Esc extends Key {
		override val index: Int = 0x1B
	}
	case object PageUp extends Key {
		override val index: Int = 0x21
	}
	case object PageDown extends Key {
		override val index: Int = 0x22
	}
	case object Home extends Key {
		override val index: Int = 0x24
	}
	case object End extends Key {
		override val index: Int = 0x23
	}
	case object Delete extends Key {
		override val index: Int = 0x7F
	}
	case object PrintScreen extends Key {
		override val index: Int = 0x9A
	}
	
	case object Shift extends Key {
		override val index: Int = 0x10
	}
	case object Control extends Key {
		override val index: Int = 0x11
	}
	case object Alt extends Key {
		override val index: Int = 0x12
	}
	
	case object CapsLock extends Key {
		override val index: Int = 0x14
	}
	case object NumLock extends Key {
		override val index: Int = 0x90
	}
	case object Insert extends Key {
		override val index: Int = 0x9B
	}
	
	case object BackSlash extends CharKey {
		override val char: Char = '\\'
		override val index: Int = 0x5C
	}
	case object LessThan extends CharKey {
		override val char: Char = '<'
		override val index = 0x99
	}
	case object GreaterThan extends CharKey {
		override val char: Char = '>'
		override val index = 0xa0
	}
	
	object ArrowKey
	{
		// ATTRIBUTES   --------------------------
		
		/**
		  * All 4 arrow keys
		  */
		val values = Vector[ArrowKey](UpArrow, DownArrow, LeftArrow, RightArrow)
		
		
		// OTHER    ------------------------------
		
		/**
		  * @param direction Arrow direction
		  * @return Arrow key that matches that direction
		  */
		def apply(direction: Direction2D): ArrowKey = direction match {
			case Up => UpArrow
			case Down => DownArrow
			case Direction2D.Left => LeftArrow
			case Direction2D.Right => RightArrow
		}
		
		/**
		  * @param index A key index
		  * @param includeNumpad Whether to count/include numpad-arrows (default = false)
		  * @return Arrow key that matches that index. None if no arrow key matches that index.
		  */
		def apply(index: Int, includeNumpad: Boolean = false) = {
			if (includeNumpad)
				values.find { k => k.index == index || k.onNumpad.index == index }
			else
				values.find { _.index == index }
		}
	}
	trait ArrowKey extends Key
	{
		// ABSTRACT ---------------------
		
		/**
		  * @return Direction this arrow faces
		  */
		def direction: Direction2D
		
		/**
		  * @return A key that matches this arrow on the num-pad
		  */
		def onNumpad: Key
		
		
		// COMPUTED --------------------
		
		/**
		  * @return Arrow key that points to the opposite direction from this one
		  */
		def opposite = ArrowKey(direction.opposite)
	}
	case object LeftArrow extends ArrowKey {
		override val direction: Direction2D = Direction2D.Left
		override val index: Int = 0x25
		
		override def onNumpad: Key = Key(0xE2)
	}
	case object RightArrow extends ArrowKey {
		override val direction: Direction2D = Direction2D.Right
		override val index: Int = 0x27
		
		override def onNumpad: Key = Key(0xE3)
	}
	case object UpArrow extends ArrowKey {
		override val direction: Direction2D = Direction2D.Up
		override val index: Int = 0x26
		
		override def onNumpad: Key = Key(0xE0)
	}
	case object DownArrow extends ArrowKey {
		override val direction: Direction2D = Direction2D.Down
		override val index: Int = 0x28
		
		override def onNumpad: Key = Key(0xE1)
	}
	
	object CharKey
	{
		// OTHER    ---------------------------
		
		/**
		  * @param char A character
		  * @return A key matching that character
		  */
		// The letter keys are identified with upper key characters
		def apply(char: Char): CharKey = _CharKey(if (char.isLetter) char.toUpper else char)
		
		
		// NESTED   --------------------------
		
		private case class _CharKey(char: Char) extends CharKey
		{
			override val index = super.index
		}
	}
	/**
	  * Common trait for character-based keys, like letter keys.
	  * Not all character keys follow this simple indexing logic, however.
	  */
	trait CharKey extends Key
	{
		// ABSTRACT --------------------------
		
		def char: Char
		
		
		// IMPLEMENTED  ----------------------
		
		override def index = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(char)
	}
	
	case class DigitKey(digit: Byte) extends CharKey
	{
		// CHECKS   --------------------------
		
		if (digit < 0 || digit > 9)
			throw new IndexOutOfBoundsException(s"Invalid digit key: $digit")
			
		
		// COMPUTED --------------------------
		
		/**
		  * @return A key that corresponds to this digit on the num-pad
		  */
		def onNumpad = Key(index + 48)
		
		
		// IMPLEMENTED  ----------------------
		
		override val char: Char = digit.toString.head
		override val index = super.index
	}
	
	case class FunctionKey(functionIndex: Byte) extends Key
	{
		override val index: Int = (if (functionIndex < 13) 111 else 61439) + functionIndex
	}
	
	private case class _Key(index: Int) extends Key
}