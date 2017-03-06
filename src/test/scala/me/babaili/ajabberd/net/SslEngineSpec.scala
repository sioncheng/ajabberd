package me.babaili.ajabberd.net

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest._

/**
  * Created by chengyongqiao on 26/02/2017.
  */
class SslEngineSpec extends TestKit(ActorSystem("TcpListenerSpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

    override def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }

    "An ssl engine" must {
        //val props = Props(classOf[SslEngine])
        //val sslEngine = TestActorRef(props)

        "be able to to handle shake" in {
            import akka.io.Tcp._
            import akka.util.ByteString
            //sslEngine ! Received(ByteString.fromString("skfjlfj"))
        }


        Thread.sleep(1000)
    }


}
