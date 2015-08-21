import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.{Route, Directive0}
import akka.http.scaladsl.server.Directives._

import com.typesafe.config.ConfigFactory

/**
 * CORS support for akka-http
 *
 * see : https://groups.google.com/forum/#!topic/akka-user/5RCZIJt7jHo
 *
 * User: terry
 * Date: 8/21/15
 * Time: 9:33 AM
 *
 */
trait Cors {
  lazy val allowedOrigin = {
    val config = ConfigFactory.load()
    val allowedOrigin = config.getString("cors.allowed-origin")
    HttpOrigin(allowedOrigin)
  }

  /**
   * Main Directive to use in your route.  Adds the AccessControlHeader with the route for the preflight OPTIONS check
   *
   * @return
   */
  def cors = {
    r : Route =>
      accessControlHeaders {
        preFlightRequestHandler ~ r
      }
  }

  private def accessControlHeaders : Directive0 = {
    mapResponseHeaders {  // mapResponseHeaders is an existing akka-http Directive
      headers =>
        `Access-Control-Allow-Origin`(allowedOrigin) +:
        `Access-Control-Allow-Credentials`(true) +:
        `Access-Control-Allow-Headers`(
          "Accept", "Authorization", "Content-Type", "Origin", "X-Requested-With"
        ) +:
        headers
    }
  }

  private def preFlightRequestHandler : Route = options {
    complete {
      HttpResponse(200).withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE))
    }
  }

}
