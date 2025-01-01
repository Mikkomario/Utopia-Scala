package utopia.manuscript.pdf.write

import org.apache.pdfbox.pdmodel.{PDPage, PDPageContentStream}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.ResettableFlag

/**
  * An interface for writing text contents on an individual PDF page
  * @author Mikko Hilpinen
  * @since 31.12.2024, v2.0
  */
class PdfPageWriter(page: PDPage, stream: PDPageContentStream)(implicit log: Logger)
{
	// ATTRIBUTES   ----------------------
	
	private val writingFlag = ResettableFlag()
	private val appliedFontPointer = Pointer.eventful.empty[Font]
	
	
	// INITIAL CODE ----------------------
	
	// Controls the beginText() and endText() by tracking the writingFlag
	writingFlag.addListener { e =>
		if (e.newValue) stream.beginText() else stream.endText()
	}
	// Applies the font changes automatically during writing mode
	appliedFontPointer.addListenerWhile(writingFlag) { e =>
		e.newValue.foreach { font => stream.setFont(font, font.size.toFloat) }
	}
	
	// page.setMediaBox()
	// page.getMediaBox
}
