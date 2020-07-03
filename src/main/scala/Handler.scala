import scalajs.js
import js.annotation.JSExportTopLevel
import net.exoego.facade.aws_lambda._

import scala.concurrent.{ExecutionContext, Future}
import facade.amazonaws.services.dynamodb._
import facade.amazonaws.services.rekognition._

import scala.scalajs.js.Dictionary
import js.JSConverters._

case class FaceData(gender: String, ageLow: Int, ageHigh: Int)

object Handler {
  val tblName = "cupper-tbl" // Change your table name

  def main(event: S3Event)(implicit ec: ExecutionContext): Future[Unit] = {
    implicit def toFaceData(res: DetectFacesResponse): List[FaceData] =
      res.FaceDetails.get.foldLeft(List[FaceData]()) {(list, detail) =>
        FaceData(
          detail.Gender.get.Value.toString,
          detail.AgeRange.get.Low.toString.toInt,
          detail.AgeRange.get.High.toString.toInt) :: list
      }

    val r = event.Records.pop()
    val bucketName = r.s3.bucket.name
    val key = r.s3.`object`.key
    for {
      res <- detectImage(bucketName, key)
      putItem <- putData(bucketName, key, res)
    } yield {
      for (detail <- res.FaceDetails.get) {
        val gender = detail.Gender.get.Value
        val ageRange = detail.AgeRange.get

        println("==============")
        println(gender.toString)
        println(ageRange.High)
        println(ageRange.Low)
        println("==============")
      }
    }
  }

  def putData(bucketName: String, key: String, faceDataList: List[FaceData])(implicit ec: ExecutionContext): Future[BatchWriteItemOutput] = {
    val items = faceDataList.foldLeft(List[WriteRequest]())((list, faceData) => {
      WriteRequest(
        PutRequest = PutRequest(
          Item = Dictionary(
            "name" -> AttributeValue.S(s"${bucketName}:${key}"),
            "ageLow" -> AttributeValue.NFromInt(faceData.ageLow),
            "ageHigh" -> AttributeValue.NFromInt(faceData.ageHigh),
            "gender" -> AttributeValue.S(faceData.gender)
          )
        )
      ) :: list
    })

    new DynamoDB().batchWriteItemFuture(BatchWriteItemInput(
      RequestItems = Dictionary(tblName -> items.toJSArray)
    ))
  }

  def detectImage(bucketName: String, key: String)(implicit ec: ExecutionContext): Future[DetectFacesResponse] = {
    new Rekognition().detectFacesFuture(
      DetectFacesRequest(
        Image = Image(
          S3Object = S3Object(
            Bucket = bucketName,
            Name = key
          )
        ),
        Attributes = List(Attribute.ALL).toJSArray
      )
    )
  }

  @JSExportTopLevel(name="handler")
  val handler: js.Function2[S3Event, Context, js.Promise[Unit]] = {
    implicit val ec = ExecutionContext.global

    (event, context) => main(event).toJSPromise
  }
}
