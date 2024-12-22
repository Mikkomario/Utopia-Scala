package utopia.reach.test.interactive

import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.modifier.NoShrinkingLengthModifier
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.image.{CompositeScalingImage, Image}
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.selection.Slider
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

import java.nio.file.Paths

/**
  * Tests displaying a composite image as a scalable icon
  * @author Mikko Hilpinen
  * @since 21.12.2024, v1.5
  */
object CompositeImageTest extends App
{
	private val icon = SingleColorIcon(CompositeScalingImage(
		Paths.get("Reach/test-images")
			.tryIterateChildren { _.filter { _.fileName.contains("smart-toy") }.toOptimizedSeq
				.tryMap { Image.readFrom(_) } }
			.get))
	
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(Stack) { stackF =>
			stackF.centered.build(Mixed) { factories =>
				val slider = factories(Slider).forDoubles(NumericSpan(0.2, 1.0))(240.any, 1.0)
				val scalingPointer = slider.valuePointer
				
				val imageLabel = factories(ViewImageLabel).withImageScalingPointer(scalingPointer).icon(icon)
				imageLabel.addConstraint(new NoShrinkingLengthModifier().symmetric)
				
				Pair(imageLabel, slider)
			}
		}
	}
	
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
