package utopia.firmament.drawing.view

import utopia.firmament.drawing.immutable.ImageDrawer
import utopia.firmament.factory.VariableFramedFactory
import utopia.firmament.model.stack.StackInsets
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.{DrawLevel, FromDrawLevelFactory}
import utopia.genesis.image.Image
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.transform.LinearTransformable

import scala.annotation.unused
import scala.language.implicitConversions

object ViewImageDrawer
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A factory used for constructing view-based image drawers.
	  * Set up with static default settings.
	  */
	val factory = ViewImageDrawerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: ViewImageDrawer.type): ViewImageDrawerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class ViewImageDrawerFactory(transformationView: View[Option[Matrix2D]] = View.fixed(None),
	                                  insetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	                                  alignmentView: View[Alignment] = View.fixed(Center), drawLevel: DrawLevel = Normal,
	                                  upscales: Boolean = false)
		extends LinearTransformable[ViewImageDrawerFactory] with VariableFramedFactory[ViewImageDrawerFactory]
			with FromAlignmentFactory[ViewImageDrawerFactory] with FromDrawLevelFactory[ViewImageDrawerFactory]
	{
		// IMPLEMENTED  ---------------------------
		
		override def identity: ViewImageDrawerFactory = this
		
		override def apply(alignment: Alignment): ViewImageDrawerFactory = withAlignmentView(View.fixed(alignment))
		override def apply(drawLevel: DrawLevel): ViewImageDrawerFactory = copy(drawLevel = drawLevel)
		override def withInsetsPointer(p: Changing[StackInsets]): ViewImageDrawerFactory = copy(insetsPointer = p)
		override def transformedWith(transformation: Matrix2D): ViewImageDrawerFactory = mapTransformation {
			case Some(t) => Some(t * transformation)
			case None => Some(transformation)
		}
		
		
		// OTHER    ------------------------------
		
		def withTransformationView(v: View[Option[Matrix2D]]) = copy(transformationView = v)
		def mapTransformation(f: Mutate[Option[Matrix2D]]) =
			withTransformationView(transformationView.mapValue(f))
		
		def withAlignmentView(v: View[Alignment]) = copy(alignmentView = v)
		
		/**
		  * @param imageView A view that contains the image to display
		  * @return A drawer that draws the viewed image
		  */
		def apply(imageView: View[Image]) =
			new ViewImageDrawer(imageView, transformationView, insetsPointer, alignmentView, drawLevel, upscales)
		/**
		  * @param image The image to display
		  * @return A drawer that draws that image
		  */
		def apply(image: Image): ViewImageDrawer = apply(View.fixed(image))
	}
}

/**
  * A view-based drawer used for drawing images
  * @author Mikko Hilpinen
  * @since 23/02/2024, v1.3
  */
class ViewImageDrawer(imageView: View[Image], transformationView: View[Option[Matrix2D]] = View.fixed(None),
                      insetsView: View[StackInsets] = View.fixed(StackInsets.any),
                      alignmentView: View[Alignment] = View.fixed(Center), override val drawLevel: DrawLevel = Normal,
                      override val useUpscaling: Boolean = false)
	extends ImageDrawer
{
	override def image: Image = imageView.value
	override def transformation: Option[Matrix2D] = transformationView.value
	override def insets: StackInsets = insetsView.value
	override def alignment: Alignment = alignmentView.value
}
