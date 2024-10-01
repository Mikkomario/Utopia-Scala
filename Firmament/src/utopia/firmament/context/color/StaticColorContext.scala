package utopia.firmament.context.color

import utopia.firmament.context.base.{StaticBaseContext, StaticBaseContextWrapper}
import utopia.flow.util.EitherExtensions._
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorSet}

object StaticColorContext
{
	// OTHER    ----------------------------
	/**
	  * @param base Base context
	  * @param background Background color in this context
	  * @return A color context
	  */
	def apply(base: StaticBaseContext, background: Color): StaticColorContext =
		_ColorContext(base, background, None)
	
	
	// NESTED   ----------------------------
	
	private case class _ColorContext(base: StaticBaseContext, background: Color,
	                                 _textColor: Option[Either[Color, ColorSet]])
		extends StaticColorContext with StaticBaseContextWrapper[StaticBaseContext, StaticColorContext]
	{
		// ATTRIBUTES   --------------------------
		
		override lazy val textColor: Color = {
			_textColor match {
				// Case: Custom color defined
				case Some(color) =>
					color.leftOrMap { set =>
						// If the custom color is defined as a set, finds the best variant for the current background
						this.color.forText(set)
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
		
		override def self = this
		
		// FIXME: Implement
		override def forTextComponents = ???
		
		override def withDefaultTextColor = if (_textColor.isDefined) copy(_textColor = None) else this
		
		override def withTextColor(color: Color) = copy(_textColor = Some(Left(color)))
		override def withTextColor(color: ColorSet) = copy(_textColor = Some(Right(color)))
		
		override def withBase(baseContext: StaticBaseContext) =
			if (base == baseContext) this else copy(base = baseContext)
		
		override def against(background: Color) =
			if (background == this.background) this else copy(background = background)
		
		override def *(mod: Double) = copy(base = base * mod)
	}
}

/**
  * Common trait for fixed color context implementations. Removes generic types from [[StaticColorContextLike]].
  * @author Mikko Hilpinen
  * @since 01.10.2024, v1.3.1
  */
// TODO: Change Textual type
trait StaticColorContext
	extends StaticBaseContext with ColorContext2 with StaticColorContextLike[StaticColorContext, StaticColorContext]
