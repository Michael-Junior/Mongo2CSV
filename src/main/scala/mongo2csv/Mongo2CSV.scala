package mongo2csv

import org.apache.commons.csv.{CSVFormat, CSVPrinter}
import org.mongodb.scala.{Document, bson}

import java.io.BufferedWriter
import java.nio.file.{Files, Paths}
import java.util.Calendar
import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

case class M2CSV_Parameters(csvDir: String,
                            database: String,
                            collection: String,
                            host: Option[String],
                            port: Option[Int],
                            user: Option[String],
                            password: Option[String])

class Mongo2CSV {

  def exportCsv(parameters: M2CSV_Parameters): Try[Unit] = {
    Try {
      val mExport: MongoExport = new MongoExport(parameters.database, parameters.collection, parameters.host, parameters.port)
      val docsMongo: Seq[Document] = mExport.findAll
      createCSVFile(parameters.csvDir, docsMongo) match {
        case Success(fileCsvOut) => println(s"\nFILE GENERATED SUCCESSFULLY IN: $fileCsvOut")
        case Failure(e) => println(s"\nFAILURE TO GENERATE FILE: $e")
      }
    }
  }

  def createCSVFile(path_out: String, listDocsMongo: Seq[Document]): Try[String] = {
    Try {
      val headers: Iterable[String] = getHeaders(listDocsMongo)
      val listDocsValue: Iterable[Iterable[String]] = listDocsMongo.groupBy(identity).map(f => f._1.map(f => bsonValueToString(f._2)))

      val fileCsvOut: String = if (!path_out.endsWith(".csv")) path_out.concat(s"${LocalDateTime.now}.csv") else path_out
      val pathFile: BufferedWriter = Files.newBufferedWriter(Paths.get(fileCsvOut))
      val csvFileFormat: CSVFormat.Builder = if (headers.nonEmpty) CSVFormat.EXCEL.builder().setHeader(headers.toSeq.distinct: _*) else
        CSVFormat.EXCEL.builder().setHeader("No documents found! -> Check the parameters")
      val csvFilePrinter: CSVPrinter = new CSVPrinter(pathFile, csvFileFormat.build())

      listDocsValue.foreach(f => csvFilePrinter.printRecord(f.toArray: _*))
      csvFilePrinter.flush()
      csvFilePrinter.close()

      fileCsvOut
    }
  }

  def getHeaders(listDocsMongo: Seq[Document]): Iterable[String] = {
    val listHeader: Seq[Iterable[String]] = listDocsMongo.map(f => f.map(f => f._1))
    val headers: Iterable[String] = for {as <- listHeader
                                         r <- as} yield r
    headers
  }

  def bsonValueToString(value: bson.BsonValue): String = {
    value.getBsonType.getValue match {
      case 1 ⇒ value.asDouble().getValue.toString
      case 2 ⇒ value.asString().getValue
      case 3 ⇒ value.asDocument().toString
      case 4 ⇒ value.asArray().toString
      case 5 ⇒ value.asBinary().toString
      case 7 ⇒ value.asObjectId().getValue.toString
      case 8 ⇒ value.asBoolean().toString
      case 9 ⇒ value.asDateTime().toString
      case _ ⇒ ""
    }
  }
}

object Mongo2CSV {
  private def usage(): Unit = {
    System.err.println("-database=<name>   - MongoDB database name")
    System.err.println("-collection=<name> - MongoDB database collection name")
    System.err.println("-csvDir=<path>     - CSV file output directory")
    System.err.println("[-host=<name>]     - MongoDB server name. Default value is 'localhost'")
    System.err.println("[-port=<number>]   - MongoDB server port number. Default value is 27017")
    System.err.println("[-user=<name>])    - MongoDB user name")
    System.err.println("[-password=<pwd>]  - MongoDB user password")
    System.exit(1)
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 3) usage()

    val parameters: Map[String, String] = args.foldLeft[Map[String, String]](Map()) {
      case (map, par) =>
        val split = par.split(" *= *", 2)
        if (split.size == 1) map + ((split(0).substring(2), ""))
        else map + (split(0).substring(1) -> split(1))
    }

    if (!Set("csvDir", "database", "collection").forall(parameters.contains)) usage()

    val csvDir: String = parameters("csvDir")
    val database: String = parameters("database")
    val collection: String = parameters("collection")

    val host: Option[String] = parameters.get("host")
    val port: Option[Int] = parameters.get("port").flatMap(_.toIntOption)
    val user: Option[String] = parameters.get("user")
    val password: Option[String] = parameters.get("password")

    val params: M2CSV_Parameters = M2CSV_Parameters(csvDir, database, collection, host, port, user, password)
    val time1: Long = Calendar.getInstance().getTimeInMillis

    (new Mongo2CSV).exportCsv(params) match {
      case Success(_) =>
        println("Successful!")
        val time2: Long = Calendar.getInstance().getTime.getTime
        println(s"Diff time=${time2 - time1}ms\n")
        System.exit(0)
      case Failure(exception) =>
        println(s"Error: ${exception.toString}\n")
        System.exit(1)
    }
  }
}