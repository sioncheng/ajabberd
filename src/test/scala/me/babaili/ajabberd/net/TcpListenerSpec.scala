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
        "accept inbound connection " in {
            //initialize listener and expect connections is 0
            val tcpListenerProps = Props(classOf[TcpListener], "localhost", 6000)
            val tcpListener = TestActorRef(tcpListenerProps)

            tcpListener ! TcpListener.QueryConnections

            expectMsg(0)

            Thread.sleep(1000)
            //connect to listener and expect connections is 1
            val socket = new Socket("localhost", 6000)

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections

            expectMsg(1)

            //close the connection and expect connections is 0
            socket.getOutputStream().write("hello".getBytes())

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections

            expectMsg(0)

            Thread.sleep(1000)

            socket.shutdownInput()
            socket.shutdownOutput()
            socket.close()
        }
    }
}
