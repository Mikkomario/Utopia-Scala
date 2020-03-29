package utopia.reflection.component.swing

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, BorderDrawer, TextDrawContext}
import utopia.reflection.component.{Focusable, Refreshable}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.swing.label.{ImageLabel, ItemLabel, TextLabel}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.{Alignment, Border, Insets, StackInsets, StackLength, StackSize, StackSizeModifier}
import utopia.reflection.text.{Font, Prompt}
import utopia.reflection.util.ComponentContext

import scala.concurrent.ExecutionContext

object DropDown
{
	/**
	  * Creates a contextual drop down field
	  * @param noResultsView View displayed when no results are available (no items to select)
	  * @param icon Icon displayed at the right side of this view
	  * @param selectionPromptText Text displayed while no item is selected
	  * @param displayFunction Function for converting items to displayable strings (default = toString)
	  * @param borderColor Color used in component border (default = black)
	  * @param displayStackLayout Stack layout used for selection displays (default = Fit)
	  * @param contentPointer Pointer that holds current selection options (default = new pointer)
	  * @param valuePointer Pointer that holds currently selected value (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether this component should always open selection pop-up on focus gain (default = true)
	  * @param equalsCheck Function for checking equality between selection options (default = a == b)
	  * @param makeDisplayFunction Function for producing new selection displays
	  * @param context Component creation context
	  * @param exc Implicit execution context
	  * @tparam A Type of selected item
	  * @tparam C Type of selection display
	  * @return A new drop down field
	  */
	def contextual[A, C <: AwtStackable with Refreshable[A]]
	(noResultsView: AwtStackable, icon: Image, selectionPromptText: LocalizedString,
	 displayFunction: DisplayFunction[A] = DisplayFunction.raw, borderColor: Color = Color.textBlack,
	 displayStackLayout: StackLayout = Fit,
	 contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
	 valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 equalsCheck: (A, A) => Boolean = (a: A, b: A) => a == b)
	(makeDisplayFunction: A => C)
	(implicit context: ComponentContext, exc: ExecutionContext) =
	{
		val dd = new DropDown[A, C](context.actorHandler, noResultsView, icon,
			new BackgroundDrawer(context.highlightColor, Normal), context.highlightColor, selectionPromptText,
			context.font, context.textColor, displayFunction, context.textAlignment, context.insets,
			context.insets.mapVertical { _.withLowPriority }, borderColor,
			context.borderWidth, context.stackMargin, displayStackLayout, contentPointer, valuePointer,
			context.textHasMinWidth, context.allowImageUpscaling, shouldDisplayPopUpOnFocusGain, equalsCheck)(makeDisplayFunction)
		context.background.foreach { dd.background = _ }
		dd
	}
	
	/**
	  * Creates a contextual drop down field that uses text fields for item display
	  * @param noResultsView View displayed when no results are available (no items to select)
	  * @param icon Icon displayed at the right side of this view
	  * @param selectionPromptText Text displayed while no item is selected
	  * @param displayFunction Function for converting items to displayable strings (default = toString)
	  * @param borderColor Color used in component border (default = black)
	  * @param contentPointer Pointer that holds current selection options (default = new pointer)
	  * @param valuePointer Pointer that holds currently selected value (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether this component should always open selection pop-up on focus gain (default = true)
	  * @param equalsCheck Function for checking equality between selection options (default = a == b)
	  * @param context Component creation context
	  * @param exc Implicit execution context
	  * @tparam A Type of selected item
	  * @return A new drop down field
	  */
	def contextualWithTextOnly[A]
	(noResultsView: AwtStackable, icon: Image, selectionPromptText: LocalizedString,
	 displayFunction: DisplayFunction[A] = DisplayFunction.raw, borderColor: Color = Color.textBlack,
	 contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
	 valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 equalsCheck: (A, A) => Boolean = (a: A, b: A) => a == b)(implicit context: ComponentContext, exc: ExecutionContext) =
	{
		def makeDisplay(item: A) = ItemLabel.contextual(item, displayFunction)
		contextual(noResultsView, icon, selectionPromptText, displayFunction, borderColor, Fit, contentPointer,
			valuePointer, shouldDisplayPopUpOnFocusGain, equalsCheck)(makeDisplay)
	}
}

/**
  * This component allows the user to choose from a set of pre-existing options via a selection pop-up
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class DropDown[A, C <: AwtStackable with Refreshable[A]]
(override protected val actorHandler: ActorHandler, override protected val noResultsView: AwtStackable, icon: Image,
 selectionDrawer: CustomDrawer, focusColor: Color, selectionPrompt: Prompt, defaultFont: Font,
 defaultTextColor: Color = Color.textBlack, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
 textAlignment: Alignment = Alignment.Left, textInsets: StackInsets = StackInsets.any,
 imageInsets: StackInsets = StackInsets.any, borderColor: Color = Color.textBlack,
 borderWidth: Double = 1.0, betweenDisplaysMargin: StackLength = StackLength.any, displayStackLayout: StackLayout = Fit,
 override val contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
 valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None), textHasMinWidth: Boolean = true,
 allowImageUpscaling: Boolean = false, shouldDisplayPopUpOnFocusGain: Boolean = true,
 equalsCheck: (A, A) => Boolean = (a: A, b: A) => a == b)
(makeDisplayFunction: A => C)(implicit exc: ExecutionContext)
	extends DropDownFieldLike[A, C](selectionDrawer, betweenDisplaysMargin, displayStackLayout, contentPointer, valuePointer)
{
	// ATTRIBUTES	------------------------------
	
	private val finalTextInsets = textInsets.mapRight { _.noMax.withLowPriority } + borderWidth
	private val noValueContext = TextDrawContext(selectionPrompt.font, selectionPrompt.color, textAlignment, finalTextInsets)
	private val valueSelectedContext = TextDrawContext(defaultFont, defaultTextColor, textAlignment, finalTextInsets)
	
	private val textLabel = new TextLabel(selectionPrompt.text, selectionPrompt.font, selectionPrompt.color,
		finalTextInsets, textAlignment, textHasMinWidth)
	private val imageLabel = new ImageLabel(icon, allowUpscaling = allowImageUpscaling)
	
	private val view = Stack.rowWithItems(Vector(textLabel, imageLabel.framed(imageInsets + borderWidth)), StackLength.fixedZero)
	
	
	// INITIAL CODE	------------------------------
	
	view.component.setFocusable(true)
	view.addFocusChangedListener {
		textLabel.background = focusColor
		textLabel.repaint()
	} {
		if (!isDisplayingPopUp)
		{
			textLabel.isTransparent = true
			textLabel.repaint()
		}
	}
	
	view.addConstraint(MainDisplayWidthConstraint)
	setup(shouldDisplayPopUpOnFocusGain)
	setHandCursor()
	
	// Because of size constraint, revalidates component whenever content updates
	addContentListener { _ => revalidate() }
	
	// Adds border drawing to the view
	{
		val basicBorderDrawer = new BorderDrawer(Border.symmetric(borderWidth, borderColor))
		view.addCustomDrawer(basicBorderDrawer)
		textLabel.addCustomDrawer(new BorderDrawer(Border(Insets.right(borderWidth), borderColor)))
		popupContentView.addCustomDrawer(basicBorderDrawer)
	}
	
	// Updates the item display whenever value changes
	addValueListener { _.newValue match
	{
		case Some(selected) =>
			textLabel.drawContext = valueSelectedContext
			textLabel.text = displayFunction(selected)
		case None =>
			textLabel.drawContext = noValueContext
			textLabel.text = selectionPrompt.text
	} }
	
	
	// IMPLEMENTED	------------------------------
	
	override def isInFocus = view.isInFocus
	
	override def requestFocusInWindow() = view.component.requestFocusInWindow()
	
	override protected def checkEquals(first: A, second: A) = equalsCheck(first, second)
	
	override protected def makeDisplay(item: A) = makeDisplayFunction(item)
	
	override protected def mainDisplay: StackableAwtComponentWrapperWrapper with Focusable = ViewWrapper
	
	
	// NESTED	---------------------------------
	
	private object ViewWrapper extends StackableAwtComponentWrapperWrapper with Focusable
	{
		override protected def wrapped = view
		
		override def requestFocusInWindow() = view.component.requestFocusInWindow()
	}
	
	private object MainDisplayWidthConstraint extends StackSizeModifier
	{
		override def apply(size: StackSize) =
		{
			// The optimal width must not be smaller than that of the current search stack width
			val minWidth = currentSearchStackSize.width.optimal
			val rawWidth = size.optimalWidth
			if (rawWidth >= minWidth)
				size
			else
				size.withOptimalWidth(minWidth)
		}
	}
}
