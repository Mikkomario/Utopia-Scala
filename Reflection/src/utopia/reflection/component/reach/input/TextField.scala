package utopia.reflection.component.reach.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Changing
import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.Insets
import utopia.reflection.color.ColorRole.Error
import utopia.reflection.color.{ColorRole, ColorScheme, ComponentColor}
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.view.{BackgroundViewDrawer, BorderViewDrawer}
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.{ViewTextLabel, ViewTextLabelFactory}
import utopia.reflection.component.reach.template.{Focusable, ReachComponentWrapper}
import utopia.reflection.component.reach.wrapper.Open
import utopia.reflection.container.reach.{MutableStack, StackFactory, ViewStack, ViewStackFactory}
import utopia.reflection.event.FocusStateTracker
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.{Alignment, Border}
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.text.{Font, Regex}
import utopia.reflection.localization.LocalString._

import scala.concurrent.duration.Duration

/**
  * Used for requesting text input from the user
  * @author Mikko Hilpinen
  * @since 14.11.2020, v2
  */
/*
val baseStylePointer: PointerWithEvents[TextDrawContext],
						selectedTextColorPointer: Changing[Color] = Changing.wrap(Color.textBlack),
						selectionBackgroundColorPointer: Changing[Option[Color]] = Changing.wrap(None),
						caretColor: Color = Color.textBlack, caretWidth: Double = 1.0,
						caretBlinkFrequency: Duration = 0.5.seconds,
						val textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
						inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
						enabledPointer: Changing[Boolean] = Changing.wrap(true),
						allowSelectionWhileDisabled: Boolean = true, allowLineBreaks: Boolean = true,
						override val allowTextShrink: Boolean = false)
 */
class TextField[A](parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, colorScheme: ColorScheme,
				   contextBackgroundPointer: Changing[ComponentColor], defaultWidth: StackLength,
				   font: Font, alignment: Alignment, textInsets: StackInsets,
				   fieldNamePointer: Option[Changing[LocalizedString]], promptPointer: Changing[LocalizedString],
				   hintPointer: Changing[LocalizedString], errorMessagePointer: Changing[LocalizedString],
				   textPointer: PointerWithEvents[String],
				   selectedTextColorPointer: Changing[Color],
				   selectionBackgroundPointer: Changing[Option[Color]],
				   highlightStylePointer: Changing[Option[ColorRole]], focusColorRole: ColorRole,
				   defaultBorderWidth: Double, focusBorderWidth: Double, hintScaleFactor: Double, caretWidth: Double,
				   caretBlinkFrequency: Duration, betweenLinesMargin: Double, inputFilter: Option[Regex],
				   resultFilter: Option[Regex], maxLength: Option[Int], enabledPointer: Changing[Boolean],
				   fillBackground: Boolean, allowLineBreaks: Boolean, allowTextShrink: Boolean,
				   showCharacterCount: Boolean)
				  (parseResult: Option[String] => A)
	extends ReachComponentWrapper
{
	// ATTRIBUTES	------------------------------------------
	
	/*
	private val actualTextInsets =
	{
		// Text insets always expand horizontally
		val base = textInsets.expandingHorizontallyAccordingTo(alignment)
		// Top inset is increased (based on border) only if field name pointer is not used and border is
		// drawn on all sides
		val borderInsets =
		{
			if (fillBackground)
				Insets.bottom(focusBorderWidth)
			else
				Insets(focusBorderWidth, focusBorderWidth, if (fieldNamePointer.isDefined) 0 else focusBorderWidth,
					focusBorderWidth)
		}
		base + borderInsets
	}*/
	
	private val focusTracker = new FocusStateTracker(false)
	
	// Displays an error if there is one, otherwise displays the hint (provided there is one)
	private val actualHintTextPointer = hintPointer.mergeWith(errorMessagePointer) { (hint, error) =>
		error.notEmpty getOrElse hint }
	private val hintVisibilityPointer = actualHintTextPointer.map { _.nonEmpty }
	
	// A pointer to whether this field currently highlights an error
	private val errorStatePointer = errorMessagePointer.map { _.nonEmpty }
	private val externalHighlightStatePointer =
		highlightStylePointer.mergeWith(errorStatePointer) { (custom, isError) => if (isError) Some(Error) else custom }
	private val highlightStatePointer = focusPointer.mergeWith(externalHighlightStatePointer) { (focus, custom) =>
		custom.orElse { if (focus) Some(focusColorRole) else None }
	}
	private val highlightColorPointer = highlightStatePointer
		.mergeWith(contextBackgroundPointer) { (state, background) =>
			state.map { s => colorScheme(s).forBackground(background) } }
	// If a separate background color is used for this component, it depends from this component's state
	private val innerBackgroundPointer =
	{
		// TODO: Handle mouse over state (highlights one more time)
		if (fillBackground)
			contextBackgroundPointer.mergeWith(focusPointer) { (context, focus) =>
				val base = context.highlighted
				if (focus) base.highlighted else base
			}
		else
			contextBackgroundPointer
	}
	
	private val editTextColorPointer = innerBackgroundPointer.map { _.defaultTextColor }
	private val contentColorPointer: Changing[Color] = highlightColorPointer
		.mergeWith(editTextColorPointer) { (highlight, default) =>
			highlight match
			{
				case Some(color) => color: Color
				case None => default
			}
		}
	private val defaultHintColorPointer = contextBackgroundPointer.map { _.textColorStandard.hintTextColor }
	private val errorHintColorPointer = contextBackgroundPointer.mergeWith(errorStatePointer) { (background, isError) =>
		if (isError) Some(colorScheme.error.forBackground(background)) else None }
	private val hintColorPointer = defaultHintColorPointer.mergeWith(errorHintColorPointer) { (default, error) =>
		error match
		{
			case Some(color) => color: Color
			case None => default
		}
	}
	/*
	private val normalTextStylePointer = editTextColorPointer.map { color => TextDrawContext(font, color, alignment,
		actualTextInsets, betweenLinesMargin) }
		*/
	private val hintTextStylePointer = hintColorPointer.map { color =>
		TextDrawContext(font * hintScaleFactor, color, alignment,
			textInsets.expandingHorizontallyAccordingTo(alignment) * hintScaleFactor,
			betweenLinesMargin * hintScaleFactor)
	}
	
	private val borderPointer =
	{
		// Border widths at 0 => No border is drawn
		if (defaultBorderWidth <= 0 && focusBorderWidth <= 0)
			Changing.wrap(Border.zero)
		// When using filled background style, only draws the bottom border which varies in style based state
		else if (fillBackground)
		{
			// In case both focus and default borders share the same width, doesn't listen to the focus state
			if (defaultBorderWidth == focusBorderWidth)
				contentColorPointer.map { Border.bottom(defaultBorderWidth, _) }
			// Otherwise uses a different height border when focused
			else
				contentColorPointer.mergeWith(focusPointer) { (color, focus) =>
					Border.bottom(if (focus) focusBorderWidth else defaultBorderWidth, color)
				}
		}
		else if (defaultBorderWidth == focusBorderWidth)
			contentColorPointer.map { color => Border.symmetric(defaultBorderWidth, color) }
		else
			contentColorPointer.mergeWith(focusPointer) { (color, focus) =>
				Border.symmetric(if (focus) focusBorderWidth else defaultBorderWidth, color)
			}
	}
	
	/*
	private val openMainLabel = Open.using(EditableTextLabel) { factory =>
		factory.apply(actorHandler, ???, selectedTextColorPointer, selectionBackgroundPointer, highlightColorPointer,
			caretWidth, caretBlinkFrequency, textPointer, inputFilter, maxLength, enabledPointer,
			allowLineBreaks = allowLineBreaks, allowTextShrink = allowTextShrink)
	}(parentHierarchy.top)*/
	
	// The main vertical stack contains the label portion and a hint / info portion, which might not always be displayed
	/*private val mainStack = ViewStack(parentHierarchy).builder(Mixed).withFixedStyle(margin = StackLength.fixedZero) { ff =>
		
		// val labelArea = ff.next()()
		???
	}*/
	
	
	// COMPUTED	----------------------------------------------
	
	def hasFocus = focusTracker.hasFocus
	
	def focusPointer = focusTracker.focusPointer
	
	
	// IMPLEMENTED	------------------------------------------
	
	override protected def wrapped = ???
	
	
	// OTHER	----------------------------------------------
	
	// Creates the main label in situations where there is no field name label to handle
	private def makeTextLabelOnly(factory: EditableTextLabelFactory) =
	{
		// Text insets always expand horizontally
		val baseInsets = textInsets.expandingHorizontallyAccordingTo(alignment)
		// Top and side inset are increased if border is drawn on all sides
		val borderInsets = if (fillBackground) Insets.bottom(focusBorderWidth) else Insets.symmetric(focusBorderWidth)
		val insets = baseInsets + borderInsets
		
		val textStylePointer = editTextColorPointer.map { color => TextDrawContext(font, color, alignment,
			insets, betweenLinesMargin) }
		val label = factory.apply(actorHandler, textStylePointer, selectedTextColorPointer, selectionBackgroundPointer,
			contentColorPointer, caretWidth, caretBlinkFrequency, textPointer, inputFilter, maxLength, enabledPointer,
			allowSelectionWhileDisabled = false, allowLineBreaks, allowTextShrink)
		
		// Draws background (optional) and border
		if (fillBackground)
			label.addCustomDrawer(BackgroundViewDrawer(innerBackgroundPointer.lazyMap { c => c }))
		label.addCustomDrawer(BorderViewDrawer(borderPointer))
		
		label
	}
	
	private def makeTextAndNameArea(factories: ViewStackFactory, fieldNamePointer: Changing[LocalizedString]) =
	{
		// TODO: Implement
	}
	
	// Returns the generated component, along with its visibility pointer (if applicable)
	private def makeHintArea(factories: Mixed) =
	{
		// In some cases, displays both message field and character count label
		// In other cases only the message field (which is hidden while empty)
		(if (showCharacterCount) None else maxLength) match
		{
			// Case: Character count should be displayed => Always displays at least the counter
			case Some(maxLength) =>
				// Places caps to stack equal to horizontal content margin
				val cap = textInsets.horizontal / 2 + (if (fillBackground) 0 else defaultBorderWidth)
				val stack = factories(ViewStack).builder(ViewTextLabel).withFixedStyle(X,
					margin = StackLength.any, cap = cap) { labelFactories =>
					val hintLabel = makeHintLabel(labelFactories.next())
					
					val countStylePointer = defaultHintColorPointer.map { color =>
						TextDrawContext(font * hintScaleFactor, color, Alignment.Right, textInsets * hintScaleFactor) }
					val textLengthPointer = textPointer.map { _.length }
					val countLabel = labelFactories.next()(textLengthPointer, countStylePointer,
						DisplayFunction.noLocalization[Int] { length => s"$length / $maxLength".noLanguage })
					
					// Hint label is only displayed while there is a hint to display,
					// Count label is always displayed
					Vector(hintLabel -> Some(hintVisibilityPointer), countLabel -> None)
				}.parent
				stack -> None
			// Case: Only hint label should be displayed (only when there's a message to show)
			case None => makeHintLabel(factories(ViewTextLabel)) -> Some(hintVisibilityPointer)
		}
	}
	
	private def makeHintLabel(factory: ViewTextLabelFactory) =
		factory.forText(actualHintTextPointer, hintTextStylePointer, allowLineBreaks = false, allowTextShrink = true)
}
