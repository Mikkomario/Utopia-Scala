package utopia.firmament.drawing.view

import utopia.firmament.context.ComponentCreationDefaults.componentLogger
import utopia.firmament.drawing.template.SelectableTextDrawerLike
import utopia.firmament.model.TextDrawContext
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.{DrawLevel, MeasuredText}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
  * A view-based implementation of a text drawer that supports text selection
  * @author Mikko Hilpinen
  * @since 4.11.2020, Reflection v2
  */
case class SelectableTextViewDrawer(textPointer: Changing[MeasuredText], stylePointer: View[TextDrawContext],
                                    selectedRangesPointer: Changing[Iterable[Range]] = Fixed(Empty),
                                    caretPositionPointer: Changing[Option[Int]] = Fixed(None),
                                    highlightedTextColorPointer: View[Color] = View.fixed(Color.textBlack),
                                    highlightedBackgroundPointer: View[Option[Color]] = View.fixed(None),
                                    caretColorPointer: View[Color] = View.fixed(Color.textBlack),
                                    caretWidthPointer: View[Double] = View.fixed(1.0),
                                    override val drawLevel: DrawLevel = Normal)
	extends SelectableTextDrawerLike
{
	// ATTRIBUTES	------------------------------
	
	override protected val lastDrawStatusPointer = EventfulPointer(Point.origin -> Vector2D.identity)
	
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
			Bounds.between(caretLine.start, caretLine.end + X(caretWidthPointer.value))
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
