package utopia.genesis.test

import utopia.genesis.graphics.{FontMetricsWrapper, MeasuredText}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.generic.ParadigmDataType

import javax.swing.JPanel

/**
  * Tests measured text functions
  * @author Mikko Hilpinen
  * @since 11.8.2022, v3.0
  */
object MeasuredTextTest extends App
{
	ParadigmDataType.setup()
	
	val panel = new JPanel()
	val fontMetrics: FontMetricsWrapper = panel.getFontMetrics(panel.getFont)
	
	println(s"Ascent: ${fontMetrics.ascent}\nDescent: ${fontMetrics.descent}\nLeading: ${fontMetrics.leading}\n")
	
	val s = "test"
	
	def printText(t: MeasuredText) = {
		println(s"\n\n${t.alignment} \t----------------\n")
		println(s"Bounds: ${t.bounds}")
		println(s"Line bounds: [${t.lineBounds.mkString(", \n")}]")
		println(s"Default draw targets: [${t.defaultDrawTargets.mkString(", \n")}]")
	}
	
	Alignment.values.foreach(a => printText(MeasuredText(s, fontMetrics, a)))
}
