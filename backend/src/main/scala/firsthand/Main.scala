package firsthand

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main extends App with LazyLogging {

  private val config = ConfigFactory.load()

  val system: ActorSystem[Nothing] = ActorSystem[Nothing](
    Behaviors.empty,
    "firsthand-system",
    config
  )
  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
  implicit val ec: ExecutionContext = system.executionContext

  // Initialize Firebase if credentials are available
  FirebaseInitializer.initialize(config)

  // Define routes
  val routes: Route =
    path("health") {
      get {
        complete("OK")
      }
    } ~
    pathPrefix("api" / "v1") {
      concat(
        // TODO: Add API routes here
        path("status") {
          get {
            complete("""{"status": "running", "service": "FirstHand API"}""")
          }
        }
      )
    }

  // Start HTTP server
  val interface = config.getString("firsthand.http.interface")
  val port = config.getInt("firsthand.http.port")

  val serverBinding = Http()
    .newServerAt(interface, port)
    .bind(routes)

  serverBinding.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info(s"FirstHand API server online at http://${address.getHostString}:${address.getPort}/")
    case Failure(ex) =>
      logger.error(s"Failed to bind HTTP server: ${ex.getMessage}", ex)
      system.terminate()
  }
}
