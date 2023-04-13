package utopia.reflection.component.swing.input

import utopia.firmament.image.ButtonImageSet
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{WeekDay, WeekDays}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.shape.shape2d.Size
import utopia.firmament.drawing.view.SelectionCircleViewDrawer
import utopia.reflection.component.swing.StackSpace
import utopia.reflection.component.swing.button.{CustomDrawableButtonLike, ImageButton}
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.component.swing.template.{AwtComponentRelated, StackableAwtComponentWrapperWrapper}
import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.controller.data.SelectionGroup
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.SwitchPanel
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.{StackInsets, StackLength, StackSize}

import java.time._
import scala.collection.immutable.HashMap

object Calendar
{
	// TODO: Add contextual creation options once date limits have been added
	
	/**
	  * Creates a new calendar component
	  * @param monthDropDown Drop down used for selecting months
	  * @param yearDropDown Drop down used for selecting years
	  * @param forwardIcon Images used for forward button
	  * @param backwardIcon Images used for backward button
	  * @param headerHMargin Horizontal margin between header (month selection) items
	  * @param afterHeaderMargin Margin after header
	  * @param dayNameDisplayFunction Display function used with week days
	  * @param dayNameFont Font used with day names
	  * @param dayNameTextColor Text color used with day names
	  * @param dayNameInsets Insets used inside day name labels
	  * @param dateFont Font used with date number buttons
	  * @param dateTextColor Text color used in date number buttons
	  * @param dateInsets Insets used inside date number buttons
	  * @param selectionHoverColor Color used to highlight the field mouse is hovering over
	  * @param weekDays Calendar system to use
	  * @return A new calendar
	  */
		// TODO: Update to a new version that uses new drop down implementations
	def apply(monthDropDown: JDropDownWrapper[Month], yearDropDown: JDropDownWrapper[Year], forwardIcon: ButtonImageSet,
			  backwardIcon: ButtonImageSet, headerHMargin: StackLength, afterHeaderMargin: StackLength,
			  dayNameDisplayFunction: DisplayFunction[WeekDay], dayNameFont: Font, dayNameTextColor: Color,
			  dayNameInsets: StackInsets, dateFont: Font, dateTextColor: Color, dateInsets: StackInsets,
			  selectionHoverColor: Color, selectedColor: Color)
	         (implicit weekDays: WeekDays) =
	{
		def makeDayNameLabel(day: WeekDay) =
		{
			new ItemLabel[WeekDay](new PointerWithEvents(day), dayNameDisplayFunction, dayNameFont, dayNameTextColor,
				dayNameInsets, Alignment.Center)
		}
		def makeDateLabel(date: Int) = new DateLabel(date, dateFont, dateInsets, dateTextColor, selectionHoverColor,
			selectedColor)
		
		new Calendar(monthDropDown, yearDropDown, forwardIcon, backwardIcon, headerHMargin, afterHeaderMargin,
			StackSize.fixed(Size.zero), makeDayNameLabel, makeDateLabel)
	}
	
	private class DateLabel(val date: Int, font: Font, insets: StackInsets, textColor: Color, hoverColor: Color,
							selectedColor: Color)
		extends StackableAwtComponentWrapperWrapper with MutableCustomDrawableWrapper
		with CustomDrawableButtonLike with InteractionWithPointer[Boolean]
	{
		// ATTRIBUTES	-----------------
		
		private val label = new ItemLabel[Int](new PointerWithEvents(date), DisplayFunction.raw, font, textColor,
			insets, Alignment.Center)
		
		override val valuePointer = new PointerWithEvents[Boolean](false)
		
		
		// INITIAL CODE	-----------------
		
		label.component.setFocusable(true)
		addCustomDrawer(SelectionCircleViewDrawer(hoverColor, selectedColor, valuePointer, statePointer))
		
		valuePointer.addContinuousAnyChangeListener { repaint() }
		registerAction { () => value = !value }
		initializeListeners()
		setHandCursor()
		
		
		// IMPLEMENTED	-----------------
		
		override def toString = s"DateLabel($date)"
		
		override protected def wrapped = label
		
		override def drawable = label
	}
}

/**
  * Used for letting the user choose a date
  * @author Mikko Hilpinen
  * @since 2.8.2019, v1+
  * @param monthDropDown Drop down used for selecting months, should already contain allowed values
  * @param yearDropDown Drop down used for selecting years, should already contain allowed values
  * @param forwardIcon Images used in the "next month" button
  * @param backwardIcon Images used in the "previous month" button
  * @param headerHMargin Horizontal margin between items in header (year + month selection)
  * @param afterHeaderMargin Vertical margin between header and the calendar / date selection portion
  * @param insideCalendarMargin Margins used between items inside the calendar portion
  * @param makeDayNameLabel A function for making labels for each week day name
  * @param makeDateButton A function for making an interactive element for each day of a month
  * @param weekDays Calendar system to use
  */
class Calendar(val monthDropDown: JDropDownWrapper[Month], val yearDropDown: JDropDownWrapper[Year], forwardIcon: ButtonImageSet,
               backwardIcon: ButtonImageSet, headerHMargin: StackLength, afterHeaderMargin: StackLength,
               val insideCalendarMargin: StackSize, makeDayNameLabel: WeekDay => AwtComponentRelated with ReflectionStackable,
               private val makeDateButton: Int => AwtComponentRelated with ReflectionStackable with InteractionWithPointer[Boolean])
              (implicit weekDays: WeekDays)
	extends StackableAwtComponentWrapperWrapper with MutableCustomDrawableWrapper with InteractionWithPointer[LocalDate]
{
	// ATTRIBUTES	-----------------------
	
	override val valuePointer = new PointerWithEvents[LocalDate](LocalDate.now)
	
	private var handlingDropDownUpdate = false
	private var handlingPointerUpdate = false
	
	// Selects current month from the drop downs (if possible)
	{
		val currentDate = LocalDate.now
		yearDropDown.selectOne(currentDate.year)
		yearDropDown.selectAny()
		monthDropDown.selectOne(currentDate.month)
		monthDropDown.selectAny()
	}
	
	private val segmentGroup = new SegmentGroup()
	
	private var cachedDaySelections: Map[YearMonth, DaySelection] = HashMap()
	
	private var currentSelection = selectionFor(selectedMonth)
	
	private val previousButton = ImageButton(backwardIcon) { lastMonth() }
	private val nextButton = ImageButton(forwardIcon) { nextMonth() }
	
	// Day selections are swapped each time month changes, hence the switch panel
	private val selectionSwitch = SwitchPanel(currentSelection)
	
	// Creates contents
	private val content: Stack[Stack[_]] =
	{
		// Header consists of 2 dropdowns (month & year) and 2 arrow buttons (previous & next)
		val headerRow = previousButton.rowWith(Vector(monthDropDown, yearDropDown, nextButton),
			margin = headerHMargin, layout = Center)
		
		// Calendar consists of names & numbers parts
		val dayNameLabels = weekDays.values.map(makeDayNameLabel)
		val dayNameRow = Stack.rowWithItems(segmentGroup.wrap(dayNameLabels), insideCalendarMargin.width)
		
		currentSelection.addValueListener(SelectionChangeListener)
		
		val calendarPart = dayNameRow.columnWith(Vector(selectionSwitch), margin = insideCalendarMargin.along(Y))
		headerRow.columnWith(Vector(calendarPart), margin = afterHeaderMargin)
	}
	
	
	// INITIAL CODE	-----------------------
	
	// Listens for month & year changes
	Vector(yearDropDown, monthDropDown)
		.foreach { _.valuePointer.addContinuousAnyChangeListener { handleDropDownUpdate() } }
	
	// Listens for date changes from outside
	valuePointer.addListener { e =>
		if (!handlingDropDownUpdate && !handlingPointerUpdate) {
			handlingPointerUpdate = true
			
			// May need to switch year, month and/or date
			if (e.newValue.yearMonth != selectedMonth)
			{
				yearDropDown.selectOne(e.newValue.year)
				monthDropDown.selectOne(e.newValue.month)
				updateSelectionArea()
			}
			
			// Selects correct date from selection
			currentSelection.value = Some(e.newValue)
			
			handlingPointerUpdate = false
		}
		true
	}
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @return The smallest possible month that is currently selectable
	  */
	def minMonth = {
		if (yearDropDown.isEmpty || monthDropDown.isEmpty)
			value.yearMonth
		else
			yearDropDown.content.min + monthDropDown.content.min
	}
	/**
	  * @return The largest possible month that is currently selectable
	  */
	def maxMonth = {
		if (yearDropDown.isEmpty || monthDropDown.isEmpty)
			value.yearMonth
		else
			yearDropDown.content.max + monthDropDown.content.max
	}
	
	/**
	  * @return Currently selected month
	  */
	def selectedMonth = {
		val year = yearDropDown.selected.getOrElse(value.year)
		val month = monthDropDown.selected.getOrElse(value.month)
		year + month
	}
	
	
	// IMPLEMENTED	-----------------------
	
	override def drawable = content
	
	override protected def wrapped = content
	
	
	// OTHER	---------------------------
	
	private def handleDropDownUpdate() =
	{
		if (!handlingPointerUpdate && !handlingDropDownUpdate)
		{
			handlingDropDownUpdate = true
			updateSelectionArea()
			currentSelection.value.foreach { value = _ }
			handlingDropDownUpdate = false
		}
		
		updateMonthAdjustButtons()
	}
	
	private def nextMonth() = adjustMonth(1)
	private def lastMonth() = adjustMonth(-1)
	
	private def adjustMonth(adjustment: Int) =
	{
		val newSelected = selectedMonth + adjustment
		yearDropDown.selectOne(newSelected.year)
		monthDropDown.selectOne(newSelected.getMonth)
	}
	
	private def updateSelectionArea() =
	{
		currentSelection.removeValueListener(SelectionChangeListener)
		
		currentSelection = selectionFor(selectedMonth)
		currentSelection.addValueListener(SelectionChangeListener)
		selectionSwitch.set(currentSelection)
	}
	
	private def selectionFor(month: YearMonth) =
	{
		if (cachedDaySelections.contains(month))
			cachedDaySelections(month)
		else
		{
			val selection = new DaySelection(month)
			cachedDaySelections += month -> selection
			selection
		}
	}
	
	private def updateMonthAdjustButtons() =
	{
		val current = selectedMonth
		previousButton.enabled = current > minMonth
		nextButton.enabled = current < maxMonth
	}
	
	
	// NESTED	---------------------------
	
	private object SelectionChangeListener extends ChangeListener[Option[LocalDate]]
	{
		override def onChangeEvent(event: ChangeEvent[Option[LocalDate]]) = {
			if (!handlingPointerUpdate ) {
				// Updates date pointer based on selection update
				event.newValue.foreach { value = _ }
			}
			true
		}
	}
	
	private class DaySelection(yearMonth: YearMonth) extends StackableAwtComponentWrapperWrapper
		with InteractionWithPointer[Option[LocalDate]]
	{
		// ATTRIBUTES	-------------------
		
		override val valuePointer = new PointerWithEvents[Option[LocalDate]](None)
		
		private val buttons = {
			// Groups dates by weeks
			val weeks = yearMonth.weeks
			weeks.map { _.map { d => d -> makeDateButton(d.getDayOfMonth) }.toVector }
		}
		
		private val rows = {
			// Adds empty labels as placeholders on partial weeks
			val firstRow = if (buttons.head.size >= 7) buttons.head.map { _._2 } else
				Vector.fill(7 - buttons.head.size) { new StackSpace(StackSize.any.withLowPriority) } ++ buttons.head.map { _._2 }
			val lastRow = if (buttons.last.size >= 7) buttons.last.map { _._2 } else
				buttons.last.map { _._2 } ++ Vector.fill(7 - buttons.last.size) { new StackSpace(StackSize.any.withLowPriority) }
			val rowElements = firstRow +: buttons.slice(1, buttons.size - 1).map { _.map { _._2 } } :+ lastRow
			
			// Adds button listening
			SelectionGroup(buttons.flatten.map { _._2 }.toSet)
			buttons.view.flatten.foreach { case (date, button) =>
				button.addValueListener { e =>
					if (e.newValue)
						valuePointer.value = Some(date)
					else if (value.contains(date))
						valuePointer.value = None
					true
				}
			}
			
			// Creates date rows
			rowElements.map { items => Stack.rowWithItems(segmentGroup.wrap(items), insideCalendarMargin.width) }
		}
		
		private val content = Stack.columnWithItems(rows, margin = insideCalendarMargin.height)
		
		
		// INITIAL CODE	------------------
		
		// Updates buttons based on value pointer changes
		valuePointer.addContinuousListener { e =>
			e.newValue.foreach { date => buttons.view.flatten.find { _._1 == date }.foreach { _._2.value = true } }
		}
		
		
		// IMPLEMENTED	------------------
		
		override protected def wrapped = content
	}
}
