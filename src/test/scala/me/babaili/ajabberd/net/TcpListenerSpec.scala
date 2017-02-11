package me.babaili.ajabberd.net

import java.net.Socket

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by chengyongqiao on 05/02/2017.
  */
class TcpListenerSpec extends TestKit(ActorSystem("TcpListenerSpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

    override def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }

    //
    "A tcp listener " must {
        //initialize listener and expect connections is 0
        val tcpListenerProps = Props(classOf[TcpListener], "localhost", 6000)
        val tcpListener = TestActorRef(tcpListenerProps)

        tcpListener ! TcpListener.QueryConnections

        expectMsg(0)

        "accept inbound connection and close wrong incoming stream" in {

            Thread.sleep(1000)
            //connect to listener and expect connections is 1
            val socket = new Socket("localhost", 6000)

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections

            expectMsg(1)

            //wrong xmpp stream cause the connection be closed and expect connections is 0
            socket.getOutputStream().write("hello".getBytes())

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections

            expectMsg(0)

            socket.shutdownInput()
            socket.shutdownOutput()
            socket.close()

            Thread.sleep(1000)
        }

        "accept start stream" in {
            val startStreamXml = "<?xml version=\"1.0\"?> \n" +
                "<stream:stream to=\"onepiece.com\" xmlns=\"jabber:client\" \n " +
                "xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"

            val socket = new Socket("localhost", 6000)

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections
            expectMsg(1)

            socket.getOutputStream().write(startStreamXml.getBytes())

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections
            expectMsg(1)


            val buffer = new Array[Byte](1024)
            val n = socket.getInputStream().read(buffer)
            val xmlTokenizer = new me.babaili.ajabberd.xml.XmlTokenizer()
            val data = new Array[Byte](n)
            Array.copy(buffer, 0, data, 0, n)
            val resultEither = xmlTokenizer.decode(data)
            resultEither.isLeft should equal(true)
            resultEither match {
                case Left(result) =>
                    result.length should equal(8)
                    result.head.isStartDocument() should equal(true)
                    val tail1 = result.tail
                    tail1.head.isStartElement() should equal(true)
                    tail1.head.asStartElement().getName().getLocalPart() should equal("stream")
                case Right(_) => assert(false)
            }


            socket.shutdownInput()
            socket.shutdownOutput()
            socket.close()

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections
            expectMsg(0)
        }
    }
}
