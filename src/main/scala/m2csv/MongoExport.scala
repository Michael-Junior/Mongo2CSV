package m2csv

import org.mongodb.scala._
import org.mongodb.scala.bson.collection.mutable.Document

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration


class MongoExport(database: String,
                  collection: String,
                  host: Option[String] = None,
                  port: Option[Int] = None,
                  user: Option[String] = None,
                  password: Option[String] = None) {
  require((user.isEmpty && password.isEmpty) || (user.nonEmpty && password.nonEmpty))

  private val hostStr: String = host.getOrElse("localhost")
  private val portStr: String = port.getOrElse(27017).toString
  private val usrPswStr: String = user match {
    case Some(usr)
    => s"$usr:${password.get}@"
    case None => ""
  }

  private val mongoUri: String = s"mongodb://$usrPswStr$hostStr:$portStr"
  private val mongoClient: MongoClient = MongoClient(mongoUri)
  private val dbase: MongoDatabase = mongoClient.getDatabase(database)
  private val coll: MongoCollection[Document] = dbase.getCollection(collection)

  val documentsMongo: Seq[Document] = new DocumentObservable[String](coll.find()).observable.results()

  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: Document => String = doc => doc.toJson()
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => Option(doc).map(_.toString).getOrElse("")
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]
    val converter: (C) => String

    def results(): Seq[C] = Await.result(observable.toFuture(), Duration(120, TimeUnit.SECONDS))

    def headResult() = Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))

    def printResults(initial: String = ""): Unit = {
      if (initial.nonEmpty) print(initial)
      results().foreach(res => println(converter(res)))
    }

    def printHeadResult(initial: String = ""): Unit = println(s"${initial}${converter(headResult())}")
  }
}