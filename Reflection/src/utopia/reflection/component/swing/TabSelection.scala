package utopia.reflection.component.swing

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.genesis.color.Color
import utopia.genesis.event.{ConsumeEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.drawing.template.DrawLevel.Background
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.input.SelectableWithPointers
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.{AwtContainerRelated, Stack}
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.{StackInsets, StackLength}
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentContext

import scala.collection.immutable.HashMap

object TabSelection
{
	/**
	  * Creates a new tab selection using contextual information
	  * @param displayFunction Display function used for selectable values (default = displayed as is)
	  * @param initialChoices Initially selectable choices (default = empty)
	  * @param selectionLineHeight Height of the line drawn under the currently selected component
	  * @param context Component creation context
	  * @tparam A Type of selected item
	  * @return A new tab selection
	  */
	def contextual[A](displayFunction: DisplayFunction[A] = DisplayFunction.raw, initialChoices: Seq[A] = Vector(),
					  selectionLineHeight: Double = 8.0)(implicit context: ComponentContext) =
	{
		val yMargin = context.insets.vertical
		val component = new TabSelection[A](context.font, context.highlightColor, context.insets.horizontal.optimal,
			yMargin, selectionLineHeight, displayFunction, initialChoices, context.textColor)
		context.setBorderAndBackground(component)
		component
	}
}

/**
  * This class offers a selection from multiple choices using a horizontal set of tabs
  * @author Mikko Hilpinen
  * @since 4.5.2019, v1+
  */
class TabSelection[A](val font: Font, val highlightColor: Color, val optimalHMargin: Double, val vMargin: StackLength,
					  val selectionLineHeight: Double = 8.0, val displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					  initialChoices: Seq[A] = Vector(), initialTextColor: Color = Color.textBlack)
	extends StackableAwtComponentWrapperWrapper with SwingComponentRelated
	with AwtContainerRelated with SelectableWithPointers[Option[A], Seq[A]] with CustomDrawableWrapper
{
	// ATTRIBUTES	-------------------
	
	private var _textColor = initialTextColor
	
	private val stack = Stack.row[TextLabel](0.fixed)
	private val textInsets = StackInsets.vertical(vMargin, vMargin + selectionLineHeight) +
		StackInsets.horizontal(optimalHMargin.any.expanding)
	
	private var labels: Map[A, TextLabel] = HashMap()
	
	override val valuePointer = new PointerWithEvents[Option[A]](None)
	override val contentPointer = new PointerWithEvents[Seq[A]](Vector())
	
	
	// INITIAL CODE	-------------------
	
	valuePointer.addListener(new ValueUpdateListener)
	contentPointer.addListener(new ContentUpdateListener)
	
	content = initialChoices
	// Selects the first component
	initialChoices.headOption.foreach { a => selected = Some(a) }
	
	
	// COMPUTED	-----------------------
	
	private def selectedLabel = selected.flatMap(labels.get)
	
	/**
	  * @return The current text color used in this component
	  */
	def textColor = _textColor
	/**
	  * Changes component text color
	  * @param newColor New text color to be used in this component
	  */
	def textColor_=(newColor: Color) =
	{
		if (_textColor != newColor)
		{
			_textColor = newColor
			labels.values.foreach { _.textColor = newColor }
		}
	}
	
	
	// IMPLEMENTED	-------------------
	
	override def drawable = stack
	
	override def component = stack.component
	
	override protected def wrapped = stack
	
	override def background_=(color: Color) = super[StackableAwtComponentWrapperWrapper].background_=(color)
	
	
	// OTHER	-------------------
	
	private def styleSelected(label: TextLabel) =
	{
		label.addCustomDrawer(SelectionDrawer)
		label.setArrowCursor()
	}
	
	private def styleNotSelected(label: TextLabel) =
	{
		label.removeCustomDrawer(SelectionDrawer)
		label.setHandCursor()
	}
	
	
	// NESTED CLASSES	----------
	
	private class ValueUpdateListener extends ChangeListener[Option[A]]
	{
		private var lastSelectedLabel: Option[TextLabel] = None
		
		override def onChangeEvent(event: ChangeEvent[Option[A]]) =
		{
			// Styles the labels based on selection
			lastSelectedLabel.foreach(styleNotSelected)
			
			val newSelected = selectedLabel
			newSelected.foreach(styleSelected)
			lastSelectedLabel = newSelected
			
			repaint()
		}
	}
	
	private class ContentUpdateListener extends ChangeListener[Seq[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Seq[A]]) =
		{
			// Makes sure there is a right amount of labels
			val newContent = event.newValue
			val oldValue = value
			val oldLabels = event.oldValue.map { v => labels(v) }
			val newLabels =
			{
				if (oldLabels.size > newContent.size)
				{
					val labelsToRemove = oldLabels.dropRight(oldLabels.size - newContent.size)
					stack --= labelsToRemove
					labelsToRemove.foreach { _.mouseButtonHandler.clear() }
					oldLabels.take(newContent.size)
				}
				else if (oldLabels.size < newContent.size)
				{
					val moreLabels = Vector.fill(newContent.size - oldLabels.size)
					{
						val label = new TextLabel(LocalizedString.empty, font, _textColor, textInsets, Center,
							hasMinWidth = false)
						styleNotSelected(label)
						label.addMouseButtonListener(new LabelMouseListener(label))
						label
					}
					
					stack ++= moreLabels
					oldLabels ++ moreLabels
				}
				else
					oldLabels
			}
			
			// Sets old selected label to normal style
			selectedLabel.foreach (styleNotSelected)
			
			// Assigns new values to labels
			labels = newContent.zip(newLabels).toMap
			newContent.foreach { v => labels.get(v).foreach { _.text = displayFunction(v) } }
			
			// Preserves selection, if possible
			if (oldValue.exists(newContent.contains))
				value = oldValue
			else
				value = None
		}
	}
	
	private class LabelMouseListener(val label: TextLabel) extends MouseButtonStateListener with Handleable
	{
		// ATTRIBUTES	----------
		
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
			MouseEvent.isOverAreaFilter(label.bounds)
		
		
		// IMPLEMENTED	---------
		
		// When pressed, selects the label
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			val newValue = labels.find { _._2 == label }.map { _._1 }
			if (newValue.isDefined)
			{
				value = newValue
				Some(ConsumeEvent(s"$newValue selected in TabSelection"))
			}
			else
				None
		}
	}
	
	// Draws the line under the selected item
	private object SelectionDrawer extends CustomDrawer
	{
		override def drawLevel = Background
		override def draw(drawer: Drawer, bounds: Bounds) =
			drawer.onlyFill(highlightColor).draw(bounds.bottomSlice(selectionLineHeight))
	}
}
