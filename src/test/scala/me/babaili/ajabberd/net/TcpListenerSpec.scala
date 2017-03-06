package me.babaili.ajabberd.net

import java.net.Socket
import javax.net.ssl._
import java.security.KeyStore

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

        "accept start stream, start tls" in {
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


            val xmlTokenizer = new me.babaili.ajabberd.xml.XmlTokenizer()

            val data = readData(socket)
            val resultEither = xmlTokenizer.decode(data)
            resultEither.isLeft should equal(true)
            resultEither match {
                case Left(result) =>
                    result.length should equal(20)
                    result.head.isStartDocument() should equal(true)
                    val tail1 = result.tail
                    tail1.head.isStartElement() should equal(true)
                    tail1.head.asStartElement().getName().getLocalPart() should equal("stream")
                case Right(_) => assert(false)
            }

            val startTls = "<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>"
            socket.getOutputStream().write(startTls.getBytes())

            val data2 = readData(socket)
            val resultEither2 = xmlTokenizer.decode(data2)
            resultEither2 match {
                case Left(result) =>
                    //
                    result.length should equal(2)
                    result.head.isStartElement should equal(true)
                    result.head.asStartElement().getName().getLocalPart() should equal("proceed")
                case Right(e) =>
                    e.printStackTrace()
                    assert(false)
            }

            //tls hand shake
            System.setProperty("javax.net.debug","ssl")
            val clientKeyPassword = "123456".toCharArray()
            val clientKeyStore = KeyStore.getInstance("jks")
            clientKeyStore.load(getClass().getResourceAsStream("/client.keystore"), clientKeyPassword)

            val trustKeyStore = KeyStore.getInstance("jks")
            trustKeyStore.load(getClass().getResourceAsStream("/ca-trust.keystore"), clientKeyPassword)

            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(clientKeyStore, clientKeyPassword)

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(trustKeyStore)

            val sslContext = SSLContext.getInstance("TLSv1")
            //System.out.println("enabled protocols" + sslContext.getProtocol())
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null)
            val sslSocket = sslContext.getSocketFactory().createSocket(socket, "localhost", 6000, true)
            //

            sslSocket.getOutputStream().write("what are doing now?".getBytes())
            sslSocket.getOutputStream().flush()

            Thread.sleep(1000)
            sslSocket.close()
            System.out.println("ssl socket close")

            socket.shutdownInput()
            socket.shutdownOutput()
            socket.close()

            System.out.println("ssl socket")

            Thread.sleep(1000)
            tcpListener ! TcpListener.QueryConnections
            expectMsg(0)
        }
    }


    def readData(socket: Socket): Array[Byte] = {
        val buffer = new Array[Byte](1024)
        val n = socket.getInputStream().read(buffer)
        val data = new Array[Byte](n)
        Array.copy(buffer, 0, data, 0, n)
        return data
    }
}
