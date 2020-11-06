package utopia.reflection.component.drawing.view

import utopia.flow.datastructure.immutable.View
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.datastructure.template.Viewable
import utopia.flow.event.Changing
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Bounds, Point, Vector2D}
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{DrawLevel, SelectableTextDrawerLike}
import utopia.reflection.text.MeasuredText

/**
  * A view-based implementation of a text drawer that supports text selection
  * @author Mikko Hilpinen
  * @since 4.11.2020, v2
  */
case class SelectableTextViewDrawer(textPointer: Changing[MeasuredText], stylePointer: Viewable[TextDrawContext],
									selectedRangesPointer: Changing[Iterable[Range]],
									caretPositionPointer: Changing[Option[Int]],
									highlightedTextColorPointer: Viewable[Color],
									highlightedBackgroundPointer: Viewable[Option[Color]] = View(None),
									override val caretColor: Color = Color.textBlack, caretWidth: Double = 1.0,
									override val drawLevel: DrawLevel = Normal)
	extends SelectableTextDrawerLike
{
	// ATTRIBUTES	------------------------------
	
	override protected val lastDrawStatusPointer = new PointerWithEvents(Point.origin -> Vector2D.identity)
	
	val drawTargetsPointer = textPointer.lazyMergeWith(selectedRangesPointer) { _.drawTargets(_) }
	val caretPointer = textPointer.lazyMergeWith(caretPositionPointer) { (text, caretPosition) =>
		caretPosition.map { caretPosition =>
			val caretLine = text.caretAt(caretPosition)
			Bounds.between(caretLine.start, caretLine.end.plusX(caretWidth))
		}
	}
	
	
	// IMPLEMENTED	------------------------------
	
	override def text = textPointer.value
	
	override def drawTargets = drawTargetsPointer.value
	
	override def caret = caretPointer.value
	
	override def font = stylePointer.value.font
	
	override def insets = stylePointer.value.insets
	
	override def normalTextColor = stylePointer.value.color
	
	override def highlightedTextColor = highlightedTextColorPointer.value
	
	override def highlightedTextBackground = highlightedBackgroundPointer.value
}
