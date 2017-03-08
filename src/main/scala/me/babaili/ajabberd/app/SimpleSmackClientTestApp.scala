package me.babaili.ajabberd.app

import javax.net.ssl.{HostnameVerifier, SSLSession}
import javax.security.auth.callback.CallbackHandler

import org.jivesoftware.smack.SASLAuthentication
import org.jivesoftware.smack.java7.Java7SmackInitializer
import org.jivesoftware.smack.sasl.SASLMechanism
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.slf4j.LoggerFactory

/**
  * Created by cyq on 08/03/2017.
  */
object SimpleSmackClientTestApp extends App {

    val logger = LoggerFactory.getLogger("SimpleSmackClientTestApp")


    SASLAuthentication.registerSASLMechanism(new SASLMechanism {override def checkIfSuccessfulOrThrow() = {
            logger.debug("checkIfSuccessfulOrThrow")
        }

        override def getAuthenticationText = "hello".getBytes()

        override def getName = SASLMechanism.DIGESTMD5

        override def newInstance() = {
            this
        }

        override def getPriority = 100

        override def authenticateInternal(cbh: CallbackHandler) = {
            //
            logger.debug(s"authenticateInternal")
        }
    })

    //new Java7SmackInitializer().initialize()

    val connectionConfiguration = XMPPTCPConnectionConfiguration
        .builder()
        .setServiceName("localhost")
        .setDebuggerEnabled(true)
        .setHostnameVerifier(new HostnameVerifier {
            override def verify(s: String, sslSession: SSLSession) = {
                logger.debug(s"s ${s}")
                logger.debug(s"sslSession ${sslSession.getPeerHost()}")
                true
            }
        })
        .build()

    val trustPath = getClass().getResource("/netty/client/cChat.jks").getPath()
    System.setProperty("javax.net.ssl.trustStore", trustPath)

    val conn = new XMPPTCPConnection(connectionConfiguration)

    conn.connect()

    logger.debug("connected to server")

    conn.login("aa","bbb")

    logger.debug("logon to server")


    Thread.sleep(5 * 1000)


}
