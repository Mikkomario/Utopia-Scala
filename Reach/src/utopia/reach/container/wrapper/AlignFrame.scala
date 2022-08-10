package utopia.reach.container.wrapper

import utopia.paradigm.color.Color
import utopia.reach.component.factory.ContextInsertableComponentFactoryFactory.ContextualBuilderContentFactory
import utopia.reach.component.factory.{BuilderFactory, ComponentFactoryFactory, ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, SimpleFilledBuilderFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas
import utopia.reflection.color.ComponentColor
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.container.stack.template.layout.AlignFrameLike2
import utopia.paradigm.enumeration.Alignment

object AlignFrame extends ContextInsertableComponentFactoryFactory[Any, AlignFrameFactory, ContextualAlignFrameFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new AlignFrameFactory(hierarchy)
}

class AlignFrameFactory(val parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[Any, ContextualAlignFrameFactory] with BuilderFactory[AlignFrameBuilder]
		with SimpleFilledBuilderFactory[ContextualFilledAlignFrameBuilder]
{
	// IMPLEMENTED  ------------------------------
	
	override def withContext[N <: Any](context: N) =
		ContextualAlignFrameFactory(this, context)
	
	override def build[FF](contentFactory: ComponentFactoryFactory[FF]) =
		new AlignFrameBuilder[FF](this, contentFactory)
	
	protected def makeBuilder[NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
		(background: ComponentColor, contentContext: NC, contentFactory: ContextualBuilderContentFactory[NC, F]) =
		new ContextualFilledAlignFrameBuilder[NC, F](this, background, contentContext, contentFactory)
	
	
	// OTHER    ----------------------------------
	
	/**
	 * Creates a new align frame
	 * @param content Content to place in this frame (open)
	 * @param alignment Alignment to use
	 * @param customDrawers Custom drawers to assign (default = empty)
	 * @tparam C Type of wrapped component
	 * @tparam R Type of additional creation result
	 * @return A new align frame
	 */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R], alignment: Alignment,
	                                      customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val frame = new AlignFrame(parentHierarchy, content.component, alignment, customDrawers)
		content attachTo frame
	}
}

case class ContextualAlignFrameFactory[N](factory: AlignFrameFactory, context: N)
	extends ContextualComponentFactory[N, Any, ContextualAlignFrameFactory]
{
	// COMPUTED ------------------------------
	
	/**
	 * @return A copy of this factory with no contextual information
	 */
	def withoutContext = factory
	
	
	// IMPLEMENTED  --------------------------
	
	override def withContext[N2 <: Any](newContext: N2) = copy(context = newContext)
	
	
	// OTHER    ------------------------------
	
	/**
	 * Creates a new builder based on this factory
	 * @param contentFactory A frame content creation factory factory
	 * @tparam F Type of component creation factory used
	 * @return A new builder
	 */
	def build[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
		(contentFactory: ContextualBuilderContentFactory[N, F]) =
		new ContextualAlignFrameBuilder(factory, context, contentFactory)
}

class AlignFrameBuilder[+F](factory: AlignFrameFactory, contentFactory: ComponentFactoryFactory[F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	 * Creates a new filled align frame
	 * @param alignment Alignment to use
	 * @param customDrawers Custom drawers to assign (default = empty)
	 * @param fill A function for creating container contents. Accepts component creation factory.
	 * @tparam C Type of wrapped component
	 * @tparam R Type of additional creation result
	 * @return A new align frame
	 */
	def apply[C <: ReachComponentLike, R](alignment: Alignment, customDrawers: Vector[CustomDrawer] = Vector())
	                                     (fill: F => ComponentCreationResult[C, R]) =
	{
		val content = Open.using(contentFactory)(fill)
		factory(content, alignment, customDrawers)
	}
}

class ContextualAlignFrameBuilder[N, +F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
(factory: AlignFrameFactory, context: N, contentFactory: ContextualBuilderContentFactory[N, F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	 * Creates a new filled align frame
	 * @param alignment Alignment to use
	 * @param customDrawers Custom drawers to assign (default = empty)
	 * @param fill A function for creating container contents. Accepts component creation factory.
	 * @tparam C Type of wrapped component
	 * @tparam R Type of additional creation result
	 * @return A new align frame
	 */
	def apply[C <: ReachComponentLike, R](alignment: Alignment, customDrawers: Vector[CustomDrawer] = Vector())
	                                     (fill: F[N] => ComponentCreationResult[C, R]) =
	{
		val content = Open.withContext(contentFactory, context)(fill)
		factory(content, alignment, customDrawers)
	}
}

class ContextualFilledAlignFrameBuilder[NC, +F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
(factory: AlignFrameFactory, background: Color, contentContext: NC,
 contentFactory: ContextualBuilderContentFactory[NC, F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	 * Creates a new filled align frame
	 * @param alignment Alignment to use
	 * @param customDrawers Custom drawers to assign (default = empty)
	 * @param fill A function for creating container contents. Accepts component creation factory.
	 * @tparam C Type of wrapped component
	 * @tparam R Type of additional creation result
	 * @return A new align frame
	 */
	def apply[C <: ReachComponentLike, R](alignment: Alignment, customDrawers: Vector[CustomDrawer] = Vector())
	                                     (fill: F[NC] => ComponentCreationResult[C, R]) =
	{
		val content = Open.withContext(contentFactory, contentContext)(fill)
		factory(content, alignment, BackgroundDrawer(background) +: customDrawers)
	}
}

/**
 * A container which contains a single item, aligned to some side
 * @author Mikko Hilpinen
 * @since 30.1.2021, v0.1
 */
class AlignFrame(override val parentHierarchy: ComponentHierarchy, override val content: ReachComponentLike,
                 override val alignment: Alignment, override val customDrawers: Vector[CustomDrawer] = Vector())
	extends CustomDrawReachComponent with AlignFrameLike2[ReachComponentLike]