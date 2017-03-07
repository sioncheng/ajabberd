package me.babaili.ajabberd.app

import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext, SSLSocket, TrustManagerFactory}

import me.babaili.ajabberd.net.TcpListener

/**
  * Created by cyq on 06/03/2017.
  */
object SimpleClientTestApp extends App {

    def readData(socket: Socket): Array[Byte] = {
        val buffer = new Array[Byte](1024)
        val n = socket.getInputStream().read(buffer)
        val data = new Array[Byte](n)
        Array.copy(buffer, 0, data, 0, n)
        return data
    }

    println("simple client test app")

    System.setProperty("javax.net.debug", "all")

    /*
    val startStreamXml = "<?xml version=\"1.0\"?> \n" +
        "<stream:stream to=\"onepiece.com\" xmlns=\"jabber:client\" \n " +
        "xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"

    val socket = new Socket("localhost", 6666)

    socket.getOutputStream().write(startStreamXml.getBytes())


    val xmlTokenizer = new me.babaili.ajabberd.xml.XmlTokenizer()

    val data = readData(socket)
    val resultEither = xmlTokenizer.decode(data)
    resultEither match {
        case Left(result) =>
            //
            println(s"has result 1 ${result}")
        case Right(_) => assert(false)
    }

    val startTls = "<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>"
    socket.getOutputStream().write(startTls.getBytes())

    val data2 = readData(socket)
    val resultEither2 = xmlTokenizer.decode(data2)
    resultEither2 match {
        case Left(result) =>
            //
            println(s"has result 2 ${result}")
        case Right(e) =>
            e.printStackTrace()
            assert(false)
    }


    val data3 = readData(socket)
    val resultEither3 = xmlTokenizer.decode(data3)
    resultEither3 match {
        case Left(result) =>
            //
            println(s"has result 3 ${result}")
        case Right(e) =>
            e.printStackTrace()
            assert(false)
    }
    */

    //tls hand shake
    val password = "123456".toCharArray
    val keyStore = KeyStore.getInstance("JKS")
    val inServer = getClass().getResourceAsStream("/netty/client/cChat.jks")
    keyStore.load(inServer, password)

    val trustKeyStore = KeyStore.getInstance("JKS")
    val inCaTrust = getClass().getResourceAsStream("/netty/client/cChat.jks")
    trustKeyStore.load(inCaTrust, password)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, password)
    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(trustKeyStore)

    val sslContext = SSLContext.getInstance("TLSv1.2")
    //System.out.println("enabled protocols" + sslContext.getProtocol())
    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null)

    val sslSocket:SSLSocket = (sslContext.getSocketFactory().createSocket( "localhost", 6666)).asInstanceOf[SSLSocket]
    //
    sslSocket.setUseClientMode(true)
    sslSocket.startHandshake()



    //sslSocket.getOutputStream().write("what are doing now?".getBytes())
    //sslSocket.getOutputStream().flush()

    Thread.sleep(60 * 1000)
    sslSocket.close()
    System.out.println("ssl socket close")

    /*
    socket.shutdownInput()
    socket.shutdownOutput()
    socket.close()
    */

    System.out.println("ssl socket")

}
