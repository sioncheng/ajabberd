package me.babaili.ajabberd.net

import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl._
import javax.net.ssl.SSLEngineResult.HandshakeStatus
import javax.net.ssl.SSLEngineResult.Status

import akka.actor.Actor
import akka.io.Tcp.Received
import com.typesafe.scalalogging.Logger


/**
  * Created by chengyongqiao on 12/02/2017.
  */

object SslEngine {
    case class Proceed()
    val logger = Logger("me.babaili.ajabberd.net.SslEngine")
}

class SslEngine extends Actor {

    import SslEngine._

    val password = "123456".toCharArray
    val keyStore = KeyStore.getInstance("jks")
    val inServer = getClass().getResourceAsStream("server.keystore")
    keyStore.load(inServer, password)

    val trustKeyStore = KeyStore.getInstance("jks")
    val inCaTrust = getClass().getResourceAsStream("ca-trust.keystore")
    trustKeyStore.load(inCaTrust, password)

    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore, password)
    logger.debug(s"KeyManagerFactory.getDefaultAlgorithm ${KeyManagerFactory.getDefaultAlgorithm()}")

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(trustKeyStore)
    logger.debug(s"TrustManagerFactory.getDefaultAlgorithm ${TrustManagerFactory.getDefaultAlgorithm()}")

    val myX509TrustManager = new X509TrustManager {
        override def getAcceptedIssuers = {
            logger.debug("get accepted iss users")
            null
        }

        override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) = {
            logger.debug("check client trusted")
        }

        override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) = {
            logger.debug("check server trusted")
        }
    }
    val trustManagers = new Array[TrustManager](1)
    trustManagers(0) = myX509TrustManager

    val sslContext = SSLContext.getInstance("TLSv1")
    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null)

    val sslEngine = sslContext.createSSLEngine()
    sslEngine.setUseClientMode(false)

    val sslSession = sslEngine.getSession()

    val myAppData = ByteBuffer.allocate(sslSession.getApplicationBufferSize())
    val myNetData = ByteBuffer.allocate(sslSession.getPacketBufferSize())
    var peerAppData = ByteBuffer.allocate(sslSession.getApplicationBufferSize())
    var peerNetData = ByteBuffer.allocate(sslSession.getPacketBufferSize())
    myAppData.clear()
    myNetData.clear()
    peerAppData.clear()
    peerNetData.clear()

    logger.debug(s"application buffer size ${sslSession.getApplicationBufferSize()}")
    logger.debug(s"packet buffer size ${sslSession.getPacketBufferSize()}")

    var handshakeStatus: HandshakeStatus = null
    var assumeHasPeerNetData = true
    var assumeHasMyAppData = true


    logger.debug("Ssl Engine initialization")
    /*
    logger.debug("supported cipher suites")
    sslEngine.getSupportedCipherSuites().foreach(println _)
    logger.debug("supported protocols")
    sslEngine.getSupportedProtocols().foreach(println _)
    logger.debug("enabled cipher suites")
    sslEngine.getEnabledCipherSuites().foreach(println _)
    logger.debug("enabled protocols")
    sslEngine.getEnabledProtocols().foreach(println _)
    */
    sslEngine.setUseClientMode(false)
    sslEngine.setWantClientAuth(true)
    sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites())
    sslEngine.beginHandshake()
    doHandshake()


    def receive = {
        case Received(data) =>
            logger.debug(s"received ${data.length} \r\n ${data.utf8String}")
            logger.debug(s"peer net data ${peerNetData.position()} ${peerNetData.limit()} ${peerNetData.capacity()}")
            //peerNetData.compact()
            //data.foreach(b => peerNetData.put(b))
            val dest = new Array[Byte](data.length)
            data.copyToArray(dest)
            peerNetData.put(dest)
            if (handshakeStatus == null || handshakeStatus != HandshakeStatus.FINISHED) {
                doHandshake()
            } else {
                //assumeHasPeerNetData = true
                unwrap()
            }
    }

    def unwrap() = {
        var shouldLoop = true
        while (peerNetData.hasRemaining() && shouldLoop) {
            peerAppData.clear();
            val result = sslEngine.unwrap(peerNetData, peerAppData)
            logger.debug(s"bytes produced:${result.bytesProduced()} bytes consumed:${result.bytesConsumed()}")
            result.getStatus() match {
                case Status.OK =>
                    peerAppData.flip()
                    logger.debug("Incoming message: " + new String(peerAppData.array()))
                    shouldLoop = false
                case Status.BUFFER_OVERFLOW =>
                    peerAppData = enlargeApplicationBuffer(sslEngine, peerAppData)
                    shouldLoop = false
                case Status.BUFFER_UNDERFLOW =>
                    peerNetData = handleBufferUnderflow(sslEngine, peerNetData)
                    shouldLoop = false
                case Status.CLOSED =>
                    logger.debug("Client wants to close connection...")
                    //closeConnection(socketChannel, engine);
                    logger.debug("Goodbye client!")
            }
        }

    }

    def doHandshake() = {
        logger.debug("about to do handshake")


        var result: SSLEngineResult = null

        handshakeStatus = sslEngine.getHandshakeStatus()

        var shouldLoop: Boolean = true


        while(shouldLoop) {
            handshakeStatus match {
                case HandshakeStatus.NEED_UNWRAP =>
                    //
                    logger.debug("need unwrap")
                    logger.debug(s"peer net data has remaining ${peerNetData.hasRemaining()}")
                    logger.debug(s"peer net data ${peerNetData.position()} ${peerNetData.limit()} ${peerNetData.capacity()}")

                    peerNetData.flip()
                    logger.debug(s"peer net data flipped ${peerNetData.position()} ${peerNetData.limit()} ${peerNetData.capacity()}")
                    shouldLoop = peerNetData.limit() > 0
                    if (shouldLoop) {
                        try {
                            result = sslEngine.unwrap(peerNetData, peerAppData)
                            handshakeStatus = result.getHandshakeStatus()
                            logger.debug(s"bytes produced:${result.bytesProduced()} bytes consumed:${result.bytesConsumed()}")
                        } catch {
                            case e: SSLException =>
                                logger.error("ssl exception during do handshake", e)
                                handshakeStatus = sslEngine.getHandshakeStatus()
                                shouldLoop = false
                        }
                    }
                    peerNetData.compact() //for next write
                    if (shouldLoop) {
                        result.getStatus() match  {
                            case Status.OK =>
                                //
                                logger.debug("ok")
                                peerAppData.flip()
                                logger.debug(s"peer app data ${peerAppData.limit()}")
                                val inData = new String(peerAppData.array())
                                logger.debug(s"incoming message ${inData}")
                                peerAppData.compact()
                                //shouldLoop = false
                            case Status.BUFFER_OVERFLOW =>
                                //
                                logger.debug("buffer overflow")
                            case Status.BUFFER_UNDERFLOW =>
                                //
                                // Will occur either when no data was read from the peer or when the peerNetData
                                // buffer was too small to hold all peer's data.
                                logger.debug("buffer underflow")
                                peerNetData = handleBufferUnderflow(sslEngine, peerNetData)
                                //shouldLoop = false
                            case Status.CLOSED =>
                                //
                                logger.debug("closed")

                        }
                    }


                case HandshakeStatus.NEED_WRAP =>
                    //
                    logger.debug("need wrap")
                    myAppData.clear()
                    myAppData.put("".getBytes())
                    myAppData.flip()

                    try {
                        result = sslEngine.wrap(myAppData, myNetData)
                        handshakeStatus = result.getHandshakeStatus()
                    } catch {
                        case e: SSLException =>
                            logger.error("ssl exception during do handshake", e)
                            handshakeStatus = sslEngine.getHandshakeStatus()
                            shouldLoop = false
                    }

                    if (shouldLoop) {
                        result.getStatus() match {
                            case Status.OK =>
                                logger.debug("ok")
                                myNetData.flip()
                                logger.debug(s"my netdata limit ${myNetData.limit()}")
                            case Status.BUFFER_OVERFLOW =>
                                //
                                logger.debug("buffer overflow")
                            case Status.BUFFER_UNDERFLOW =>
                                //
                                logger.debug("buffer underflow")
                            case Status.CLOSED =>
                                //
                                logger.debug("closed")

                        }
                    }

                case HandshakeStatus.NEED_TASK =>
                    //
                    logger.debug("need task")
                    var task = sslEngine.getDelegatedTask()
                    while(task != null) {
                        logger.debug("run task")
                        task.run()
                        task = sslEngine.getDelegatedTask()
                    }
                    handshakeStatus = sslEngine.getHandshakeStatus()
                    logger.debug(s"after run task ${handshakeStatus}")
                case HandshakeStatus.FINISHED =>
                    //
                    logger.debug("finished")
                case HandshakeStatus.NOT_HANDSHAKING =>
                    //
                    logger.debug("not handshaking")
            }

            shouldLoop = shouldLoop && handshakeStatus != HandshakeStatus.FINISHED
            shouldLoop = shouldLoop && handshakeStatus != HandshakeStatus.NOT_HANDSHAKING

            logger.debug(s"should loop ? ${shouldLoop} ${handshakeStatus}")
        }
    }






    def handleBufferUnderflow(engine: SSLEngine, buffer: ByteBuffer) = {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
            logger.debug("remain the old buffer")
            buffer
        } else {
            logger.debug("enlarge the old buffer")
            val newBuffer = enlargePacketBuffer(engine, buffer)
            buffer.flip()
            newBuffer.put(buffer)
            newBuffer
        }
    }

    def enlargePacketBuffer(engine: SSLEngine, buffer: ByteBuffer) = {
        enlargeBuffer(buffer, engine.getSession().getPacketBufferSize())
    }

    def enlargeApplicationBuffer(engine: SSLEngine, buffer: ByteBuffer) = {
        enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize())
    }


    def enlargeBuffer(buffer: ByteBuffer , sessionProposedCapacity: Int) = {
        if(sessionProposedCapacity > buffer.capacity()) {
            ByteBuffer.allocate(sessionProposedCapacity)
        } else {
            ByteBuffer.allocate(buffer.capacity() * 2)
        }
    }

}
