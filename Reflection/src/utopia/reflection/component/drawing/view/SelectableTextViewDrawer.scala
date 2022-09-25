package utopia.reflection.component.drawing.view

import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
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
case class SelectableTextViewDrawer(textPointer: Changing[MeasuredText], stylePointer: View[TextDrawContext],
                                    selectedRangesPointer: Changing[Iterable[Range]] = Fixed(Vector()),
                                    caretPositionPointer: Changing[Option[Int]] = Fixed(None),
                                    highlightedTextColorPointer: View[Color] = View(Color.textBlack),
                                    highlightedBackgroundPointer: View[Option[Color]] = View(None),
                                    caretColorPointer: View[Color] = Fixed(Color.textBlack),
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
