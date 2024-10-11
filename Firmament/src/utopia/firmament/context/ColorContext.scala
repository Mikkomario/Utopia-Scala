package utopia.firmament.context

import utopia.flow.util.EitherExtensions._
import utopia.paradigm.color.ColorShade._
import utopia.paradigm.color.{Color, ColorSet}

@deprecated("Replaced with a new version", "v1.4")
object ColorContext
{
	// OTHER    ----------------------------
	
	/**
	  * @param base Base context
	  * @param background Background color in this context
	  * @return A color context
	  */
	def apply(base: BaseContext, background: Color): ColorContext = _ColorContext(base, background, None)
	
	
	// NESTED   ----------------------------
	
	private case class _ColorContext(base: BaseContext, background: Color, _textColor: Option[Either[Color, ColorSet]])
		extends ColorContext
	{
		// ATTRIBUTES   --------------------------
		
		override lazy val textColor: Color = {
			_textColor match {
				// Case: Custom color defined
				case Some(color) =>
					color.leftOrMap { set =>
						// If the custom color is defined as a set, finds the best variant for the current background
						set.against(background,
							minimumContrast = contrastStandard.minimumContrastForText(font.sizeOnScreen, font.isBold))
					}
				// Case: No custom color defined => Uses black or white color
				case None =>
					background.shade match {
						case Dark => Color.textWhite
						case Light => Color.textBlack
					}
			}
		}
		
		
		// IMPLEMENTED  --------------------------
		
		override def self: ColorContext = this
		
		override def withDefaultTextColor: ColorContext = if (_textColor.isDefined) copy(_textColor = None) else this
		
		override def withTextColor(color: Color): ColorContext = copy(_textColor = Some(Left(color)))
		override def withTextColor(color: ColorSet): ColorContext = copy(_textColor = Some(Right(color)))
		
		override def withBase(baseContext: BaseContext): ColorContext =
			if (base == baseContext) this else copy(base = baseContext)
		
		override def against(background: Color): ColorContext =
			if (background == this.background) this else copy(background = background)
		
		override def *(mod: Double): ColorContext = copy(base = base * mod)
		
		override def forTextComponents: TextContext = TextContext(this)
	}
}

/**
  * This is a more specific instance of base context that also includes information about surrounding container's
  * background color
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
@deprecated("Replaced with a new version", "v1.4")
trait ColorContext extends ColorContextLike[ColorContext, TextContext] with BaseContext