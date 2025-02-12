package utopia.reflection.component.swing.input

import utopia.firmament.component.input.SelectableWithPointers
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.graphics.DrawLevel.Background
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseEvent}
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack

import scala.collection.immutable.HashMap

object TabSelection
{
	def contextual[A](displayFunction: DisplayFunction[A] = DisplayFunction.raw, initialChoices: Seq[A] = Vector(),
	                  background: Option[Color] = None)
	                 (implicit context: StaticTextContext) =
	{
		val (bg, isOpaque, fieldContext) = background match {
			case Some(bg) => (bg, true, context.against(bg))
			case _ => (context.background, false, context)
		}
		
		val yMargin = context.textInsets.vertical
		val field = new TabSelection[A](fieldContext.font, fieldContext.color.secondary,
			fieldContext.textInsets.horizontal.optimal, yMargin, fieldContext.margins.small, displayFunction,
			initialChoices, fieldContext.textColor)
		if (isOpaque)
			field.background = bg
		field
	}
	
	/**
	  * Creates a new tab selection using contextual information
	  * @param displayFunction Display function used for selectable values (default = displayed as is)
	  * @param initialChoices Initially selectable choices (default = empty)
	  * @param context Component creation context
	  * @tparam A Type of selected item
	  * @return A new tab selection
	  */
	def contextualWithBackground[A](background: Color, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
									initialChoices: Seq[A] = Vector())(implicit context: StaticTextContext) =
		contextual(displayFunction, initialChoices, Some(background))
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
	with AwtContainerRelated with SelectableWithPointers[Option[A], Seq[A]] with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	-------------------
	
	private var _textColor = initialTextColor
	
	private val stack = Stack.row[TextLabel](0.fixed)
	private val textInsets = StackInsets.vertical(vMargin, vMargin + selectionLineHeight) +
		StackInsets.horizontal(optimalHMargin.any.expanding)
	
	private var labels: Map[A, TextLabel] = HashMap()
	
	override val valuePointer = EventfulPointer[Option[A]](None)
	override val contentPointer = EventfulPointer[Seq[A]](Vector())
	
	
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
	def textColor_=(newColor: Color) = {
		if (_textColor != newColor) {
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
		
		override def onChangeEvent(event: ChangeEvent[Option[A]]) = {
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
	
	private class LabelMouseListener(val label: TextLabel) extends MouseButtonStateListener
	{
		// ATTRIBUTES	----------
		
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent.filter.leftPressed && MouseEvent.filter.over(label.bounds)
		
		
		// IMPLEMENTED	---------
		
		override def handleCondition: Flag = AlwaysTrue
		
		// When pressed, selects the label
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) =
		{
			val newValue = labels.find { _._2 == label }.map { _._1 }
			if (newValue.isDefined) {
				value = newValue
				Consume(s"$newValue selected in TabSelection")
			}
			else
				Preserve
		}
	}
	
	// Draws the line under the selected item
	private object SelectionDrawer extends CustomDrawer
	{
		private implicit val ds: DrawSettings = DrawSettings.onlyFill(highlightColor)
		
		override def opaque = false
		override def drawLevel = Background
		override def draw(drawer: Drawer, bounds: Bounds) = drawer.draw(bounds.bottomSlice(selectionLineHeight))
	}
}
