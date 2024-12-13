package utopia.reach.test

import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.reach.component.input.selection.DropDown
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.context.ReachContentWindowContext.apply
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
	val baseDdf = DropDown.withExpandAndCollapseIcon(Pair(expandIcon, shrinkIcon))
		.withPromptPointer(Fixed("Select One")).withoutListMargin
	
	val items = Map("Fruits" -> Vector("Apple", "Banana", "Kiwi"), "Minerals" -> Vector("Diamond", "Ruby", "Sapphire"))
	
	val window = ReachWindow.contentContextual.withWindowBackground(colors.gray.light)
		.using(Framing, title = "Drop-Down Test") { (_, framingF) =>
			framingF.build(Stack) { stackF =>
				stackF.mapContext { _.forTextComponents.borderless.nonResizable }.related.variable.build(baseDdf) { ddF =>
					val selectedCategoryPointer = EventfulPointer[Option[String]](None)
					val selectedItemPointer = EventfulPointer[Option[String]](None)
					
					Vector(
						ddF.withFieldName("Category")
							.simple(Fixed(items.keys.toVector.sorted), selectedCategoryPointer),
						ddF
							.withFieldNamePointer(selectedCategoryPointer.map {
								case Some(category) => category
								case None => "Item"
							})
							.withHintPointer(selectedCategoryPointer.map {
								case Some(_) => LocalizedString.empty
								case None => "Select category first"
							})
							.withNoOptionsViewConstructor { (hierarchy, context) =>
									ViewTextLabel(hierarchy).withContext(context).hint
										.text("Please select a category first")
							}
							.simple(
								selectedCategoryPointer.map {
									case Some(category) => items(category)
									case None => Vector()
								},
								selectedItemPointer)
					)
				}
			}
	}
	
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
