package utopia.genesis.graphics

import utopia.flow.datastructure.immutable.Lazy
import utopia.genesis.shape.shape2D.{Matrix2D, Polygonic}
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * A drawer that relies on the state of the drawer above it
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
class DerivedDrawer(parentGraphics: => (ClosingGraphics, Option[Matrix3D], Seq[ClosingGraphics => Unit]),
                    parentClip: => (Polygonic, Option[Matrix3D])) extends Drawer2
{
	// ATTRIBUTES   -------------------------------
	
	private val graphicsPointer = Lazy {
		val (base, transformation, mutators) = parentGraphics
		transformation match
		{
			// Case: Transformation to apply
			case Some(transformation) =>
				val newGraphics = base.createChild()
				newGraphics.transform(transformation)
				// Also applies mutations
				mutators.foreach { _(newGraphics) }
				newGraphics
			// Case: No transformation
			case None =>
				// Case: Mutators to apply => Creates a new graphics instance
				if (mutators.nonEmpty)
				{
					val newGraphics = base.createChild()
					mutators.foreach { _(newGraphics) }
					newGraphics
				}
				// Case: No changes => Uses the existing graphics instance
				else
					base
		}
	}
	private val clippingPointer = Lazy {
		val (base, transformation) = parentClip
		transformation match
		{
			case Some(transformation) => base.transformedWith(transformation)
			case None => base
		}
	}
	
	override lazy val clipBounds = clippingPointer.value.bounds
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def graphics = graphicsPointer.value
	
	override def transformedWith(transformation: Matrix2D): DerivedDrawer = transformedWith(transformation.to3D)
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedDrawer(
			graphicsPointer.current match
			{
				case Some(cached) => (cached, Some(transformation), Vector())
				case None =>
					val (base, firstTransformation, mutators) = parentGraphics
					val combinedTransformation = firstTransformation match
					{
						case Some(t) => t(transformation)
						case None => transformation
					}
					(base, Some(combinedTransformation), mutators)
			},
			clippingPointer.current match
			{
				case Some(cached) => cached -> None
				case None =>
					val (base, firstTransformation) = parentClip
					// TODO: There's a good chance that this is not correct. Test and tweak first.
					val clippingTransformation = transformation.inverse match
					{
						case Some(inverseTransformation) =>
							Some(firstTransformation match
							{
								case Some(first) => first(inverseTransformation)
								case None => inverseTransformation
							})
						case None => firstTransformation
					}
					base ->  clippingTransformation
			})
}
