package utopia.reach.container.wrapper

import utopia.firmament.component.container.single.FramingLike
import utopia.firmament.context.base.{BaseContextCopyable, BaseContextPropsView}
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory, RoundedBackgroundDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.RoundedBackgroundViewDrawer
import utopia.firmament.factory.VariableFramedFactory
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Large, Medium, Small, VeryLarge, VerySmall}
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.factory.contextual.ContextualFramedFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, ReachComponent}
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}

object Framing extends Cff[FramingFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = FramingFactory(hierarchy)
}

trait FramingFactoryLike[+Repr]
	extends WrapperContainerFactory[Framing, ReachComponent] with CustomDrawableFactory[Repr]
		with VariableFramedFactory[Repr]
{
	// IMPLEMENTED  --------------------
	
	override def apply[C <: ReachComponent, R](content: OpenComponent[C, R]): ComponentWrapResult[Framing, C, R] = {
		val framing = new Framing(hierarchy, content, insetsPointer, customDrawers)
		// Closes the content
		content.attachTo(framing)
	}
}

case class FramingFactory(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[BaseContextPropsView, ContextualFramingFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: BaseContextPropsView](context: N) =
		ContextualFramingFactory(parentHierarchy, context)
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param insets The insets to place around the content in this framing
	  * @return A new framing factory that uses the specified insets
	  */
	def apply(insets: StackInsetsConvertible) = InitializedFramingFactory(parentHierarchy, Fixed(insets.toInsets))
	/**
	  * @param insetsPointer A pointer that contains insets to place around the content in this framing
	  * @return A new framing factory that uses the specified insets pointer
	  */
	def apply(insetsPointer: Changing[StackInsets]) = InitializedFramingFactory(parentHierarchy, insetsPointer)
	
	/**
	  * Creates a new framing
	  * @param content A component that is yet to attach to a component hierarchy
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @tparam C Type of wrapped component
	  * @return A new framing and the produced content component
	  */
	@deprecated("Replaced with .apply(StackInsetsConvertible).apply(OpenComponent)", "v1.1")
	def apply[C <: ReachComponent, R](content: OpenComponent[C, R], insets: StackInsetsConvertible,
	                                  customDrawers: Seq[CustomDrawer] = Empty): ComponentWrapResult[Framing, C, R] =
		apply(insets).withCustomDrawers(customDrawers).apply(content)
}

case class InitializedFramingFactory(hierarchy: ComponentHierarchy, insetsPointer: Changing[StackInsets],
                                     customDrawers: Seq[CustomDrawer] = Empty)
	extends FramingFactoryLike[InitializedFramingFactory]
		with NonContextualWrapperContainerFactory[Framing, ReachComponent]
{
	override def withInsetsPointer(p: Changing[StackInsets]): InitializedFramingFactory = copy(insetsPointer = p)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): InitializedFramingFactory =
		copy(customDrawers = drawers)
}

object ContextualFramingFactory
{
	implicit class BackgroundSensitiveFramingFactory[NT <: BaseContextPropsView](val f: ContextualFramingFactory[_ <: BaseContextCopyable[_, NT]])
		extends AnyVal
	{
		/**
		  * Creates a new framing with background color and rounding at the four corners
		  * @param background The background color used
		  * @return A new framing and the produced content component
		  */
		def rounded(background: Color) = {
			// The rounding amount is based on insets
			val drawer = f.insetsPointer.fixedValue match {
				// Case: Using static insets => Uses a static drawer, also
				case Some(fixedInsets) =>
					fixedInsets.lengthsIterator.map { _.optimal }.filter { _ > 0.0 }.minOption match {
						case Some(minSideLength) => RoundedBackgroundDrawer.withRadius(background, minSideLength)
						// If the insets default to 0, uses solid background drawing instead
						case None => BackgroundDrawer(background)
					}
				// Case: Using variable insets => Uses a variable drawer
				case None =>
					val minSideLengthPointer = f.insetsPointer
						.map { _.lengthsIterator.map { _.optimal }.filter { _ > 0 }.minOption.getOrElse(0.0) }
					RoundedBackgroundViewDrawer.withRadius(View.fixed(background), minSideLengthPointer)
			}
			f.withCustomDrawer(drawer).mapContext { _.against(background) }
		}
	}
}

case class ContextualFramingFactory[N <: BaseContextPropsView](hierarchy: ComponentHierarchy, context: N,
                                                               customDrawers: Seq[CustomDrawer] = Empty,
                                                               customInsets: Either[Changing[SizeCategory], Changing[StackInsets]] = Left(Fixed(Medium)))
	extends FramingFactoryLike[ContextualFramingFactory[N]]
		with ContextualWrapperContainerFactory[N, BaseContextPropsView, Framing, ReachComponent, ContextualFramingFactory]
		with ContextualFramedFactory[ContextualFramingFactory[N]]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val insetsPointer: Changing[StackInsets] = customInsets.rightOrMap { sizePointer =>
		context.scaledStackMarginPointer(sizePointer).map { _.toInsets }
	}
	
	
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
	
	override def withContext[N2 <: BaseContextPropsView](newContext: N2) =
		copy(context = newContext)
	
	override def withInsetsPointer(p: Changing[StackInsets]): ContextualFramingFactory[N] =
		copy(customInsets = Right(p))
	
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualFramingFactory[N] =
		copy(customDrawers = drawers)
	
	override def withInsets(insetSize: SizeCategory) =
		copy(customInsets = Left(Fixed(insetSize)))
}

/**
  * A reach implementation of the framing trait which places insets or margins around a wrapped component
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class Framing(override val hierarchy: ComponentHierarchy, override val content: ReachComponent,
              insetsPointer: Changing[StackInsets], override val customDrawers: Seq[CustomDrawer] = Empty)
	extends ConcreteCustomDrawReachComponent with FramingLike[ReachComponent]
{
	// INITIAL CODE -----------------------------
	
	// Revalidates this component when applied insets change
	insetsPointer.addListenerWhile(linkedFlag) { _ => revalidate() }
	
	
	// IMPLEMENTED  -----------------------------
	
	override def insets: StackInsets = insetsPointer.value
}