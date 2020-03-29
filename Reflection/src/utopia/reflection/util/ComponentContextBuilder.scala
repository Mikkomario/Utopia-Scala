package utopia.reflection.util

import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.{Axis2D, LinearAcceleration}
import utopia.genesis.util.Drawer
import utopia.reflection.color.ComponentColor
import utopia.reflection.container.stack.{ScrollAreaLike, ScrollBarDrawer}
import utopia.reflection.shape.{Alignment, Border, ScrollBarBounds, StackInsets, StackLength}
import utopia.reflection.text.Font

/**
  * Used for configuring component style at creation time
  * @author Mikko Hilpinen
  * @since 4.8.2019, v1+
 *  @param actorHandler Handler that relays action events to components (required)
 *  @param font The font used in most components (required)
 *  @param highlightColor A color used for highlighting certain values in some components (required)
 *  @param focusColor A color used for specifying user focus (required)
 *  @param normalWidth Standard width of components whose size is not determined by text only (required)
 * @param textColor Color used in all text (default = slightly opaque black)
 * @param promptFont Font used in prompts (default = normally used font)
 * @param promptTextColor Text color used in prompts (default = normal text color with lower alpha value)
 * @param textHasMinWidth Whether text shouldn't be cut when resizing components (default = true)
 * @param textAlignment Alignment used for most text-based components (default = left alignment)
 * @param background Background color used for nearly all components, if any (default = None = transparent background)
 * @param barBackground Background color for bars (scroll, progress, etc.) (default = specified background color or
 *                      gray if that's not specified either)
 * @param insets Insets placed inside components that support them (default = any, preferring zero)
 * @param border Border used for nearly all components (default = None = no special border)
 * @param borderWidth Border width used for some components that create their own borders
 *                    (default = width of overall border or half of inside margins (with minimum of 4 px))
 * @param stackMargin Margin used in almost all stacks (default = any margin, preferring zero)
 * @param relatedItemsStackMargin Margin used for items that are considered closely related (default = specified overall stack margin)
 * @param stackCap Cap at each end of each stack (default = fixed to zero)
 * @param dropDownWidthLimit Maximum width for drop down fields, if any (default = None = no limit)
 * @param switchWidth Width used for switch components (default = any width, preferring 1/4 of specified normal width)
 * @param textFieldWidth Width used for text field components (default = any width, preferring specified normal width)
 * @param scrollPerWheelClick How many pixels each "click" of mouse wheel represents (default = 32 px)
 * @param scrollBarWidth Width of scroll bars (default = 24 px)
 * @param scrollBarDrawer Drawer used for drawing scroll bars (default = box drawer using highlight color and gray
 *                        background if bar is outside content and slightly transparent gray bar if is inside content)
 * @param scrollBarIsInsideContent Whether scroll bar should be drawn inside the content (default = false)
 * @param scrollFriction Friction used when drag-scrolling (default = default scroll area friction (2000 px/s&#94;2))
 * @param allowImageUpscaling Whether images should be allowed to scale above their original source resolution (default = false)
  */
case class ComponentContextBuilder(actorHandler: ActorHandler, font: Font, highlightColor: Color, focusColor: Color,
								   normalWidth: Int, textColor: Color = Color.textBlack, promptFont: Option[Font] = None,
								   promptTextColor: Option[Color] = None, textHasMinWidth: Boolean = true,
								   textAlignment: Alignment = Alignment.Left, background: Option[Color] = None,
								   barBackground: Option[Color] = None, insets: StackInsets = StackInsets.any,
								   border: Option[Border] = None, borderWidth: Option[Double] = None,
								   stackMargin: StackLength = StackLength.any, relatedItemsStackMargin: Option[StackLength] = None,
								   stackCap: StackLength = StackLength.fixed(0), dropDownWidthLimit: Option[Int] = None,
								   switchWidth: Option[StackLength] = None, textFieldWidth: Option[StackLength] = None,
								   scrollPerWheelClick: Double = 32, scrollBarWidth: Int = 24,
								   scrollBarDrawer: Option[ScrollBarDrawer] = None, scrollBarIsInsideContent: Boolean = false,
								   scrollFriction: LinearAcceleration = ScrollAreaLike.defaultFriction,
								   allowImageUpscaling: Boolean = false)
{
	// ATTRIBUTES	-------------------------
	
	/**
	 * A complete context based on this builder
	 */
	lazy val result = ComponentContext(actorHandler, font, highlightColor, focusColor, normalWidth, textColor,
		promptFont.getOrElse(font), promptTextColor.getOrElse(textColor.timesAlpha(0.625)), textHasMinWidth,
		textAlignment, background, barBackground.orElse(background).getOrElse(Color.gray(0.5)), insets,
		border, borderWidth.orElse(border.map { _.insets.average }).getOrElse(
			((insets.optimal.horizontal / 2) min (insets.optimal.vertical / 2)) max 4),
		stackMargin, relatedItemsStackMargin.getOrElse(stackMargin), stackCap, dropDownWidthLimit,
		switchWidth.getOrElse(StackLength.any(normalWidth / 4)), textFieldWidth.getOrElse(StackLength.any(normalWidth)),
		scrollPerWheelClick, scrollBarWidth, scrollBarDrawer.getOrElse(new DefaultScrollBarDrawer),
		scrollBarIsInsideContent, scrollFriction, allowImageUpscaling)
	
	
	// COMPUTED	-----------------------------
	
	def withTextMinWidth = copy(textHasMinWidth = true)
	
	def withNoTextMinWidth = copy(textHasMinWidth = false)
	
	def transparent = copy(background = None)
	
	def WithNoBorder = copy(border = None)
	
	def withScrollBarInsideContent = copy(scrollBarIsInsideContent = true)
	
	def withScrollBarOutsideContent = copy(scrollBarIsInsideContent = false)
	
	def withImageUpscaling = copy(allowImageUpscaling = true)
	
	def withNoImageUpscaling = copy(allowImageUpscaling = false)
	
	
	// OTHER	-----------------------------
	
	def withFont(font: Font) = copy(font = font)
	
	def withScaledFont(scaling: Double) = copy(font = font * scaling)
	
	def withHighlightColor(color: Color) = copy(highlightColor = color)
	
	def withFocusColor(color: Color) = copy(focusColor = color)
	
	def withTextColor(color: Color) = copy(textColor = color)
	
	def withPromptFont(font: Font) = copy(promptFont = Some(font))
	
	def withPromptTextColor(color: Color) = copy(promptTextColor = Some(color))
	
	def withAlignment(alignment: Alignment) = copy(textAlignment = alignment)
	
	def withBackground(background: Color) = copy(background = Some(background))
	
	def withColors(colors: ComponentColor) = copy(
		background = Some(colors.background), textColor = colors.defaultTextColor)
	
	def withInsets(insets: StackInsets) = copy(insets = insets)
	
	def mapInsets(f: StackInsets => StackInsets) = copy(insets = f(insets))
	
	def withBorder(border: Border) = copy(border = Some(border))
	
	def withBorderWidth(borderWidth: Int) = copy(borderWidth = Some(borderWidth))
	
	def withStackMargin(margin: StackLength) = copy(stackMargin = margin)
	
	def withStackCap(cap: StackLength) = copy(stackCap = cap)
	
	def withDropDownWidthLimit(widthLimit: Int) = copy(dropDownWidthLimit = Some(widthLimit))
	
	def withSwitchWidth(switchWidth: StackLength) = copy(switchWidth = Some(switchWidth))
	
	def withTextFieldWidth(width: StackLength) = copy(textFieldWidth = Some(width))
	
	def withScrollPerWheelClick(scrollAmount: Double) = copy(scrollPerWheelClick = scrollAmount)
	
	def withScrollBarWidth(barWidth: Int) = copy(scrollBarWidth = barWidth)
	
	def withScrollBarDrawer(drawer: ScrollBarDrawer) = copy(scrollBarDrawer = Some(drawer))
	
	def withScrollFriction(friction: LinearAcceleration) = copy(scrollFriction = friction)
	
	/**
	  * Completes a block of code using a context built from this builder
	  * @param f A function that uses build context
	  */
	def use[A](f: ComponentContext => A) = f(result)
	
	
	// NESTED	-----------------------------
	
	private class DefaultScrollBarDrawer extends ScrollBarDrawer
	{
		override def draw(drawer: Drawer, barBounds: ScrollBarBounds, barDirection: Axis2D) =
		{
			// If scroll bar is inside content, only draws the bar, otherwise draws background as well
			if (scrollBarIsInsideContent)
				drawer.onlyFill(Color.black.withAlpha(0.55)).draw(barBounds.bar.toRoundedRectangle(1))
			else
			{
				drawer.onlyFill(barBackground.getOrElse(Color.gray(0.5))).draw(barBounds.area)
				drawer.onlyFill(highlightColor).draw(barBounds.bar)
			}
		}
	}
}
