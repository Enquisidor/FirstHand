package firsthand

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import java.io.FileInputStream
import scala.util.{Failure, Success, Try}

object FirebaseInitializer extends LazyLogging {

  def initialize(config: Config): Unit = {
    if (config.hasPath("firsthand.firebase.credentials-path")) {
      val credentialsPath = config.getString("firsthand.firebase.credentials-path")
      val projectId = config.getString("firsthand.firebase.project-id")

      Try {
        val serviceAccount = new FileInputStream(credentialsPath)

        val options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
          .setProjectId(projectId)
          .build()

        FirebaseApp.initializeApp(options)
      } match {
        case Success(_) =>
          logger.info("Firebase initialized successfully")
        case Failure(ex) =>
          logger.warn(s"Firebase initialization failed: ${ex.getMessage}. Running without Firebase.")
      }
    } else {
      logger.info("Firebase credentials not configured. Running without Firebase.")
    }
  }
}
