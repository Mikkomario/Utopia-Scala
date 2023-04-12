package utopia.genesis.text

/**
  * FontStyle trait represents a font presentation style (bold, italicized etc.)
  * @author Mikko Hilpinen
  * @since 24.4.2019 - Moved from Reflection 10.4.2023, at v3.3
  */
trait FontStyle
{
	/**
	  * @return The java.awt.Font integer representation of this font style
	  */
	def toAwt: Int
}

object FontStyle
{
	// ATTRIBUTES	---------------------
	
	/**
	  * All basic font styles
	  */
	val values = Vector[FontStyle](Plain, Bold, Italic)
	
	/**
	  * Converts an awt font style to Reflection Font Style
	  * @param awtFontStyle Integer representing an awt font style
	  * @return A Font Style matching specified awt font style
	  */
	def fromAwt(awtFontStyle: Int) = values.find { _.toAwt == awtFontStyle }
	
	
	// VALUES   --------------------------
	
	/**
	  * The plain (default) font style
	  */
	case object Plain extends FontStyle
	{
		def toAwt = java.awt.Font.PLAIN
	}
	/**
	  * The bold font style
	  */
	case object Bold extends FontStyle
	{
		def toAwt = java.awt.Font.BOLD
	}
	/**
	  * The italic font style
	  */
	case object Italic extends FontStyle
	{
		def toAwt = java.awt.Font.ITALIC
	}
}
