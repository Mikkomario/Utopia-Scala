package utopia.reflection.component.drawing.view

import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.Viewable
import utopia.flow.view.template.eventful.ChangingLike
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.{Bounds, Point, Vector2D}
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{DrawLevel, SelectableTextDrawerLike}
import utopia.genesis.graphics.MeasuredText

/**
  * A view-based implementation of a text drawer that supports text selection
  * @author Mikko Hilpinen
  * @since 4.11.2020, v2
  */
case class SelectableTextViewDrawer(textPointer: ChangingLike[MeasuredText], stylePointer: Viewable[TextDrawContext],
									selectedRangesPointer: ChangingLike[Iterable[Range]] = Fixed(Vector()),
									caretPositionPointer: ChangingLike[Option[Int]] = Fixed(None),
									highlightedTextColorPointer: Viewable[Color] = View(Color.textBlack),
									highlightedBackgroundPointer: Viewable[Option[Color]] = View(None),
									caretColorPointer: Viewable[Color] = Fixed(Color.textBlack),
									caretWidth: Double = 1.0,
									override val drawLevel: DrawLevel = Normal)
	extends SelectableTextDrawerLike
{
	// ATTRIBUTES	------------------------------
	
	override protected val lastDrawStatusPointer = new PointerWithEvents(Point.origin -> Vector2D.identity)
	
	/**
	  * Pointer to the currently drawn targets
	  */
	val drawTargetsPointer = textPointer.lazyMergeWith(selectedRangesPointer) { _.drawTargets(_) }
	/**
	  * Pointer to the currently drawn caret
	  */
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
	
	override def caretColor = caretColorPointer.value
}
