package me.babaili.ajabberd.app

import javax.net.ssl.{HostnameVerifier, SSLSession}
import javax.security.auth.callback.CallbackHandler

import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.{SASLAuthentication, StanzaListener}
import org.jivesoftware.smack.java7.Java7SmackInitializer
import org.jivesoftware.smack.packet.{Message, Stanza}
import org.jivesoftware.smack.sasl.SASLMechanism
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.slf4j.LoggerFactory

/**
  * Created by cyq on 08/03/2017.
  */
object SimpleSmackClientTestApp extends App {

    val logger = LoggerFactory.getLogger("SimpleSmackClientTestApp")


    SASLAuthentication.registerSASLMechanism(new SASLMechanism {
        override def checkIfSuccessfulOrThrow() = {
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
        .setHost("localhost")
        .setPort(5222)
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

    conn.login("aa", "bbb")

    logger.debug("logon to server")

    val hello = new Message()
    hello.setFrom("aa")
    hello.setTo("bb")
    hello.setType(Message.Type.chat)
    hello.setBody("hello")
    hello.setThread("hello-1")

    conn.sendStanzaWithResponseCallback(hello, new StanzaFilter {
        override def accept(stanza: Stanza) = {
            logger.debug(s"accept package ${stanza.getStanzaId()}")
            true
        }
    }, new StanzaListener {
        override def processPacket(packet: Stanza) = {
            logger.debug(s"process package ${packet.getStanzaId()}")
        }
    })


    Thread.sleep(4 * 1000)

    conn.disconnect()

    Thread.sleep(2 * 1000)


}
