package utopia.reflection.component.swing

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Changing
import utopia.flow.util.StringExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.reflection.component.Refreshable
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, ImageDrawer}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.swing.label.{ItemLabel, TextLabel}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.{Alignment, StackInsets, StackLength}
import utopia.reflection.util.ComponentContext

import scala.collection.immutable.HashMap
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
	  * @param checkEquals Function for checking item equality (default = use standard equals)
	  * @param makeDisplay Function for creating display components
	  * @param itemToSearchString Function for converting selectable items to search / display strings
	  * @param context Component creation context (implicit)
	  * @param exc Execution context (implicit)
	  * @tparam A Type of selected item
	  * @tparam C Type of display used
	  * @return A new search from field
	  */
	def wrapFieldWithContext[A, C <: AwtStackable with Refreshable[A]]
	(searchField: TextField, noResultsView: AwtStackable, displayStackLayout: StackLayout = Fit, searchIcon: Option[Image] = None,
	 contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
	 selectedValuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 checkEquals: (A, A) => Boolean = (a: A, b: A) => a == b)(makeDisplay: A => C)(itemToSearchString: A => String)
	(implicit context: ComponentContext, exc: ExecutionContext) =
	{
		val field = new SearchFrom[A, C](searchField, noResultsView, context.actorHandler,
			new BackgroundDrawer(context.highlightColor, Normal), context.stackMargin, displayStackLayout, searchIcon,
			context.insets, contentPointer, selectedValuePointer, shouldDisplayPopUpOnFocusGain, checkEquals)(
			makeDisplay)(itemToSearchString)
		context.setBorderAndBackground(field)
		field
	}
	
	/**
	  * Creates a new search from field using component creation context
	  * @param noResultsView View displayed when no result exist of none are found with current filter
	  * @param selectionPrompt Prompt text displayed on the search field
	  * @param displayStackLayout Stack layout used for the selection display items (default = Fit)
	  * @param searchIcon Icon displayed at the right side of the search field (optional)
	  * @param contentPointer     Content pointer used (default = new pointer)
	  * @param selectedValuePointer Pointer that holds the currently selected item (default = new pointer)
	  * @param searchFieldPointer Pointer that holds the search field's current value / text (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether pop-up window should be opened whenever this field gains focus (default = true)
	  * @param checkEquals Function for checking item equality (default = use standard equals)
	  * @param makeDisplay        Function for creating display components
	  * @param itemToSearchString Function for converting selectable items to search / display strings
	  * @param context Component creation context (implicit)
	  * @param exc Execution context (implicit)
	  * @tparam A Type of selected item
	  * @tparam C Type of display used
	  * @return A new search from field
	  */
	def contextual[A, C <: AwtStackable with Refreshable[A]]
	(noResultsView: AwtStackable, selectionPrompt: LocalizedString, displayStackLayout: StackLayout = Fit,
	 searchIcon: Option[Image] = None, contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
	 selectedValuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 searchFieldPointer: PointerWithEvents[Option[String]] = new PointerWithEvents[Option[String]](None),
	 shouldDisplayPopUpOnFocusGain: Boolean = true,
	 checkEquals: (A, A) => Boolean = (a: A, b: A) => a == b)(makeDisplay: A => C)(itemToSearchString: A => String)
	(implicit context: ComponentContext, exc: ExecutionContext) =
	{
		val searchField = TextField.contextual(prompt = Some(selectionPrompt), valuePointer = searchFieldPointer)
		wrapFieldWithContext(searchField, noResultsView, displayStackLayout, searchIcon, contentPointer, selectedValuePointer,
			shouldDisplayPopUpOnFocusGain, checkEquals)(makeDisplay)(itemToSearchString)
	}
	
	/**
	  * Creates a new search from field that displays items as text
	  * @param noResultsView View displayed when no results are available or none were found with current filter
	  * @param displayFunction Display function used for transforming items to text
	  * @param selectionPrompt Prompt that is displayed
	  * @param searchIcon Icon displayed at the right side of the search field (optional)
	  * @param displayStackLayout Stack layout used in selection items display (default = Fit)
	  * @param contentPointer Content pointer used (default = new pointer)
	  * @param selectedValuePointer Pointer for currently selected value (default = new pointer)
	  * @param searchFieldPointer Pointer for search field's current text (default = new pointer)
	  * @param shouldDisplayPopUpOnFocusGain Whether pop-up window should be opened whenever this field gains focus (default = true)
	  * @param checkEquals Function for checking item equality (default = use standard equals)
	  * @param context Component creation context (implicit)
	  * @param exc Execution context (implicit)
	  * @tparam A Type of selected item
	  * @return A new search from field
	  */
	def contextualWithTextOnly[A](noResultsView: AwtStackable, selectionPrompt: LocalizedString,
								  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
								  displayStackLayout: StackLayout = Fit, searchIcon: Option[Image] = None,
								  contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
								  selectedValuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
								  searchFieldPointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None),
								  shouldDisplayPopUpOnFocusGain: Boolean = true,
								  checkEquals: (A, A) => Boolean = (a: A, b: A) => a == b)
								 (implicit context: ComponentContext, exc: ExecutionContext) =
	{
		def makeField(item: A) = ItemLabel.contextual(item, displayFunction)
		def itemToSearchString(item: A) = displayFunction(item).string
		
		contextual(noResultsView, selectionPrompt, displayStackLayout, searchIcon,
			contentPointer, selectedValuePointer, searchFieldPointer, shouldDisplayPopUpOnFocusGain,
			checkEquals)(makeField)(itemToSearchString)
	}
	
	// TODO: Add one more constructor that displays text + icon
	
	/**
	  * Creates a label that can be used to indicate that no results were found. This label can be used in SearchFromFields.
	  * @param noResultsText Text displayed when no results are found. Should contain a placeholder (%s) for the search filter.
	  * @param searchStringPointer A pointer to current search filter
	  * @param context Component creation context (implicit)
	  * @return New label that adjusts itself based on changes in the search filter
	  */
	def noResultsLabel(noResultsText: LocalizedString, searchStringPointer: Changing[Option[String]])
					  (implicit context: ComponentContext) =
	{
		val label = TextLabel.contextual(noResultsText.interpolate(searchStringPointer.value.getOrElse("")))
		searchStringPointer.addListener { e => label.text = noResultsText.interpolate(e.newValue.getOrElse("")) }
		label
	}
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
  * @param equalsCheck Function for comparing two selectable items, whether they represent the same option
  *                    (default = { a == b })
  * @param makeDisplayFunction Function for creating new selection displays. Takes the initially displayed item.
  * @param itemToSearchString Function for converting a selectable item to searchable string. Used when filtering items.
  */
class SearchFrom[A, C <: AwtStackable with Refreshable[A]]
(searchField: TextField, override protected val noResultsView: AwtStackable, override protected val actorHandler: ActorHandler,
 selectionDrawer: CustomDrawer, betweenDisplaysMargin: StackLength = StackLength.any, displayStackLayout: StackLayout = Fit,
 searchIcon: Option[Image] = None, searchIconInsets: StackInsets = StackInsets.any,
 override val contentPointer: PointerWithEvents[Vector[A]] = new PointerWithEvents[Vector[A]](Vector()),
 selectedValuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
 shouldDisplayPopUpOnFocusGain: Boolean = true, equalsCheck: (A, A) => Boolean = (a: A, b: A) => a == b)
(makeDisplayFunction: A => C)(itemToSearchString: A => String)
(implicit exc: ExecutionContext)
	extends DropDownFieldLike[A, C](selectionDrawer, betweenDisplaysMargin, displayStackLayout,
		valuePointer = selectedValuePointer) with SwingComponentRelated
{
	// ATTRIBUTES	----------------------------
	
	private val defaultWidth = searchField.targetWidth
	
	private var currentSearchString = ""
	private var currentOptions: Map[String, A] = HashMap()
	
	
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
	addContentListener({ e =>
		currentOptions = e.newValue.map { a => itemToSearchString(a) -> a }.toMap
		updateDisplayedOptions()
		searchField.targetWidth = (if (content.isEmpty) defaultWidth else currentSearchStackSize.width) +
			searchIcon.map { _.width + searchIconInsets.horizontal.optimal }.getOrElse(0.0)
	}, Some(Vector()))
	
	// When text field updates (while no value is selected)
	searchField.addValueListener
	{
		_.newValue match
		{
			case Some(newFilter) =>
				if (currentSearchString != newFilter)
				{
					currentSearchString = newFilter
					updateDisplayedOptions()
				}
			case None =>
				currentSearchString = ""
				updateDisplayedOptions()
		}
	}
	
	addValueListener({e =>
		e.newValue match
		{
			case Some(newValue) =>
				currentSearchString = itemToSearchString(newValue)
				searchField.text = currentSearchString
			case None =>
				currentSearchString = ""
				searchField.clear()
		}
	}, Some(None))
	
	// Possibly adds custom drawing for the search image
	searchIcon.foreach { img => searchField.addCustomDrawer(new ImageDrawer(img, searchIconInsets, Alignment.Right)) }
	
	
	// IMPLEMENTED	----------------------------
	
	override def requestFocusInWindow() = searchField.requestFocusInWindow()
	
	override def component = searchField.component
	
	override protected def checkEquals(first: A, second: A) = equalsCheck(first, second)
	
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
				currentOptions.filterKeys { k =>
					val lower = k.toLowerCase
					searchWords.forall(lower.contains)
				}.values.toVector
			}
		}
	}
}
