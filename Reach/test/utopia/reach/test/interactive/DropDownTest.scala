package utopia.reach.test.interactive

import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.flow.collection.CollectionExtensions.RichIterableOnce
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.reach.component.interactive.input.selection.DropDown
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext
import utopia.reach.window.ReachWindow

/**
  * A test application with drop down fields
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDownTest extends App
{
	import ReachTestContext._
	
	val arrowImage = Image.readFrom("Reflection/test-images/arrow-back-48dp.png")
	arrowImage.log
	val expandIcon = arrowImage.map { i => SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationCounterClockwise)) }
		.getOrElse(SingleColorIcon.empty)
	val shrinkIcon = arrowImage.map { i => SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationClockwise)) }
		.getOrElse(SingleColorIcon.empty)
	val baseDdf = DropDown.withExpandAndCollapseIcon(expandIcon, shrinkIcon)
		.withPromptPointer(Fixed("Select One")).withoutMarginInSelection.mapScrollSettings { _.withMaxOptimalHeight(240) }
	
	val items = Map(
		"Fruits" -> Vector("Apple", "Banana", "Kiwi", "Orange", "Strawberry", "Dragonfruit"),
		"Minerals" -> Vector("Diamond", "Ruby", "Sapphire"))
	
	val window = ReachWindow.contentContextual.withWindowBackground(colors.gray.light)
		.using(Framing, title = "Drop-Down Test") { (_, framingF) =>
			framingF.build(Stack) { stackF =>
				stackF.related.build(baseDdf) { ddF =>
					val selectedCategoryPointer = EventfulPointer[Option[String]](None)
					val selectedItemPointer = EventfulPointer[Option[String]](None)
					
					Pair(
						ddF.withFieldName("Category")
							.labels(Fixed(items.keys.toOptimizedSeq.sorted), selectedCategoryPointer),
						ddF
							.withFieldNamePointer(selectedCategoryPointer.map {
								case Some(category) => category
								case None => "Item"
							})
							.withHintPointer(selectedCategoryPointer.map {
								case Some(_) => LocalizedString.empty
								case None => "Select category first"
							})
							.withNoOptionsViewConstructor { factories =>
								factories(ViewTextLabel).hint.text("Please select a category first")
							}
							.labels(
								selectedCategoryPointer.map {
									case Some(category) => items(category)
									case None => Empty
								},
								selectedItemPointer)
					)
				}
			}
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
}
