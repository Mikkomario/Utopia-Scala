package utopia.firmament.context.color

import utopia.firmament.context.base.{StaticBaseContext, StaticBaseContextWrapper, VariableBaseContext}
import utopia.firmament.context.text.StaticTextContext
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorSet}

object StaticColorContext
{
	// OTHER    ----------------------------
	/**
	  * @param base Base context
	  * @param background Background color in this context
	  * @param textColor Custom text color to apply.
	  *                     - None (default) if default text color should be applied
	  *                     - Some(Left) for a custom color
	  *                     - Some(Right) for a custom color set, where the applied shade is based on this context
	  * @return A color context
	  */
	def apply(base: StaticBaseContext, background: Color, textColor: Option[Either[Color, ColorSet]] = None): StaticColorContext =
		_ColorContext(base, background, textColor)
	
	
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
		
		override def forTextComponents = StaticTextContext(this)
		override def current = this
		override def toVariableContext =
			VariableColorContext(base, Fixed(background), _textColor.map { _.mapBoth(Fixed.apply)(Fixed.apply) })
		
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
  * @since 01.10.2024, v1.4
  */
trait StaticColorContext
	extends StaticBaseContext with ColorContext with StaticColorContextLike[StaticColorContext, StaticTextContext]
