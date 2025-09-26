package utopia.reach.test.interactive

import utopia.firmament.model.enumeration.SizeCategory.Medium
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment.TopLeft
import utopia.reach.component.factory.Mixed
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.Creation
import utopia.reach.container.layered.{LayerPositioning, Layers}
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
 * @author Mikko Hilpinen
 * @since 25.09.2025, v1.7
 */
object LayersTest2 extends App
{
	private val window = ReachWindow.contentContextual.using(Layers) { (_, layersF) =>
		layersF.build(Framing, TextLabel) { (framingF, labelF) =>
			// Determines the color to use for the header label.
			// Attempts to make it visible against both backgrounds
			val areaBg = Color.cyan
			val backgroundColors = Pair(layersF.context.background, areaBg)
			val headerColor = Pair(Color.textBlack, Color.textWhite).maxBy { textC =>
				backgroundColors.map(textC.contrastAgainst).min
			}
			
			// Creates the header and the content
			val headerLabel = labelF.withTextColor(headerColor)
				.withoutTextInsets.withHorizontalTextInsets(Medium).apply("Test")
			val halfHeaderHeight = headerLabel.stackSize.height / 2
			// Uses two levels of framing:
			//      1. To add space for the header
			//      2. To add the background color & standard framing
			val content = framingF.withoutInsets.withTop(halfHeaderHeight).build(Framing) { framingF =>
				framingF.withBackground(areaBg).small.withTop(halfHeaderHeight).build(Mixed) { factories =>
					factories(EmptyLabel).apply(StackSize.twice(320.any))
				}
			}
			
			val positioning = LayerPositioning.AlignedToSide(TopLeft, expandIfPossible = false)
			Creation(content.parent -> Single(headerLabel -> positioning), content.result)
		}
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
}
