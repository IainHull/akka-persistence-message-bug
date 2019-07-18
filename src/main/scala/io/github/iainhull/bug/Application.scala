package io.github.iainhull.bug

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.typed.{ActorSystem, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.StrictLogging

object Application extends App with StrictLogging {
  val app = ActorSystem[Nothing](
    LoggingBehaviorInterceptor[Nothing](logger) {
      Behaviors.setup[Nothing] { context =>
        val persistentActor = context.spawn(PersistentActor(), "persistence")
        val testActor = context.spawn(TestActor(persistentActor), "test")
        context.watch(testActor)
        Behaviors.receiveSignal[Nothing] {
          case (_, Terminated(_)) => Behaviors.stopped
        }
      }
    }, "app")

  Await.result(app.whenTerminated, Duration.Inf)
}
