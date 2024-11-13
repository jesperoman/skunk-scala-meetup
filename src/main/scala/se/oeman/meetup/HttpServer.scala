package se.oeman.meetup

import cats.effect.*
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.Stream
import fs2.io.net.Network
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.circe.CirceEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.server.middleware.ErrorAction
import org.http4s.server.middleware.ErrorHandling
import org.http4s.syntax.all.*
import scala.concurrent.duration.*

object HttpServer extends CirceEntityEncoder with CirceEntityDecoder:
  def apply[F[_]: Async: Network](
    postgres: Postgres[F]
  ): Resource[F, Unit] =
    EmberServerBuilder
      .default[F]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withShutdownTimeout(15.seconds)
      .withHttpApp(
        ErrorHandling.Recover.total(
          ErrorAction.log(
            HttpServer.routes[F](postgres).orNotFound,
            messageFailureLogAction = (t, msg) => Async[F].delay(println(s"$msg, ${t.getMessage}")),
            serviceErrorLogAction = (t, msg) => Async[F].delay(println(s"$msg, ${t.getMessage}"))
          )
        )
      )
      .build
      .void

  private def routes[F[_]: Async](postgres: Postgres[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    val _ = postgres
    HttpRoutes.of:
      case _ => NotFound()

  given [F[_]: Async, A: Encoder]: EntityEncoder[F, Stream[F, A]] =
    new EntityEncoder[F, Stream[F, A]]:
      override def toEntity(a: Stream[F, A]): Entity[F] = Entity(
        body = (Stream.emit("[") ++ a.map(_.asJson.noSpaces).intersperse(",") ++ Stream.emit("]"))
          .through(fs2.text.utf8.encode),
        length = None
      )
      override def headers: Headers = Headers(`Content-Type`(MediaType.application.json))
