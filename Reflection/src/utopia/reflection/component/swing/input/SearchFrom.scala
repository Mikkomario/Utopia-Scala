package utopia.reflection.component.swing.input

import utopia.firmament.component.display.Refreshable
import utopia.firmament.context.ComponentCreationDefaults.componentLogger
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, ImageDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.{Fit, Leading}
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.util.StringExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.image.Image
import utopia.reflection.component.swing.label.{ItemLabel, ViewLabel}
import utopia.reflection.component.swing.template.SwingComponentRelated
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable

import scala.concurrent.ExecutionContext

object SearchFrom
{
	/**
	  * Creates a new search from field by wrapping a text field and using specified component creation context
	  * @param searchField Search field to be wrapped
	  * @param noResultsView View displayed when no result exist or none are found with current filter
	  * @param displayStackLayout Stack layout used for the selection display items
	  * @param searchIcon Icon displayed at the right side of the search field (optional)
	  * @param contentPointer Content pointer used (default = new pointer)
	  * @param selectedValuePointer Pointer that holds the currently selected item (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether pop-up window should be opened whenever this field gains focus (default = true)
	  * @param sameInstanceCheck  Function for checking whether two items represent the same option (default = use standard equals)
	  * @param contentIsStateless Whether each displayed item should be considered an individual instance and not a state
	  *                           of some other instance. If you didn't specify sameInstanceCheck, don't specify this either.
	  * @param makeDisplay        Function for creating display components
	  * @param itemToSearchString Function for converting selectable items to search / display strings
	  * @param context Component creation context (implicit)
	  * @param exc Execution context (implicit)
	  * @tparam A Type of selected item
	  * @tparam C Type of display used
	  * @return A new search from field
	  */
	def wrapFieldWithContext[A, C <: AwtStackable with Refreshable[A]]
	(searchField: TextField[String], noResultsView: AwtStackable, displayStackLayout: StackLayout = Fit,
	 searchIcon: Option[Image] = None,
	 contentPointer: EventfulPointer[Seq[A]] = EventfulPointer[Seq[A]](Empty),
	 selectedValuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 sameInstanceCheck: (A, A) => Boolean = (a: A, b: A) => a == b, contentIsStateless: Boolean = true)
	(makeDisplay: A => C)(itemToSearchString: A => String)
	(implicit context: StaticTextContext, exc: ExecutionContext) =
	{
		val backgroundColor = searchField.background
		val highlightColor = backgroundColor.highlighted
		val field = new SearchFrom[A, C](searchField, noResultsView, context.actorHandler,
			BackgroundDrawer(highlightColor, Normal), context.smallStackMargin, displayStackLayout, searchIcon,
			context.textInsets, contentPointer, selectedValuePointer, shouldDisplayPopUpOnFocusGain, sameInstanceCheck,
			contentIsStateless)(makeDisplay)(itemToSearchString)
		field.background = backgroundColor
		field
	}
	
	/**
	  * Creates a new search from field using component creation context
	  * @param selectionPrompt               Prompt text displayed on the search field
	  * @param standardWidth                 Width used in the field by default
	  * @param displayStackLayout            Stack layout used for the selection display items (default = Fit)
	  * @param searchIcon                    Icon displayed at the right side of the search field (optional)
	  * @param contentPointer                Content pointer used (default = new pointer)
	  * @param selectedValuePointer          Pointer that holds the currently selected item (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether pop-up window should be opened whenever this field gains focus (default = true)
	  * @param sameInstanceCheck             Function for checking whether two items represent the same option (default = use standard equals)
	  * @param contentIsStateless Whether each displayed item should be considered an individual instance and not a state
	  *                                      of some other instance. If you didn't specify sameInstanceCheck, don't specify this either.
	  * @param makeNoResultsView A function for creating a view that is shown when no results are found with the
	  *                          provided search. Accepts the search string pointer. Called only once.
	  * @param makeDisplay                   Function for creating display components
	  * @param itemToSearchString            Function for converting selectable items to search / display strings
	  * @param context                       Component creation context (implicit)
	  * @param exc                           Execution context (implicit)
	  * @tparam A Type of selected item
	  * @tparam C Type of display used
	  * @return A new search from field
	  */
	def contextual[A, C <: AwtStackable with Refreshable[A]]
	(selectionPrompt: LocalizedString, standardWidth: StackLength, displayStackLayout: StackLayout = Fit,
	 searchIcon: Option[Image] = None, contentPointer: EventfulPointer[Seq[A]] = EventfulPointer[Seq[A]](Empty),
	 selectedValuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 sameInstanceCheck: (A, A) => Boolean = (a: A, b: A) => a == b, contentIsStateless: Boolean = true)
	(makeNoResultsView: Changing[String] => AwtStackable)
	(makeDisplay: A => C)(itemToSearchString: A => String)
	(implicit context: StaticTextContext, exc: ExecutionContext) =
	{
		val searchField = TextField.contextualForStrings(standardWidth, prompt = selectionPrompt)
		wrapFieldWithContext(searchField, makeNoResultsView(searchField.valuePointer), displayStackLayout, searchIcon,
			contentPointer, selectedValuePointer, shouldDisplayPopUpOnFocusGain, sameInstanceCheck,
			contentIsStateless)(makeDisplay)(itemToSearchString)
	}
	
	/**
	  * Creates a new search from field that displays items as text
	  * @param displayFunction               Display function used for transforming items to text
	  * @param selectionPrompt               Prompt that is displayed
	  * @param standardWidth                 Default width for the search field
	  * @param searchIcon                    Icon displayed at the right side of the search field (optional)
	  * @param displayStackLayout            Stack layout used in selection items display (default = Fit)
	  * @param contentPointer                Content pointer used (default = new pointer)
	  * @param selectedValuePointer          Pointer for currently selected value (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether pop-up window should be opened whenever this field gains focus (default = true)
	  * @param sameInstanceCheck             Function for checking whether two items represent the same option (default = use standard equals)
	  * @param contentIsStateless Whether each displayed item should be considered an individual instance and not a state
	  *                                      of some other instance. If you didn't specify sameInstanceCheck, don't specify this either.
	  * @param makeNoResultsView A function for creating a view that is displayed when no results are found with the
	  *                          primary search. Accepts a pointer to the current search string.
	  * @param context                       Component creation context (implicit)
	  * @param exc                           Execution context (implicit)
	  * @tparam A Type of selected item
	  * @return A new search from field
	  */
	def contextualWithTextOnly[A](selectionPrompt: LocalizedString, standardWidth: StackLength,
	                              displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                              displayStackLayout: StackLayout = Leading, searchIcon: Option[Image] = None,
	                              contentPointer: EventfulPointer[Seq[A]] = EventfulPointer[Seq[A]](Empty),
	                              selectedValuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
	                              shouldDisplayPopUpOnFocusGain: Boolean = true,
	                              sameInstanceCheck: (A, A) => Boolean = (a: A, b: A) => a == b,
	                              contentIsStateless: Boolean = true)
								 (makeNoResultsView: Changing[String] => AwtStackable)
								 (implicit context: StaticTextContext, exc: ExecutionContext) =
	{
		def makeField(item: A) = ItemLabel.contextual(item, displayFunction)
		def itemToSearchString(item: A) = displayFunction(item).string
		
		contextual(selectionPrompt, standardWidth, displayStackLayout, searchIcon,
			contentPointer, selectedValuePointer, shouldDisplayPopUpOnFocusGain,
			sameInstanceCheck, contentIsStateless)(makeNoResultsView)(makeField)(itemToSearchString)
	}
	
	// TODO: Add one more constructor that displays text + icon
	
	/**
	  * Creates a label that can be used to indicate that no results were found. This label can be used in SearchFromFields.
	  * @param noResultsText Text displayed when no results are found. Should contain a placeholder (%s) for the search filter.
	  * @param searchStringPointer A pointer to current search filter
	  * @param context Component creation context (implicit)
	  * @return New label that adjusts itself based on changes in the search filter
	  */
	def noResultsLabel(noResultsText: LocalizedString, searchStringPointer: Changing[String])
					  (implicit context: StaticTextContext) =
		ViewLabel.contextual(searchStringPointer,
			new DisplayFunction[String](s => noResultsText.interpolated(Single(s))))
}

/**
  * A custom drop down selection component with a search / filter function
  * @author Mikko Hilpinen
  * @since 26.2.2020, v1
  * @param searchField Search field wrapped by this component
  * @param noResultsView View displayed when no results exist or no are found for current filter
  * @param actorHandler Actor handler that delivers action events for pop-up and key event handling
  * @param selectionDrawer Drawer used for highlighting currently selected item in selection pop-up
  * @param betweenDisplaysMargin Margin placed between displayed items (default = any margin)
  * @param displayStackLayout Stack layout used for the selection displays stack (default = Fit)
  * @param searchIcon Icon displayed at the right side of the search field (optional)
  * @param searchIconInsets Insets placed around the search icon (default = any insets)
  * @param contentPointer Pointer for selection pool (default = new pointer)
  * @param selectedValuePointer Pointer for currently selected value (default = new pointer)
  * @param shouldDisplayPopUpOnFocusGain Whether Pop-up should be opened whenever field gains focus
  * @param sameInstanceCheck Function for comparing two selectable items, whether they represent the same option
  *                    (default = { a == b })
  * @param contentIsStateless Whether each displayed item should be considered an individual instance and not a state
  *                           of some other instance. If you didn't specify sameInstanceCheck, don't specify this either.
  * @param makeDisplayFunction Function for creating new selection displays. Takes the initially displayed item.
  * @param itemToSearchString Function for converting a selectable item to searchable string. Used when filtering items.
  */
class SearchFrom[A, C <: AwtStackable with Refreshable[A]]
(searchField: TextField[String], override protected val noResultsView: AwtStackable, actorHandler: ActorHandler,
 selectionDrawer: CustomDrawer, betweenDisplaysMargin: StackLength = StackLength.any, displayStackLayout: StackLayout = Fit,
 searchIcon: Option[Image] = None, searchIconInsets: StackInsets = StackInsets.any,
 override val contentPointer: EventfulPointer[Seq[A]] = EventfulPointer[Seq[A]](Empty),
 selectedValuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
 shouldDisplayPopUpOnFocusGain: Boolean = true, sameInstanceCheck: (A, A) => Boolean = (a: A, b: A) => a == b,
 contentIsStateless: Boolean = true)
(makeDisplayFunction: A => C)(itemToSearchString: A => String)
(implicit exc: ExecutionContext)
	extends DropDownFieldLike[A, C](actorHandler, selectionDrawer, betweenDisplaysMargin, displayStackLayout,
		valuePointer = selectedValuePointer, contentIsStateless = contentIsStateless) with SwingComponentRelated
{
	// TODO: Consider adding border to pop-up content view
	// Draws border at each side, except for top of pop-up
	// popupContentView.addCustomDrawer(new BorderDrawer(Border(Insets.symmetric(borderWidth) - Up, borderColor)))
	
	// ATTRIBUTES	----------------------------
	
	private val defaultWidth = searchField.targetWidth
	
	private var currentSearchString = ""
	private var currentOptions: Seq[(String, A)] = Empty
	
	
	// INITIAL CODE	-----------------------------
	
	setup(shouldDisplayPopUpOnFocusGain)
	
	// When focus is lost and an item is selected, updates the text as well (unless field is empty,
	// in which case selection is removed)
	searchField.addFocusLostListener {
		if (!isDisplayingPopUp)
		{
			if (searchField.text.nonEmpty)
				value match
				{
					case Some(selected) => searchField.text = itemToSearchString(selected)
					case None => searchField.clear()
				}
			else
				value = None
		}
	}
	
	// When content updates, changes selection options and updates field size
	contentPointer.addContinuousListenerAndSimulateEvent(Empty) { e =>
		currentOptions = e.newValue.map { a => itemToSearchString(a) -> a }
		updateDisplayedOptions()
		searchField.targetWidth = (if (content.isEmpty) defaultWidth else currentSearchStackSize.width) +
			searchIcon.map { _.width + searchIconInsets.horizontal.optimal }.getOrElse(0.0)
	}
	
	// When text field updates (while no value is selected)
	searchField.valuePointer.addContinuousListener { event =>
		val newFilter = event.newValue
		if (currentSearchString != newFilter)
		{
			currentSearchString = newFilter
			updateDisplayedOptions()
		}
	}
	
	valuePointer.addContinuousListenerAndSimulateEvent(None) { e =>
		e.newValue match {
			case Some(newValue) =>
				currentSearchString = itemToSearchString(newValue)
				searchField.text = currentSearchString
			case None =>
				currentSearchString = ""
				searchField.clear()
		}
	}
	
	// Possibly adds custom drawing for the search image
	searchIcon.foreach { img => searchField.addCustomDrawer(ImageDrawer.right.withInsets(searchIconInsets)(img)) }
	
	
	// IMPLEMENTED	----------------------------
	
	override def requestFocusInWindow() = searchField.requestFocusInWindow()
	
	override def component = searchField.component
	
	override protected def representSameInstance(first: A, second: A) = sameInstanceCheck(first, second)
	
	override protected def makeDisplay(item: A) = makeDisplayFunction(item)
	
	override protected def mainDisplay = searchField
	
	
	// OTHER	--------------------------------
	
	private def updateDisplayedOptions() =
	{
		// Applies filter to currently displayed options
		currentSelectionOptionsPointer.value = {
			if (currentSearchString.isEmpty)
				content
			else
			{
				val searchWords = currentSearchString.words.map { _.toLowerCase }
				currentOptions.filter { case (k, _) =>
					val lower = k.toLowerCase
					searchWords.forall(lower.contains)
				}.map { _._2 }
			}
		}
	}
}
