package utopia.genesis.graphics

import utopia.paradigm.color.Color
import utopia.flow.collection.CollectionExtensions._

import java.awt.Paint
import scala.language.implicitConversions

object DrawSettings
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * Draw settings that don't specify anything
	  */
	val empty = new DrawSettings(None, None)
	
	
	// IMPLICIT -----------------------------
	
	// StrokeSettings may implicitly be interpreted as DrawSettings without fillColor
	implicit def strokeToDraw(s: StrokeSettings): DrawSettings = apply(None, Some(s))
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param fillColor Color to use when filling shapes
	  * @param strokeSettings Settings to use when drawing edges
	  * @return A new settings instance
	  */
	private def _apply(fillColor: Color, strokeSettings: StrokeSettings): DrawSettings =
		apply(Some(Right(fillColor)), Some(strokeSettings))
	
	/**
	  * @param fillColor Color to use when filling shapes
	  * @param strokeSettings Settings to use when drawing edges (implicit)
	  * @return A new settings instance
	  */
	def apply(fillColor: Color)(implicit strokeSettings: StrokeSettings): DrawSettings =
		_apply(fillColor, strokeSettings)
	
	/**
	  * @param fillColor Color to use when filling shapes
	  * @return A new settings instance where edges are not drawn
	  */
	def onlyFill(fillColor: Color) = apply(Some(Right(fillColor)), None)
}

/**
  * An object used for specifying shape drawing settings, like color and stroke
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
case class DrawSettings(_fill: Option[Either[Paint, Color]], strokeSettings: Option[StrokeSettings])
{
	// COMPUTED ---------------------------
	
	/**
	  * @return Whether filling operations should be performed when using these settings. False if no fill color is
	  *         specified or if the color is fully transparent.
	  */
	def shouldFill = _fill.exists {
		case Right(c) => c.alpha > 0
		case Left(_) => true
	}
	/**
	  * @return Whether edge drawing operations should be performed when using these settings. False if no edge
	  *         drawing settings are specified or if the settings would draw invisible edges.
	  */
	def shouldDrawEdges = strokeSettings.exists { _.shouldDraw }
	/**
	  * @return Whether any drawing operations should be performed when using these settings
	  */
	def shouldDraw = shouldFill || shouldDrawEdges
	
	/**
	  * @return Color used when filling shapes. None if shapes are not configured to be filled.
	  */
	def fillColor = _fill.flatMap {
		case Right(c) => Some(c)
		case Left(p) =>
			p match {
				case c: java.awt.Color => Some(Color.fromAwt(c))
				case _ => None
			}
	}
	/**
	  * @return Paint used when filling shapes. None if shapes are not configured to be filled.
	  */
	def fillPaint = _fill.map { _.leftOrMap { _.toAwt } }
	/**
	  * @return Paint used when filling shapes.
	  *         None if shapes are not configured to be filled or if the paint is transparent.
	  */
	def visibleFillPaint = _fill.flatMap {
		case Right(c) => if (c.alpha > 0) Some(c.toAwt) else None
		case Left(p) => Some(p)
	}
	
	/**
	  * @return Color to use when drawing edges
	  */
	def edgeColor = strokeSettings.map { _.color }
	/**
	  * @return Paint used when drawing edges
	  */
	def edgePaint = strokeSettings.map { _.paint }
	
	/**
	  * @return Settings used when drawing edges. None if no edge drawing should be done.
	  */
	def visibleStrokeSettings = strokeSettings.filter { _.shouldDraw }
	
	/**
	  * @return A copy of these settings where no fill drawing is performed
	  */
	def withoutFill = if (_fill.isEmpty) this else copy(_fill = None)
	/**
	  * @return A copy of these settings where no edge drawing is performed
	  */
	def withoutEdges = if (strokeSettings.isEmpty) this else copy(strokeSettings = None)
	
	/**
	  * @return A copy of these settings where drawn strokes are rounded
	  */
	def withRoundedStrokes = mapStrokeSettings { _.rounded }
	/**
	  * @return A copy of these settings where drawn strokes are sharp
	  */
	def withSharpStrokes = mapStrokeSettings { _.sharp }
	
	
	// OTHER    --------------------------
	
	/**
	  * @param color Color to use when filling shapes
	  * @return A copy of these settings where that color is used when filling shapes
	  */
	def withFillColor(color: Color) =
		if (_fill.exists { _.contains(color) }) this else copy(_fill = Some(Right(color)))
	/**
	  * @param color Color to use when filling shapes
	  * @return A copy of these settings where that color is used when filling shapes
	  */
	def withFillColor(color: Option[Color]): DrawSettings = color match {
		case Some(c) => withFillColor(c)
		case None => withoutFill
	}
	/**
	  * @param paint Paint used when filling shapes
	  * @return A copy of these settings where that paint is used when filling shapes
	  */
	def withFillPaint(paint: Paint) = copy(_fill = Some(Left(paint)))
	
	/**
	  * @param strokeSettings Settings to use when drawing shape edges
	  * @return An updated copy of these settings
	  */
	def withStrokeSettings(strokeSettings: StrokeSettings) = copy(strokeSettings = Some(strokeSettings))
	/**
	  * @param f A function for mapping used stroke settings. Receives the default StrokeSettings instance if these
	  *          settings don't specify any strokeSettings at this time.
	  * @return A copy of these settings where stroke settings have been modified / mapped
	  */
	def mapStrokeSettings(f: StrokeSettings => StrokeSettings) =
		withStrokeSettings(f(strokeSettings.getOrElse(StrokeSettings.default)))
	
	/**
	  * @param color Color to use when drawing edges
	  * @return A copy of these settings with the specified edge-drawing color
	  */
	def withEdgeColor(color: Color) = mapStrokeSettings { _.withColor(color) }
	/**
	  * @param color Color to use when drawing edges
	  * @return A copy of these settings with the specified edge-drawing color
	  */
	def withEdgeColor(color: Option[Color]): DrawSettings = color match {
		case Some(c) => withEdgeColor(c)
		case None => withoutEdges
	}
	/**
	  * @param paint Paint to use when drawing edges
	  * @return A copy of these settings with the specified edge-drawing paint
	  */
	def withEdgePaint(paint: Paint) = mapStrokeSettings { _.withPaint(paint) }
	/**
	  * @param width Stoke width in pixels
	  * @return A copy of these settings using the specified stroke width
	  */
	def withStrokeWidth(width: Double) = mapStrokeSettings { _.withStrokeWidth(width) }
}
