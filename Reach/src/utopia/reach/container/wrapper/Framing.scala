package utopia.reach.container.wrapper

import utopia.firmament.component.container.single.FramingLike
import utopia.firmament.context.{BaseContext, BaseContextLike}
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory, RoundedBackgroundDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.factory.FramedFactory
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Large, Medium, Small, VeryLarge, VerySmall}
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.EitherExtensions._
import utopia.paradigm.color.Color
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.factory.contextual.ContextualFramedFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}

object Framing extends Cff[FramingFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = FramingFactory(hierarchy)
}

trait FramingFactoryLike[+Repr]
	extends WrapperContainerFactory[Framing, ReachComponentLike] with CustomDrawableFactory[Repr]
		with FramedFactory[Repr]
{
	// IMPLEMENTED  --------------------
	
	override def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R]): ComponentWrapResult[Framing, C, R] = {
		val framing = new Framing(parentHierarchy, content, insets, customDrawers)
		// Closes the content
		content.attachTo(framing)
	}
}

case class FramingFactory(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[BaseContext, ContextualFramingFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: BaseContext](context: N) =
		ContextualFramingFactory(parentHierarchy, context)
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param insets The insets to place around the content in this framing
	  * @return A new framing factory that uses the specified insets
	  */
	def apply(insets: StackInsetsConvertible) = InitializedFramingFactory(parentHierarchy, insets.toInsets)
	
	/**
	  * Creates a new framing
	  * @param content A component that is yet to attach to a component hierarchy
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @tparam C Type of wrapped component
	  * @return A new framing and the produced content component
	  */
	@deprecated("Replaced with .apply(StackInsetsConvertible).apply(OpenComponent)", "v1.1")
	def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R], insets: StackInsetsConvertible,
										  customDrawers: Seq[CustomDrawer] = Empty): ComponentWrapResult[Framing, C, R] =
		apply(insets).withCustomDrawers(customDrawers).apply(content)
}

case class InitializedFramingFactory(parentHierarchy: ComponentHierarchy, insets: StackInsets,
                                     customDrawers: Seq[CustomDrawer] = Empty)
	extends FramingFactoryLike[InitializedFramingFactory]
		with NonContextualWrapperContainerFactory[Framing, ReachComponentLike]
{
	override def withInsets(insets: StackInsetsConvertible): InitializedFramingFactory = copy(insets = insets.toInsets)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): InitializedFramingFactory =
		copy(customDrawers = drawers)
}

object ContextualFramingFactory
{
	implicit class BackgroundSensitiveFramingFactory[NT <: BaseContext](val f: ContextualFramingFactory[_ <: BaseContextLike[_, NT]])
		extends AnyVal
	{
		/**
		  * Creates a new framing with background color and rounding at the four corners
		  * @param background The background color used
		  * @return A new framing and the produced content component
		  */
		def rounded(background: Color) = {
			// The rounding amount is based on insets
			val drawer = f.insets.lengthsIterator.map { _.optimal }.filter { _ > 0.0 }.minOption match {
				case Some(minSideLength) => RoundedBackgroundDrawer.withRadius(background, minSideLength)
				// If the insets default to 0, uses solid background drawing instead
				case None => BackgroundDrawer(background)
			}
			f.withCustomDrawer(drawer).mapContext { _.against(background) }
		}
	}
}

case class ContextualFramingFactory[N <: BaseContext](parentHierarchy: ComponentHierarchy, context: N,
                                                      customDrawers: Seq[CustomDrawer] = Empty,
                                                      customInsets: Either[SizeCategory, StackInsets] = Left(Medium))
	extends FramingFactoryLike[ContextualFramingFactory[N]]
		with ContextualWrapperContainerFactory[N, BaseContext, Framing, ReachComponentLike, ContextualFramingFactory]
		with ContextualFramedFactory[ContextualFramingFactory[N]]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return Copy of this factory that places very small insets
	  */
	def verySmall = withInsets(VerySmall)
	/**
	  * @return Copy of this factory that places small insets
	  */
	def small = withInsets(Small)
	/**
	  * @return Copy of this factory that places large insets
	  */
	def large = withInsets(Large)
	/**
	  * @return Copy of this factory that places very large insets
	  */
	def veryLarge = withInsets(VeryLarge)
	
	
	// IMPLEMENTED	------------------------
	
	override def withContext[N2 <: BaseContext](newContext: N2) = copy(context = newContext)
	
	override def insets: StackInsets = customInsets.rightOrMap(s => StackInsets.symmetric(context.scaledStackMargin(s)))
	
	override def withInsets(insets: StackInsetsConvertible): ContextualFramingFactory[N] =
		copy(customInsets = Right(insets.toInsets))
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualFramingFactory[N] =
		copy(customDrawers = drawers)
	
	override def withInsets(insetSize: SizeCategory) = copy(customInsets = Left(insetSize))
}

/**
  * A reach implementation of the framing trait which places insets or margins around a wrapped component
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class Framing(override val parentHierarchy: ComponentHierarchy, override val content: ReachComponentLike,
			  override val insets: StackInsets, override val customDrawers: Seq[CustomDrawer] = Empty)
	extends CustomDrawReachComponent with FramingLike[ReachComponentLike]