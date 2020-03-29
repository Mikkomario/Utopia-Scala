package utopia.reflection.text

import utopia.flow.util.FileExtensions._

import scala.language.implicitConversions
import java.awt
import java.awt.GraphicsEnvironment
import java.io.FileNotFoundException
import java.nio.file.Path

import utopia.reflection.text.FontStyle.{Bold, Italic, Plain}

import scala.util.{Failure, Try}

object Font
{
	// Implicitly converts an awt font to Reflection font
	implicit def awtFontToFont(awtFont: java.awt.Font): Font = Font(awtFont.getName, awtFont.getSize,
		FontStyle.fromAwt(awtFont.getStyle).getOrElse(Plain))
	
	/**
	  * Loads and registers specified font from the file system. After loading the font, it may be constructed
	  * normally as well
	  * @param fontFilePath A path from which the font will be loaded
	  * @param newFontSize The size to use for the newly created font
	  * @return The newly loaded font
	  */
	def load(fontFilePath: Path, newFontSize: Int) =
	{
		if (fontFilePath.notExists)
			Failure(new FileNotFoundException(s"No font file exists at $fontFilePath"))
		else
		{
			Try
			{
				val font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFilePath.toFile)
				GraphicsEnvironment.getLocalGraphicsEnvironment.registerFont(font)
				awtFontToFont(font).copy(baseSize = newFontSize)
			}
		}
	}
}

/**
  * This is a wrapper for awt font, but also supports scaling
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  * @param name The name of this font
  * @param baseSize The base size for this font (adjusted by scaling factors)
  * @param style The style of this font
  * @param scaling The instance level scaling of this font (affects final font size)
  */
case class Font(name: String, baseSize: Int, style: FontStyle = FontStyle.Plain, scaling: Double = 1.0)
{
	// ATTRIBUTES	-------------------
	
	/**
	  * The awt representation of this font
	  */
	lazy val toAwt = new awt.Font(name, style.toAwt, (baseSize * scaling).toInt)
	
	
	// COMPUTED	-----------------------
	
	/**
	 * @return A copy of this font with plain style
	 */
	def plain = withFontStyle(Plain)
	
	/**
	 * @return A bold copy of this font
	 */
	def bold = withFontStyle(Bold)
	
	/**
	 * @return An italic copy of this font
	 */
	def italic = withFontStyle(Italic)
	
	
	// OPERATORS	-------------------
	
	/**
	  * @param scalingMod A scaling modifier
	  * @return A scaled version of this font
	  */
	def *(scalingMod: Double) = copy(scaling = scaling * scalingMod)
	
	/**
	  * @param div A division modifier
	  * @return A divided (downscaled) version of this font
	  */
	def /(div: Double) = copy(scaling = scaling / div)
	
	
	// OTHER	-----------------------
	
	/**
	 * @param newStyle New font style
	 * @return A copy of this font with specified style
	 */
	def withFontStyle(newStyle: FontStyle) = if (style == newStyle) this else copy(style = newStyle)
	
	/**
	 * @param fontSize New font size
	 * @return A copy of this font with specified size
	 */
	def withSize(fontSize: Int) = copy(baseSize = fontSize, scaling = 1.0)
}
