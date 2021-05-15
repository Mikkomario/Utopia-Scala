package utopia.genesis.graphics
import utopia.flow.datastructure.immutable.Lazy
import utopia.genesis.shape.shape2D.{Matrix2D, Polygonic}
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * A drawer that relies on the state of the drawer above it
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
class DerivedDrawer(override protected val writeContext: WriteableGraphicsContext,
                    clipPieces: => (Polygonic, Option[Matrix3D])) extends Drawer2
{
	// ATTRIBUTES   -------------------------------
	
	private val clippingPointer = Lazy {
		val (base, transformation) = clipPieces
		transformation match
		{
			case Some(transformation) => base.transformedWith(transformation)
			case None => base
		}
	}
	
	override lazy val clipBounds = clippingPointer.value.bounds
	
	
	// IMPLEMENTED  -------------------------------
	
	override def transformedWith(transformation: Matrix2D): DerivedDrawer = transformedWith(transformation.to3D)
	
	override def transformedWith(transformation: Matrix3D) =
		new DerivedDrawer(writeContext.transformedWith(transformation), clippingPointer.current match
		{
			case Some(cached) => cached -> None
			case None =>
				val (base, firstTransformation) = clipPieces
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
