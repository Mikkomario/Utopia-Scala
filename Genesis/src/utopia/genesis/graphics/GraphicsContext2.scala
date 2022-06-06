package utopia.genesis.graphics

/**
  * Provides a read-only access to a graphics instance. Intended for transformation and font metric reading.
  * @author Mikko Hilpinen
  * @since 30.1.2022, v2.6.3
  */
class GraphicsContext2(protected override val graphics: LazyGraphics) extends GraphicsContextLike[GraphicsContext2]
{
	override protected def withGraphics(newGraphics: LazyGraphics) = new GraphicsContext2(graphics)
}
