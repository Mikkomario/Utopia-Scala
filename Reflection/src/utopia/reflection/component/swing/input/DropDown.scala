package utopia.reflection.component.swing.input

import utopia.firmament.component.display.Refreshable
import utopia.firmament.context.TextContext
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.{Border, TextDrawContext}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration
import utopia.paradigm.enumeration.Direction2D.Up
import utopia.firmament.drawing.immutable.{BackgroundDrawer, BorderDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.template.DrawLevel.Normal
import utopia.reflection.component.swing.label.{ImageLabel, ItemLabel, TextLabel}
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.Focusable
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.stack.modifier.StackSizeModifier
import utopia.firmament.model.stack.{StackInsets, StackLength, StackSize}
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.reflection.text.Prompt

import scala.concurrent.ExecutionContext

object DropDown
{
	/**
	  * Creates a contextual drop down field
	  * @param noResultsView View displayed when no results are available (no items to select)
	  * @param icon Icon displayed at the right side of this view
	  * @param selectionPromptText Text displayed while no item is selected
	  * @param displayFunction Function for converting items to displayable strings (default = toString)
	  * @param displayStackLayout Stack layout used for selection displays (default = Fit)
	  * @param contentPointer Pointer that holds current selection options (default = new pointer)
	  * @param valuePointer Pointer that holds currently selected value (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether this component should always open selection pop-up on focus gain (default = true)
	  * @param sameInstanceCheck Function for checking whether two selection options represent the same item
	  *                          (default = standard equals (== -operator))
	  * @param contentIsStateless Whether the specified items don't represent states of other objects but are
	  *                           independent values (default = true). Set to false only when specifying
	  *                           'sameInstanceCheck' as other than default.
	  * @param makeDisplayFunction Function for producing new selection displays
	  * @param context Component creation context
	  * @param exc Implicit execution context
	  * @tparam A Type of selected item
	  * @tparam C Type of selection display
	  * @return A new drop down field
	  */
	def contextual[A, C <: AwtStackable with Refreshable[A]]
	(noResultsView: AwtStackable, icon: Image, selectionPromptText: LocalizedString,
	 displayFunction: DisplayFunction[A] = DisplayFunction.raw, displayStackLayout: StackLayout = Fit,
	 contentPointer: EventfulPointer[Vector[A]] = new EventfulPointer[Vector[A]](Vector()),
	 valuePointer: EventfulPointer[Option[A]] = new EventfulPointer[Option[A]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 sameInstanceCheck: (A, A) => Boolean = (a: A, b: A) => a == b, contentIsStateless: Boolean = true)
	(makeDisplayFunction: A => C)
	(implicit context: TextContext, exc: ExecutionContext) =
	{
		// Determines background and focus colors
		val backgroundColor = context.background
		val highlightColor = backgroundColor.highlighted
		
		val dd = new DropDown[A, C](context.actorHandler, noResultsView, icon,
			BackgroundDrawer(highlightColor, Normal), highlightColor, selectionPromptText,
			context.font, context.textColor, displayFunction, context.textAlignment, context.textInsets,
			context.textInsets.mapVertical { _.lowPriority }, context.textColor,
			context.buttonBorderWidth, context.smallStackMargin, displayStackLayout, contentPointer, valuePointer,
			!context.allowTextShrink, context.allowImageUpscaling, shouldDisplayPopUpOnFocusGain, sameInstanceCheck,
			contentIsStateless)(makeDisplayFunction)
		dd.background = backgroundColor
		dd
	}
	
	/**
	  * Creates a contextual drop down field that uses text fields for item display
	  * @param noResultsView View displayed when no results are available (no items to select)
	  * @param icon Icon displayed at the right side of this view
	  * @param selectionPromptText Text displayed while no item is selected
	  * @param displayFunction Function for converting items to displayable strings (default = toString)
	  * @param contentPointer Pointer that holds current selection options (default = new pointer)
	  * @param valuePointer Pointer that holds currently selected value (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether this component should always open selection pop-up on focus gain (default = true)
	  * @param sameInstanceCheck             Function for checking whether two selection options represent the same item
	  *                                      (default = standard equals (== -operator))
	  * @param contentIsStateless Whether the specified items don't represent states of other objects but are
	  *                                      independent values (default = true). Set to false only when specifying
	  *                                      'sameInstanceCheck' as other than default.
	  * @param context                       Component creation context
	  * @param exc Implicit execution context
	  * @tparam A Type of selected item
	  * @return A new drop down field
	  */
	def contextualWithTextOnly[A]
	(noResultsView: AwtStackable, icon: Image, selectionPromptText: LocalizedString,
	 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	 contentPointer: EventfulPointer[Vector[A]] = new EventfulPointer[Vector[A]](Vector()),
	 valuePointer: EventfulPointer[Option[A]] = new EventfulPointer[Option[A]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 sameInstanceCheck: (A, A) => Boolean = (a: A, b: A) => a == b, contentIsStateless: Boolean = true)
	(implicit context: TextContext, exc: ExecutionContext) =
	{
		def makeDisplay(item: A) = ItemLabel.contextual(item, displayFunction)
		contextual(noResultsView, icon, selectionPromptText, displayFunction, Fit, contentPointer,
			valuePointer, shouldDisplayPopUpOnFocusGain, sameInstanceCheck, contentIsStateless)(makeDisplay)
	}
}

/**
  * This component allows the user to choose from a set of pre-existing options via a selection pop-up
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class DropDown[A, C <: AwtStackable with Refreshable[A]]
(actorHandler: ActorHandler, override protected val noResultsView: AwtStackable, icon: Image,
 selectionDrawer: CustomDrawer, focusColor: Color, selectionPrompt: Prompt, defaultFont: Font,
 defaultTextColor: Color = Color.textBlack, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
 textAlignment: enumeration.Alignment = enumeration.Alignment.Left, textInsets: StackInsets = StackInsets.any,
 imageInsets: StackInsets = StackInsets.any, borderColor: Color = Color.textBlackDisabled,
 borderWidth: Double = 1.0, betweenDisplaysMargin: StackLength = StackLength.any, displayStackLayout: StackLayout = Fit,
 override val contentPointer: EventfulPointer[Vector[A]] = new EventfulPointer[Vector[A]](Vector()),
 valuePointer: EventfulPointer[Option[A]] = new EventfulPointer[Option[A]](None), textHasMinWidth: Boolean = true,
 allowImageUpscaling: Boolean = false, shouldDisplayPopUpOnFocusGain: Boolean = true,
 sameInstanceCheck: (A, A) => Boolean = (a: A, b: A) => a == b, contentIsStateless: Boolean = true)
(makeDisplayFunction: A => C)(implicit exc: ExecutionContext)
	extends DropDownFieldLike[A, C](actorHandler, selectionDrawer, betweenDisplaysMargin, displayStackLayout, contentPointer,
		valuePointer, contentIsStateless)
{
	// ATTRIBUTES	------------------------------
	
	private val finalTextInsets = textInsets.mapRight { _.noMax.expanding } + borderWidth
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
		if (!isDisplayingPopUp) {
			textLabel.isTransparent = true
			textLabel.repaint()
		}
	}
	
	view.addConstraint(MainDisplayWidthConstraint)
	setup(shouldDisplayPopUpOnFocusGain)
	setHandCursor()
	
	// Because of size constraint, revalidates component whenever content updates
	contentPointer.addContinuousAnyChangeListener { revalidate() }
	
	// Adds border drawing to the view
	{
		// Draws border around the view
		view.addCustomDrawer(BorderDrawer(Border.symmetric(borderWidth, borderColor)))
		// Draws border at the right side of text
		textLabel.addCustomDrawer(BorderDrawer(Border(Insets.right(borderWidth), borderColor)))
		// Draws border at each side, except for top of pop-up
		popupContentView.addCustomDrawer(BorderDrawer(Border(Insets.symmetric(borderWidth) - Up, borderColor)))
	}
	
	// Updates the item display whenever value changes
	valuePointer.addContinuousListener { _.newValue match {
		case Some(selected) =>
			textLabel.textDrawContext = valueSelectedContext
			textLabel.text = displayFunction(selected)
		case None =>
			textLabel.textDrawContext = noValueContext
			textLabel.text = selectionPrompt.text
	} }
	
	
	// IMPLEMENTED	------------------------------
	
	override def isInFocus = view.isInFocus
	
	override def requestFocusInWindow() = view.component.requestFocusInWindow()
	
	override protected def representSameInstance(first: A, second: A) = sameInstanceCheck(first, second)
	
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
