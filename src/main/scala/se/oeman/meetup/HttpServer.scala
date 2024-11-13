package se.oeman.meetup

import cats.effect.*
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.Stream
import fs2.io.net.Network
import io.circe.Decoder
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
    postgres: Resource[F, Postgres[F]]
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

  private def routes[F[_]: Async](postgres: Resource[F, Postgres[F]]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of:
      case req @ POST -> Root / "todos" =>
        for
          name <- req.as[String]
          id <- postgres.use(_.add(name))
          response <- Ok(id)
        yield response

      case GET -> Root / "todos" =>
        postgres.use(pg => Ok(pg.list))

      case GET -> Root / "todos" / IntVar(id) =>
        postgres.use(_.get(id)).flatMap {
          case Some(todo) => Ok(todo)
          case None => NotFound(s"No todo with id $id found")
        }

      case req @ PUT -> Root / "todos" / IntVar(id) =>
        for
          todo <- req.as[Todo]
          result <- postgres.use(_.update(id, todo))
          response <- if (result) postgres.use(_.get(id).flatMap(Ok(_))) else NotFound()
        yield response

      case DELETE -> Root / "todos" / IntVar(id) =>
        postgres.use(_.delete(id)).flatMap(result => if (result) Ok() else NotFound())

  given [F[_]: Async, A: Encoder]: EntityEncoder[F, Stream[F, A]] =
    new EntityEncoder[F, Stream[F, A]]:
      override def toEntity(a: Stream[F, A]): Entity[F] = Entity(
        body = (Stream.emit("[") ++ a.map(_.asJson.noSpaces).intersperse(",") ++ Stream.emit("]"))
          .through(fs2.text.utf8.encode),
        length = None
      )
      override def headers: Headers = Headers(`Content-Type`(MediaType.application.json))
