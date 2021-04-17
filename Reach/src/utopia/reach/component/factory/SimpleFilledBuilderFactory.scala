package utopia.reach.component.factory

import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, ColorContextLike}
import utopia.reach.component.factory.ContextInsertableComponentFactoryFactory.ContextualBuilderContentFactory

/**
  * A common trait for factory classes which produce container builders that produce containers with a specific
  * background color / fill. The containers are not expected to require any contextual information on their own.
  * @author Mikko Hilpinen
  * @since 9.12.2020, v0.1
  */
trait SimpleFilledBuilderFactory[+Builder[NC, +F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]]
{
	protected def makeBuilder[NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(background: ComponentColor, contentContext: NC, contentFactory: ContextualBuilderContentFactory[NC, F]): Builder[NC, F]
	
	/**
	  * Creates a new container builder that paints its background with specified color
	  * @param context Context to use for this area
	  * @param background Background color to fill this area with
	  * @param contentFactory A factory for producing content component factories
	  * @tparam NC Type of context used inside the created scroll area(s)
	  * @tparam F Type of component factories used
	  * @return A new container builder
	  */
	def buildFilledWithContext[NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(context: BackgroundSensitive[NC], background: ComponentColor,
	 contentFactory: ContextualBuilderContentFactory[NC, F]) =
		makeBuilder[NC, F](background, context.inContextWithBackground(background), contentFactory)
	
	/**
	  * Creates a new container builder that paints its background with specified color. Also uses a context mutator.
	  * @param context Context to use for this area
	  * @param background Background color to fill this area with
	  * @param contentFactory A factory for producing content component factories
	  * @param mapContext A function for mapping the component creation context
	  * @tparam NT Type of intermediate context accepted by the mapping function
	  * @tparam NC Type of context used inside the created scroll area(s)
	  * @tparam F Type of component factories used
	  * @return A new container builder
	  */
	def buildFilledWithMappedContext[NT, NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(context: BackgroundSensitive[NT], background: ComponentColor,
	 contentFactory: ContextualBuilderContentFactory[NC, F])(mapContext: NT => NC) =
		makeBuilder[NC, F](background, mapContext(context.inContextWithBackground(background)), contentFactory)
	
	/**
	  * Creates a new container builder that fills the container area with a background color
	  * @param context Container creation context
	  * @param role Role this container will have (color-wise)
	  * @param contentFactory Container content factory
	  * @param preferredShade Preferred coloring shade to use (default = standard)
	  * @tparam NC Type of content creation context
	  * @tparam F Type of contextual content factory
	  * @return A new container builder
	  */
	def buildFilledWithContextForRole[NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(context: BackgroundSensitive[NC] with ColorContextLike, role: ColorRole,
	 contentFactory: ContextualBuilderContentFactory[NC, F], preferredShade: ColorShade = Standard) =
		buildFilledWithContext[NC, F](context, context.color(role, preferredShade), contentFactory)
	
	/**
	  * Creates a new container builder that paints its background with specified color. Also uses a context mutator.
	  * @param context Context to use for this area
	  * @param role Role this container will have (color-wise)
	  * @param contentFactory A factory for producing content component factories
	  * @param mapContext A function for mapping the component creation context
	  * @param preferredShade Preferred coloring shade to use (default = standard)
	  * @tparam NT Type of intermediate context accepted by the mapping function
	  * @tparam NC Type of context used inside the created scroll area(s)
	  * @tparam F Type of component factories used
	  * @return A new container builder
	  */
	def buildFilledWithMappedContextForRole[NT, NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(context: BackgroundSensitive[NT] with ColorContextLike, role: ColorRole,
	 contentFactory: ContextualBuilderContentFactory[NC, F], preferredShade: ColorShade = Standard)
	(mapContext: NT => NC) =
		buildFilledWithMappedContext[NT, NC, F](context, context.color(role, preferredShade),
			contentFactory)(mapContext)
}
