package utopia.reflection.component.swing.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.ChangingLike
import utopia.reflection.color.ColorRole.{Gray, Secondary}
import utopia.reflection.color.ColorShade
import utopia.reflection.color.ColorShade.Light
import utopia.reflection.component.context.{AnimationContextLike, ScrollingContextLike, TextContext}
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.swing.button.{FramedImageButton, ImageAndTextButton, ImageButton, TextButton}
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.display.{PoolWithPointer, Refreshable}
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.layout.multi.{AnimatedStack, Stack}
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollView
import utopia.reflection.controller.data.ContainerSelectionManager
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.{LocalizedString, Localizer}
import utopia.reflection.shape.stack.{StackLength, StackLengthLimit}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object TypeOrSearch
{
	/**
	  * A component with which the user may search and select from existing options or create a completely new option
	  * @author Mikko Hilpinen
	  * @since 22.9.2020, v1.3
	  * @param optimalTextFieldWidth Optimal width for the type text field
	  * @param addButtonText Text displayed on the primary add button (default = empty = no text)
	  * @param addButtonIcon Icon displayed on the primary add button (optional)
	  * @param selectButtonText Text displayed on item select buttons (default = empty = no text)
	  * @param selectButtonIcon Icon displayed on item select buttons (optional)
	  * @param optimalSelectionAreaLength Optimal length used for the scrollable selection area (optional)
	  * @param textFieldPrompt Prompt displayed on the text field (default = empty = no prompt)
	  * @param preferredTextFieldShade Color shade preferred in the text field (default = standard shade)
	  * @param searchDelay Delay applied before performing the search (good to increase for slower search functions)
	  *                    (default = no delay)
	  * @param optionsForInput Function for converting text field input into a list of proposed values.
	  *                        Run asynchronously. Should not throw.
	  * @param context Implicit component creation context (including font information)
	  * @param scrollingContext Implicit scroll component creation context
	  * @param animationContext Implicit component animation context
	  * @param exc Implicit execution context
	  */
	def apply(optimalTextFieldWidth: Double, addButtonText: LocalizedString = LocalizedString.empty,
	          addButtonIcon: Option[SingleColorIcon] = None, selectButtonText: LocalizedString = LocalizedString.empty,
	          selectButtonIcon: Option[SingleColorIcon] = None, optimalSelectionAreaLength: Option[Double] = None,
	          textFieldPrompt: LocalizedString = LocalizedString.empty, preferredTextFieldShade: ColorShade = Light,
	          searchDelay: Duration = Duration.Zero)
	         (optionsForInput: String => Seq[String])
	         (implicit context: TextContext, scrollingContext: ScrollingContextLike,
	          animationContext: AnimationContextLike, exc: ExecutionContext) = new TypeOrSearch(context,
		optimalTextFieldWidth, addButtonText, addButtonIcon, selectButtonText, selectButtonIcon,
		optimalSelectionAreaLength, textFieldPrompt, preferredTextFieldShade, searchDelay)(optionsForInput)
}

/**
  * A component with which the user may search and select from existing options or create a completely new option
  * @author Mikko Hilpinen
  * @since 22.9.2020, v1.3
  * @param parentContext Context in the parent component
  * @param optimalTextFieldWidth Optimal width for the type text field
  * @param addButtonText Text displayed on the primary add button (default = empty = no text)
  * @param addButtonIcon Icon displayed on the primary add button (optional)
  * @param selectButtonText Text displayed on item select buttons (default = empty = no text)
  * @param selectButtonIcon Icon displayed on item select buttons (optional)
  * @param optimalSelectionAreaLength Optimal length used for the scrollable selection area (optional)
  * @param textFieldPrompt Prompt displayed on the text field (default = empty = no prompt)
  * @param preferredTextFieldShade Color shade preferred in the text field (default = standard shade)
  * @param searchDelay Delay applied before performing the search (good to increase for slower search functions)
  *                    (default = no delay)
  * @param optionsForInput Function for converting text field input into a list of proposed values.
  *                        Run asynchronously. Should not throw.
  */
class TypeOrSearch
(parentContext: TextContext, optimalTextFieldWidth: Double, addButtonText: LocalizedString = LocalizedString.empty,
 addButtonIcon: Option[SingleColorIcon] = None, selectButtonText: LocalizedString = LocalizedString.empty,
 selectButtonIcon: Option[SingleColorIcon] = None, optimalSelectionAreaLength: Option[Double] = None,
 textFieldPrompt: LocalizedString = LocalizedString.empty, preferredTextFieldShade: ColorShade = Light,
 searchDelay: Duration = Duration.Zero)
(optionsForInput: String => Seq[String])
(implicit scrollingContext: ScrollingContextLike, animationContext: AnimationContextLike, exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with PoolWithPointer[Vector[String], ChangingLike[Vector[String]]]
{
	// ATTRIBUTES   ----------------------------
	
	private val selectedItemsPointer = new PointerWithEvents(Vector[String]())
	
	private implicit val languageCode: String = "en"
	private implicit val localizer: Localizer = parentContext.localizer
	private val margin = parentContext.relatedItemsStackMargin
	private val selectionColor = parentContext.color(Secondary, Light)
	private val itemButtonColor = parentContext.colorScheme.secondary.bestAgainst(
		Vector(parentContext.containerBackground, selectionColor))
	
	private val textField = TextField.contextualForStrings(optimalTextFieldWidth.any.expanding,
		prompt = textFieldPrompt)(parentContext.forButtons(Gray, preferredTextFieldShade))
	private val addButton = parentContext.forPrimaryColorButtons.use { implicit c =>
		val button = addButtonIcon match
		{
			case Some(icon) =>
				addButtonText.notEmpty match
				{
					case Some(text) => ImageAndTextButton.contextualWithoutAction(icon.inButton, text)
					case None => FramedImageButton.contextualWithoutAction(icon, isLowPriority = true)
				}
			case None => TextButton.contextualWithoutAction(
				addButtonText.notEmpty.getOrElse { "Add".autoLocalized })
		}
		button.registerAction { () => onAddButtonPressed() }
		button
	}
	private val optionsStack = parentContext.use { implicit c => AnimatedStack.contextualColumn[Display](itemsAreRelated = true) }
	// Stack.column[Display](parentContext.relatedItemsStackMargin, margin)
	private val manager = ContainerSelectionManager.forStatelessItems[String, Display](optionsStack,
		BackgroundDrawer(selectionColor)) { new Display(_) }
	private val view =
	{
		val upperPart = Stack.rowWithItems(Vector(textField, addButton), StackLength.fixedZero)
		val scrollLengthLimit = optimalSelectionAreaLength match
		{
			case Some(optimal) => StackLengthLimit(minOptimal = Some(optimal), maxOptimal = Some(optimal))
			case None => StackLengthLimit.noLimit
		}
		val lowerPart = ScrollView.contextual(optionsStack, lengthLimits = scrollLengthLimit)
		Stack.columnWithItems(Vector(upperPart, lowerPart), StackLength.fixedZero)
	}
	
	
	// INITIAL CODE ----------------------------
	
	// Updates selectable values when search field content updates (possibly delayed)
	(if (searchDelay > Duration.Zero) textField.valuePointer.delayedBy(searchDelay) else textField.valuePointer)
		.mapAsync[Seq[String]](Vector())(optionsForInput).addListener { event =>
			// Won't include already selected items
			val selected = selectedItemsPointer.value
			manager.content = event.newValue.toVector.filterNot(selected.contains)
		}
	
	// Submits a new item on enter press
	textField.addEnterListener { text =>
		manager.selected match
		{
			case Some(selected) => onItemSelected(selected)
			case None =>
				if (!text.isEmpty)
					onItemSelected(text)
		}
	}
	
	manager.enableKeyHandling(parentContext.actorHandler, listenEnabledCondition = Some(() => textField.isInFocus))
	manager.enableMouseHandling(consumeEvents = false)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def wrapped: Stack[_] = view
	override def contentPointer = selectedItemsPointer.view
	
	
	// OTHER    --------------------------------
	
	/**
	  * Removes a word from the list of selected words
	  * @param word Word to remove
	  */
	def -=(word: String) = selectedItemsPointer.update { _.filterNot { _ == word } }
	/**
	  * Removes multiple words from the list of selected words
	  * @param words Words to remove
	  */
	def --=(words: Set[String]) = selectedItemsPointer.update { _.filterNot(words.contains) }
	
	private def makeSelectionButton(currentItem: => String) = parentContext.forCustomColorButtons(itemButtonColor)
		.use { implicit c =>
			selectButtonIcon match
			{
				case Some(icon) =>
					selectButtonText.notEmpty match
					{
						case Some(text) => ImageAndTextButton.contextual(icon.inButton, text) {
							onItemSelected(currentItem) }
						case None => ImageButton.contextual(icon.asIndividualButton, isLowPriority = true) {
							onItemSelected(currentItem) }(parentContext)
					}
				case None => TextButton.contextual(selectButtonText.notEmpty
					.getOrElse { "Add".autoLocalized }) { onItemSelected(currentItem) }
			}
		}
	
	private def onItemSelected(item: String) =
	{
		// Updates selected items pointer
		selectedItemsPointer.update { old =>
			if (old.contains(item))
				old
			else
				old :+ item
		}
		// Removes the selected item from displayed items or resets the search
		if (textField.value.isEmpty)
			manager.content = manager.content.filterNot { _ == item }
		else
			textField.clear()
	}
	
	private def onAddButtonPressed() =
	{
		val newItem = textField.value
		if (!newItem.isEmpty)
			onItemSelected(newItem)
		textField.requestFocusInWindow()
	}
	
	
	// NESTED   --------------------------------
	
	private class Display(initialItem: String) extends StackableAwtComponentWrapperWrapper with Refreshable[String]
	{
		// ATTRIBUTES   ------------------------
		
		private val label = ItemLabel.contextual(initialItem)(parentContext.expandingToRight)
		private val view = Stack.buildRowWithContext(layout = Center, isRelated = true) { s =>
			s += label
			s += makeSelectionButton(content)
		
		}(parentContext)
		
		
		// IMPLEMENTED  ------------------------
		
		override protected def wrapped = view
		
		override def content_=(newContent: String) = label.content = newContent
		override def content = label.content
	}
}
