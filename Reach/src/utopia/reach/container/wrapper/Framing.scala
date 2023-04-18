package utopia.reach.container.wrapper

import utopia.firmament.component.container.single.FramingLike
import utopia.firmament.context.BaseContextLike
import utopia.firmament.drawing.immutable.{BackgroundDrawer, RoundedBackgroundDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.paradigm.color.Color
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.factory.{BuilderFactory, ComponentFactoryFactory, FromGenericContextFactory, GenericContextualFactory, SimpleFilledBuilderFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas2

object Framing extends Cff[FramingFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = FramingFactory(hierarchy)
}

case class FramingFactory(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[Any, ContextualFramingFactory] with BuilderFactory[FramingBuilder]
		with SimpleFilledBuilderFactory[ContextualFilledFramingBuilder]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: Any](context: N) = ContextualFramingFactory(this, context)
	
	override def build[F](contentFactory: ComponentFactoryFactory[F]) =
		FramingBuilder(this, contentFactory)
	
	protected def makeBuilder[NC, F](background: Color, contentContext: NC, contentFactory: Ccff[NC, F]) =
		new ContextualFilledFramingBuilder[NC, F](this, background, contentContext, contentFactory)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new framing
	  * @param content A component that is yet to attach to a component hierarchy
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @tparam C Type of wrapped component
	  * @return A new framing and the produced content component
	  */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R], insets: StackInsetsConvertible,
										  customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val framing = new Framing(parentHierarchy, content, insets.toInsets, customDrawers)
		// Closes the content
		content.attachTo(framing)
	}
}

object ContextualFramingFactory
{
	implicit class BackgroundSensitiveFramingFactory[NT](val f: ContextualFramingFactory[_ <: BaseContextLike[_, NT]])
		extends AnyVal
	{
		/**
		  * @param background Background color used in the created framing(s)
		  * @param contentFactory Factory that will produce the component factories used
		  * @param mapContext A function for altering this framing's context
		  * @tparam NC Type of context used when creating framing contents
		  * @tparam F Type of component creation factory used
		  * @return A framing builder that fills the framing area with a background color
		  */
		def buildFilledWithMappedContext[NC, F](background: Color, contentFactory: Ccff[NC, F])
		                                       (mapContext: NT => NC) =
			new ContextualFilledFramingBuilder[NC, F](f.factory, background,
				mapContext(f.context.against(background)), contentFactory)
		
		/**
		  * @param background Background color used in the created framing(s)
		  * @param contentFactory Factory that will produce the component factories used
		  * @tparam F Type of component creation factory used
		  * @return A framing builder that fills the framing area with a background color
		  */
		def buildFilled[F](background: Color, contentFactory: Ccff[NT, F]) =
			new ContextualFilledFramingBuilder[NT, F](f.factory, background, f.context.against(background), contentFactory)
	}
}

case class ContextualFramingFactory[N](factory: FramingFactory, context: N)
	extends GenericContextualFactory[N, Any, ContextualFramingFactory]
{
	// COMPUTED	----------------------------
	
	/**
	  * @return A copy of this factory with no contextual data attached
	  */
	def withoutContext = factory
	
	
	// IMPLEMENTED	------------------------
	
	override def withContext[N2 <: Any](newContext: N2) = copy(context = newContext)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param contentFactory A factory used for creating contextual component factories
	  * @tparam F Type of component factories used
	  * @return A new framing builder that will also construct framing contents
	  */
	def build[F[X <: N]](contentFactory: Gccff[N, F]) =
		new ContextualFramingBuilder[N, F](factory, context, contentFactory)
}

case class FramingBuilder[+F](framingFactory: FramingFactory, contentFactory: ComponentFactoryFactory[F])
{
	/**
	  * Creates a new framing and content inside it
	  * @param insets Insets to place around the content
	  * @param customDrawers Custom drawers to assign to this framing (default = empty)
	  * @param fill A function for creating the content that will fill this framing
	  * @tparam C Type of created content
	  * @tparam R Type of additional creation result
	  * @return A new framing filled with content
	  */
	def apply[C <: ReachComponentLike, R](insets: StackInsetsConvertible,
										  customDrawers: Vector[CustomDrawer] = Vector())
										 (fill: F => ComponentCreationResult[C, R]) =
	{
		val newContent = Open.using(contentFactory)(fill)(framingFactory.parentHierarchy.top)
		framingFactory(newContent, insets, customDrawers)
	}
}

class ContextualFramingBuilder[N, +F[X <: N]](factory: FramingFactory, context: N, contentFactory: Gccff[N, F])
{
	private implicit def canvas: ReachCanvas2 = factory.parentHierarchy.top
	
	/**
	  * Builds a framing with content
	  * @param insets Insets placed around the content
	  * @param customDrawers Custom drawers to assign to this framing (default = empty)
	  * @param fill A function for creating the contents of this framing. Accepts a contextual component factory.
	  * @tparam C Type of component wrapped in this framing
	  * @tparam R Type of additional creation result
	  * @return A framing filled with created content
	  */
	def apply[C <: ReachComponentLike, R](insets: StackInsetsConvertible,
										  customDrawers: Vector[CustomDrawer] = Vector())
										 (fill: F[N] => ComponentCreationResult[C, R]) =
	{
		val content = Open.withContext(context)(contentFactory)(fill)
		factory(content, insets, customDrawers)
	}
}

class ContextualFilledFramingBuilder[N, +F](factory: FramingFactory, background: Color, context: N,
                                            contentFactory: Ccff[N, F])
{
	// IMPLICIT	-------------------------------
	
	private implicit def canvas: ReachCanvas2 = factory.parentHierarchy.top
	
	
	// OTHER	-------------------------------
	
	/**
	  * Creates a new framing with background color
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy and component creation
	  *                context are known
	  * @tparam C Type of wrapped component
	  * @tparam R Type of additional component creation result
	  * @return A new framing and the produced content component
	  */
	def apply[C <: ReachComponentLike, R](insets: StackInsetsConvertible,
										  moreCustomDrawers: Vector[CustomDrawer] = Vector())
										 (content: F => ComponentCreationResult[C, R]) =
		_apply(insets, BackgroundDrawer(background) +: moreCustomDrawers)(content)
	
	/**
	  * Creates a new framing with background color and rounding at the four corners
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy and component creation
	  *                context are known
	  * @tparam C Type of wrapped component
	  * @tparam R Type of additional component creation result
	  * @return A new framing and the produced content component
	  */
	def rounded[C <: ReachComponentLike, R](insets: StackInsetsConvertible,
											moreCustomDrawers: Vector[CustomDrawer] = Vector())
										   (content: F => ComponentCreationResult[C, R]) =
	{
		val activeInsets = insets.toInsets
		// The rounding amount is based on insets
		val drawer = activeInsets.lengths.map { _.optimal }.filter { _ > 0.0 }.minOption match
		{
			case Some(minSideLength) => RoundedBackgroundDrawer.withRadius(background, minSideLength)
			// If the insets default to 0, uses solid background drawing instead
			case None => BackgroundDrawer(background)
		}
		_apply(activeInsets, drawer +: moreCustomDrawers)(content)
	}
	
	private def _apply[C <: ReachComponentLike, R](insets: StackInsetsConvertible,
												   customDrawers: Vector[CustomDrawer] = Vector())
												  (fill: F => ComponentCreationResult[C, R]) =
	{
		val newContent = Open.withContext(context)(contentFactory)(fill)
		factory(newContent, insets, customDrawers)
	}
}

/**
  * A reach implementation of the framing trait which places insets or margins around a wrapped component
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class Framing(override val parentHierarchy: ComponentHierarchy, override val content: ReachComponentLike,
			  override val insets: StackInsets, override val customDrawers: Vector[CustomDrawer] = Vector())
	extends CustomDrawReachComponent with FramingLike[ReachComponentLike]