package com.karasiq.proxychecker.test

import com.karasiq.proxychecker.store.ProxyStoreJsonProtocol._
import com.karasiq.proxychecker.store.{ProxyStoreEntry, ProxyStoreJsonProtocol}
import com.karasiq.proxychecker.test.providers.TestServicesProvider
import com.karasiq.proxychecker.webservice.ProxyCheckerWebServiceProvider
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import spray.http.HttpEncodings._
import spray.http.HttpHeaders.`Accept-Encoding`
import spray.http.{HttpEntity, StatusCodes, Uri}
import spray.json._
import spray.testkit.ScalatestRouteTest

class WebServiceTest extends FlatSpec with ScalatestRouteTest with Matchers with TestServicesProvider with ProxyCheckerWebServiceProvider with BeforeAndAfterAll {
  private val service = new ProxyCheckerHttpService

  private val route = service.route

  private val testProxy = "127.0.0.1:1080"

  "Http service" should "add proxy" in {
    Post(Uri("/proxylist"), HttpEntity(testProxy)) ~> route ~> check {
      status should be (StatusCodes.OK)
    }
  }

  it should "get proxy json" in {
    Get(Uri("/proxylist.json")) ~> `Accept-Encoding`(identity) ~> route ~> check {
      val response = responseAs[String].parseJson.convertTo[Vector[ProxyStoreEntry]]
      println(response)
      response.map(_.address) should contain(testProxy)
    }
  }

  it should "get proxy list" in {
    Get(Uri("/proxylist.txt")) ~> `Accept-Encoding`(identity) ~> route ~> check {
      val response = responseAs[String]
      println(response)
      response.lines.toSeq should contain(testProxy)
    }
  }

  it should "delete proxy by country" in {
    Delete(Uri("/proxy").withQuery("country" → "ABC")) ~> route ~> check {
      status should be (StatusCodes.OK)
    }
  }

  it should "delete proxy" in {
    Delete(Uri("/proxy").withQuery("address" → testProxy)) ~> route ~> check {
      status should be (StatusCodes.OK)
    }
  }

  override protected def afterAll(): Unit = {
    actorSystem.shutdown()
  }
}
