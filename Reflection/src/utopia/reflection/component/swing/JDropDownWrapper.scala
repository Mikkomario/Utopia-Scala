package utopia.reflection.component.swing

import java.awt.event.{ActionEvent, ActionListener}

import javax.swing.plaf.basic.ComboPopup
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._
import utopia.flow.util.CollectionExtensions._
import javax.swing.{JComboBox, JList, ListCellRenderer}
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.genesis.color.Color
import utopia.reflection.component.Focusable
import utopia.reflection.component.input.SelectableWithPointers
import utopia.reflection.component.stack.{CachingStackable, StackLeaf}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.{StackInsets, StackLength, StackSize}
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentContext

object JDropDownWrapper
{
	/**
	  * Creates a new drop down using contextual information
	  * @param selectText Text displayed when no value is selected and some are available
	  * @param displayFunction A function for transforming selectable values into displayable format
	  *                        (default = displayed as is)
	  * @param initialChoices Initially shown choices (default = empty)
	  * @param context Component creation context
	  * @tparam A Type of selected item
	  * @return A new drop down
	  */
	def contextual[A](selectText: LocalizedString, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					  initialChoices: Vector[A] = Vector())(implicit context: ComponentContext) =
	{
		val dropDown = new JDropDownWrapper[A](context.insets, selectText, context.font,
			context.background.getOrElse(Color.white), context.focusColor, context.textColor, displayFunction,
			initialChoices, context.dropDownWidthLimit)
		context.border.foreach(dropDown.setBorder)
		dropDown
	}
}

/**
  * Dropdowns are used for selecting a single value from multiple alternatives
  * @author Mikko Hilpinen
  * @since 1.5.2019, v1+
  * @param insets The insets placed around the text (affects stack size)
  * @param selectText The text displayed when no value is selected
  * @param font The font used in this drop down
  * @param backgroundColor The default label background color
  * @param selectedBackground The background color of currently selected item
  * @param textColor The text color used (default = 88% opacity black)
  * @param displayFunction A function used for transforming values to displayable strings
  *                        (default = toString with no localization)
  * @param initialContent The initially available selections
  * @param maximumOptimalWidth The maximum optimal widht for this drop down (default = maximum based on text length & margin)
  */
class JDropDownWrapper[A](val insets: StackInsets, val selectText: LocalizedString, font: Font, backgroundColor: Color,
						  selectedBackground: Color, textColor: Color = Color.textBlack,
						  val displayFunction: DisplayFunction[A] = DisplayFunction.raw, initialContent: Vector[A] = Vector(),
						  val maximumOptimalWidth: Option[Int] = None)
	extends SelectableWithPointers[Option[A], Vector[A]] with JWrapper with CachingStackable with Focusable with StackLeaf
{
	// ATTRIBUTES	-------------------
	
	private val field = new JComboBox[String]()
	private var _displayValues = Vector[LocalizedString]()
	private var isShowingSelectOption = true
	private var isUpdatingSelection = false // Consider using a thread-safe solution
	
	override val valuePointer = new PointerWithEvents[Option[A]](None)
	override val contentPointer = new PointerWithEvents[Vector[A]](Vector())
	
	
	// INITIAL CODE	-------------------
	
	field.setFont(font.toAwt)
	field.setEditable(false)
	field.setMaximumRowCount(10)
	field.setForeground(textColor.toAwt)
	
	// Modifies the renderer
	field.setRenderer(new CellRenrerer(insets.left, backgroundColor, selectedBackground, textColor))
	
	{
		val popup = field.getUI.getAccessibleChild(field, 0)
		popup match
		{
			case p: ComboPopup =>
				val jlist = p.getList
				// jlist.setFixedCellHeight(h + margins.height.optimal * 2)
				// jlist.setVisibleRowCount(10)
				jlist.setSelectionBackground(selectedBackground.toAwt)
				jlist.setForeground(textColor.toAwt)
				jlist.setBackground(backgroundColor.toAwt)
				jlist.setSelectionForeground(textColor.toAwt)
				textHeight.foreach { jlist.setFixedCellHeight }
			case _ =>
		}
	}
	
	valuePointer.addListener(new SelectionUpdateListener)
	contentPointer.addListener(new ContentUpdateListener)
	
	content = initialContent
	field.addActionListener(new UserSelectionListener())
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return The currently selected index where 0 is the first item. None if no item is selected.
	  */
	def selectedIndex =
	{
		val index = field.getSelectedIndex
		// Index 0 in field is a plaeholder text (sometimes)
		if (index < indexMod) None else Some(index - indexMod)
	}
	
	/**
	  * @return The display of the currently selected value
	  */
	def selectedDisplay = selectedIndex.map(displayValues.apply)
	/**
	  * @return The currently displayed values (not including the placeholder text)
	  */
	def displayValues = _displayValues
	
	private def indexMod = if (isShowingSelectOption) 1 else 0
	
	
	// IMPLEMENTED	-------------------
	
	override protected def updateVisibility(visible: Boolean) = super[JWrapper].isVisible_=(visible)
	
	override def component = field
	
	override def calculatedStackSize =
	{
		// If this drop down contains fields, they may affect the width
		fontMetrics.map
		{
			metrics =>
				val maxTextWidth = (selectText +: displayValues).map { s => metrics.stringWidth(s.string) }.max
				val textW = insets.horizontal + maxTextWidth
				val textH = insets.vertical + metrics.getHeight
				
				// May limit text width optimal (also adds width for drop down icon)
				val finalW = (maximumOptimalWidth.map { max => if (textW.optimal > max) textW.withOptimal(max) else
					textW } getOrElse textW) + 24
				
				StackSize(finalW, textH)
				
		} getOrElse (220.any x 32.any)
	}
	
	override def updateLayout() = component.revalidate()
	
	override def requestFocusInWindow() = field.requestFocusInWindow()
	
	
	// OTHER	----------------------
	
	/**
	 * Updates the displayed values on this drop down based on the same items it already had. Only useful when the
	 * used display function returns different values at different times.
	 */
	def updateDisplays() = updateContent(content)
	
	private def updateContent(newContent: Vector[A]) =
	{
		isUpdatingSelection = true
		// Preserves selection
		val oldSelected = selected
		
		_displayValues = newContent.map(displayFunction.apply)
		
		// If there is only 1 item available or if previously selected item is still available, auto-selects it afterwards
		val newSelection =
		{
			if (newContent.size == 1)
				newContent.headOption
			else if (oldSelected.exists(newContent.contains))
				oldSelected
			else
				None
		}
		
		// Updates the field (leaves out "select") if there is an item selected or if there are no values available
		isShowingSelectOption = newSelection.isEmpty && newContent.nonEmpty
		val finalDisplayOptions = if (isShowingSelectOption) selectText +: _displayValues else _displayValues
		
		field.removeAllItems()
		finalDisplayOptions.foreach { s => field.addItem(s.string) }
		
		// Updates selection
		field.setSelectedIndex(newSelection.flatMap(newContent.optionIndexOf).getOrElse(-1) + indexMod)
		value = newSelection
		
		revalidate()
		isUpdatingSelection = false
	}
	
	
	// NESTED CLASSES	---------------
	
	private class ContentUpdateListener extends ChangeListener[Vector[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[A]]) = updateContent(event.newValue)
	}
	
	private class SelectionUpdateListener extends ChangeListener[Option[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Option[A]]) =
		{
			isUpdatingSelection = true
			if (event.newValue.isDefined)
			{
				val newIndex = event.newValue.flatMap(content.optionIndexOf) getOrElse -1
				
				// Index 0 in field sometimes represents the placeholder value (not selected)
				val trueIndex = -1 max ((-1 max newIndex) + indexMod) min (content.size - 1)
				field.setSelectedIndex(trueIndex)
				
				// Doesn't show selection onption once a selection is made
				if (isShowingSelectOption && trueIndex >= indexMod)
					updateContent(content)
			}
			isUpdatingSelection = false
		}
	}
	
	private class UserSelectionListener extends ActionListener
	{
		override def actionPerformed(e: ActionEvent) =
		{
			if (!isUpdatingSelection)
				value = selectedIndex.flatMap { i => content.getOption(i) }
		}
	}
	
	private class CellRenrerer(hmargin: StackLength, val defaultBackground: Color, val selectedBackground: Color,
							   val textColor: Color) extends ListCellRenderer[String]
	{
		// ATTRIBUTES	---------------------
		
		private val label = new TextLabel(LocalizedString.empty, font, textColor, StackInsets.left(hmargin))
		
		
		// IMPLEMENTED	---------------------
		
		override def getListCellRendererComponent(list: JList[_ <: String], value: String, index: Int, isSelected: Boolean, cellHasFocus: Boolean) =
		{
			if (value != null)
			{
				label.text = value.noLanguageLocalizationSkipped
				label.component.setPreferredSize(label.stackSize.optimal.toDimension)
			}
			
			// check if this cell is selected
			if (isSelected)
				label.background = selectedBackground
			// unselected, and not the DnD drop location
			else
				label.background = defaultBackground
			
			label.textColor = if (isShowingSelectOption && index == 0) textColor.timesAlpha(0.625) else textColor
			label.component
		}
	}
}