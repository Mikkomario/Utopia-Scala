package utopia.manuscript.pdf.write

import org.apache.pdfbox.pdmodel.font.PDFont
import utopia.flow.operator.combine.LinearScalable
import utopia.flow.util.Mutate
import utopia.flow.view.template.Extender
import utopia.paradigm.transform.{Adjustment, SizeAdjustable}

/**
  * A Scala-friendly wrapper for a [[PDFont]] class
  * @author Mikko Hilpinen
  * @since 31.12.2024, v2.0
  * @param wrapped Wrapped pdfbox font
  * @param size Size/height of this font in "font points"
  */
// TODO: Add support for dimension-based sizes, knowing that 72 points = 1 inch
case class Font(wrapped: PDFont, size: Double)
	extends Extender[PDFont] with LinearScalable[Font] with SizeAdjustable[Font]
{
	// IMPLEMENTED  --------------------------
	
	override def self: Font = this
	
	override def *(mod: Double): Font = mapSize { _ * mod }
	override protected def adjustedBy(impact: Int)(implicit adjustment: Adjustment): Font = this * adjustment(impact)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param newSize New font size to assign
	  * @return Copy of this font with the specified size
	  */
	def withSize(newSize: Double) = copy(size = newSize)
	/**
	  * @param f A mapping function applied to font size
	  * @return Copy of this font with a mapped size
	  */
	def mapSize(f: Mutate[Double]) = withSize(f(size))
}
