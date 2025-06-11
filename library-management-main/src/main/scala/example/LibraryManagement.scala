package example

import cats.effect._
import com.typesafe.scalalogging.Logger
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.circe.syntax._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.bson.{BsonObjectId, BsonDateTime, BsonNull}
import pureconfig._
import pureconfig.generic.auto._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{Logger => Http4sLogger}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.SSLKeyStoreSupport.StoreInfo
import com.comcast.ip4s.SocketAddress

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn.readLine

case class Config(mongodb: MongoConfig, server: ServerConfig)
case class MongoConfig(uri: String, database: String)
case class ServerConfig(host: String, port: Int)

case class Book(title: String, author: String, copies: Int, availableCopies: Int)
case class User(name: String, email: String)
case class Loan(bookId: String, userId: String, loanDate: Long, returnDate: Option[Long])

object LibraryManagement extends IOApp {
  private val logger = Logger(getClass)
  private val dsl = new Http4sDsl[IO] {}
  import dsl._
  
  // Helper to block and get result from Future
  def awaitResult[T](future: scala.concurrent.Future[T]): T = {
    Await.result(future, Duration.Inf)
  }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      _ <- IO(logger.info("Starting Library Management System..."))
      config <- IO(ConfigSource.default.loadOrThrow[Config])
      _ <- IO(logger.info("Configuration loaded successfully"))
      
      _ <- IO(logger.info("Connecting to MongoDB..."))
      mongoClient = MongoClient(config.mongodb.uri)
      database = mongoClient.getDatabase(config.mongodb.database)
      booksCollection = database.getCollection("books")
      usersCollection = database.getCollection("users")
      loansCollection = database.getCollection("loans")
      _ <- IO(logger.info("MongoDB connection established"))
      
      _ <- IO(logger.info("Creating HTTP service..."))
      httpApp = createHttpApp(booksCollection, usersCollection, loansCollection)
      
      host = Host.fromString(config.server.host).getOrElse(Host.fromString("0.0.0.0").get)
      port = Port.fromInt(config.server.port).getOrElse(Port.fromInt(8080).get)
      _ <- IO(logger.info(s"Starting server on ${host}:${port}"))
      
      server <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(Http4sLogger.httpApp(true, true)(httpApp))
        .withMaxConnections(100)
        .build
        .use { server =>
          IO(logger.info(s"Server started successfully at http://${host}:${port}")) >>
          IO.never
        }
        .as(ExitCode.Success)
    } yield server
  }

  def createHttpApp(
    booksCollection: MongoCollection[Document],
    usersCollection: MongoCollection[Document],
    loansCollection: MongoCollection[Document]
  ): HttpApp[IO] = {
    HttpRoutes.of[IO] {
      // Book endpoints
      case GET -> Root / "books" =>
        val booksFuture = booksCollection.find().toFuture()
        val books = awaitResult(booksFuture).map { doc =>
          Book(
            doc.getString("title"),
            doc.getString("author"),
            doc.getInteger("copies"),
            doc.getInteger("availableCopies")
          )
        }
        Ok(books.asJson)

      case req @ POST -> Root / "books" =>
        for {
          book <- req.as[Book]
          _ <- IO {
            val bookDoc = Document(
              "title" -> book.title,
              "author" -> book.author,
              "copies" -> book.copies,
              "availableCopies" -> book.copies
            )
            awaitResult(booksCollection.insertOne(bookDoc).toFuture())
          }
          resp <- Ok("Book added successfully")
        } yield resp

      // User endpoints
      case req @ POST -> Root / "users" =>
        for {
          user <- req.as[User]
          _ <- IO {
            val userDoc = Document(
              "name" -> user.name,
              "email" -> user.email
            )
            awaitResult(usersCollection.insertOne(userDoc).toFuture())
          }
          resp <- Ok("User added successfully")
        } yield resp

      // Loan endpoints
      case req @ POST -> Root / "loans" =>
        for {
          loan <- req.as[Loan]
          success <- IO {
            val bookOpt = awaitResult(booksCollection.find(equal("title", loan.bookId)).first().toFutureOption())
            val userOpt = awaitResult(usersCollection.find(equal("email", loan.userId)).first().toFutureOption())

            (bookOpt, userOpt) match {
              case (Some(book), Some(user)) =>
                val availableCopies = book.getInteger("availableCopies", 0)
                if (availableCopies > 0) {
                  val bookId = book.getObjectId("_id")
                  val userId = user.getObjectId("_id")

                  awaitResult(booksCollection.updateOne(equal("_id", bookId), inc("availableCopies", -1)).toFuture())

                  val loanDoc = Document(
                    "bookId" -> BsonObjectId(bookId),
                    "userId" -> BsonObjectId(userId),
                    "loanDate" -> BsonDateTime(System.currentTimeMillis()),
                    "returnDate" -> BsonNull()
                  )
                  awaitResult(loansCollection.insertOne(loanDoc).toFuture())
                  true
                } else false
              case _ => false
            }
          }
          resp <- if (success) Ok("Book loaned successfully") else BadRequest("Failed to loan book")
        } yield resp

      // Return book endpoint
      case req @ POST -> Root / "returns" =>
        for {
          loan <- req.as[Loan]
          success <- IO {
            val bookOpt = awaitResult(booksCollection.find(equal("title", loan.bookId)).first().toFutureOption())
            val userOpt = awaitResult(usersCollection.find(equal("email", loan.userId)).first().toFutureOption())

            (bookOpt, userOpt) match {
              case (Some(book), Some(user)) =>
                val bookId = book.getObjectId("_id")
                val userId = user.getObjectId("_id")

                val loanOpt = awaitResult(loansCollection.find(
                  and(
                    equal("bookId", bookId),
                    equal("userId", userId),
                    equal("returnDate", BsonNull())
                  )
                ).first().toFutureOption())

                loanOpt match {
                  case Some(loan) =>
                    awaitResult(loansCollection.updateOne(
                      equal("_id", loan.getObjectId("_id")),
                      set("returnDate", BsonDateTime(System.currentTimeMillis()))
                    ).toFuture())
                    awaitResult(booksCollection.updateOne(equal("_id", bookId), inc("availableCopies", 1)).toFuture())
                    true
                  case None => false
                }
              case _ => false
            }
          }
          resp <- if (success) Ok("Book returned successfully") else BadRequest("Failed to return book")
        } yield resp

      // Serve static files
      case GET -> Root =>
        StaticFile.fromResource[IO]("/index.html").getOrElseF(NotFound())
    }.orNotFound
  }
}

