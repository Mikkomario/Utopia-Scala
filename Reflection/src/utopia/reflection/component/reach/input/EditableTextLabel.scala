package utopia.reflection.component.reach.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Changing
import utopia.flow.util.TimeExtensions._
import utopia.genesis.color.Color
import utopia.genesis.handling.Actor
import utopia.genesis.shape.shape2D.{Bounds, Line, Point}
import utopia.genesis.util.Drawer
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Foreground
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.ViewTextLabel
import utopia.reflection.component.reach.template.{Focusable, MutableCustomDrawReachComponent, MutableFocusable, ReachComponentWrapper}
import utopia.reflection.component.template.text.MutableTextComponent
import utopia.reflection.event.{FocusChangeEvent, FocusChangeListener, FocusListener, FocusStateTracker}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.text.{FontMetricsContext, MeasuredText, Regex}
import utopia.reflection.localization.LocalString._

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for requesting user input in text format
  * @author Mikko Hilpinen
  * @since 30.10.2020, v2
  */
class EditableTextLabel(override val parentHierarchy: ComponentHierarchy,
						val stylePointer: PointerWithEvents[TextDrawContext],
						caretColor: Color, selectionBackgroundColor: Color, selectedTextColor: Color = Color.textBlack,
						caretWidth: Double = 1.0, caretBlinkFrequency: Duration = 0.5.seconds,
						val textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
						inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
						allowLineBreaks: Boolean = true)
	extends MutableCustomDrawReachComponent with MutableTextComponent with MutableFocusable
{
	// ATTRIBUTES	-------------------------------
	
	override var focusListeners: Seq[FocusListener] = Vector(FocusHandler)
	
	val measuredTextPointer = textPointer.mergeWith(stylePointer) { (text, style) =>
		MeasuredText(text.noLanguageLocalizationSkipped, FontMetricsContext(fontMetrics(style.font),
			style.betweenLinesMargin), style.alignment, allowLineBreaks)
	}
	private val caretIndexPointer = new PointerWithEvents(textPointer.value.length - 1)
	private val caretVisibilityPointer = new PointerWithEvents(false)
	
	private var selectionStartIndex: Option[Int] = None
	
	
	// INITIAL CODE	-------------------------------
	
	enableFocusHandlingWhileLinked()
	
	
	// COMPUTED	-----------------------------------
	
	def hasFocus = FocusHandler.hasFocus
	
	def caretIndex = caretIndexPointer.value
	
	def text_=(newText: String) = textPointer.value = newText
	
	
	// IMPLEMENTED	-------------------------------
	
	override def text_=(newText: LocalizedString) = textPointer.value = newText.string
	
	override def updateLayout() = ()
	
	override def drawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	override def measuredText = measuredTextPointer.value
	
	override def drawContext = stylePointer.value
	
	override def allowTextShrink = false
	
	override def allowsFocusEnter = ???
	
	override def allowsFocusLeave = ???
	
	
	// OTHER	----------------------------------
	
	/*
	private def caretsFrom(text: String) =
	{
		val lineHeight = wrapped.singleLineTextHeight
		val lines = text.linesIterator.toVector
		lines.zipWithIndex.flatMap { case (line, index) =>
			val topY = (index * lineHeight) + ((index - 1) max 0) * wrapped.betweenLinesMargin
			val bottomY = topY + lineHeight
			line.indices.map { i =>
				val x = wrapped.textWidthWith(line.take(i + 1))
				Line(Point(x, topY), Point(x, bottomY))
			}
		}
	}*/
	
	
	// NESTED	----------------------------------
	
	private object FocusHandler extends FocusChangeListener
	{
		// ATTRIBUTES	--------------------------
		
		var hasFocus = false
		
		
		// IMPLEMENTED	--------------------------
		
		override def onFocusChangeEvent(event: FocusChangeEvent) =
		{
			hasFocus = event.hasFocus
			if (hasFocus)
				CaretBlinker.resetCounter()
		}
	}
	
	private object CaretBlinker extends Actor
	{
		// ATTRIBUTES	--------------------------
		
		private var passedDuration = Duration.Zero
		
		
		// IMPLEMENTED	--------------------------
		
		override def act(duration: FiniteDuration) =
		{
			passedDuration += duration
			if (passedDuration >= caretBlinkFrequency)
			{
				resetCounter()
				caretVisibilityPointer.update { !_ }
			}
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus
		
		
		// OTHER	------------------------------
		
		def resetCounter() = passedDuration = Duration.Zero
	}
}
