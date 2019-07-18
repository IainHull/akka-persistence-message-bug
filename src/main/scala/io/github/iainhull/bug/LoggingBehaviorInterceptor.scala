package io.github.iainhull.bug

import akka.actor.typed.{Behavior, BehaviorInterceptor, Signal, TypedActorContext}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.Logger

object LoggingBehaviorInterceptor {
  def apply[T](logger: Logger)(behavior: Behavior[T]): Behavior[T] = {
    val interceptor = new LoggingBehaviorInterceptor[T](logger)
    Behaviors.intercept(interceptor)(behavior)
  }
}

final class LoggingBehaviorInterceptor[T] private(logger: Logger) extends BehaviorInterceptor[T, T] {

  import BehaviorInterceptor._

  override def aroundReceive(ctx: TypedActorContext[T], msg: T, target: ReceiveTarget[T]): Behavior[T] = {
    logger.debug(s"Intercepted message: $msg")
    val ret = target(ctx, msg)
    if (Behavior.isUnhandled(ret)) {
      logger.warn(s"Intercepted unhandled message: $msg")
    }
    ret
  }

  override def aroundSignal(ctx: TypedActorContext[T], signal: Signal, target: SignalTarget[T]): Behavior[T] = {
    logger.debug(s"Intercepted signal: $signal")
    target(ctx, signal)
  }

  override def toString: String = "LoggingBehaviorInterceptor"

  override def isSame(other: BehaviorInterceptor[Any, Any]): Boolean = {
    other match {
      case _: LoggingBehaviorInterceptor[_] => true
      case _ => super.isSame(other)
    }
  }
}

