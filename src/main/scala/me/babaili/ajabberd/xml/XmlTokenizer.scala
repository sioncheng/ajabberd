package me.babaili.ajabberd.xml

import javax.xml.stream.events.XMLEvent

import com.fasterxml.aalto.AsyncXMLStreamReader
import com.fasterxml.aalto.evt.EventAllocatorImpl
import com.fasterxml.aalto.stax.InputFactoryImpl

/**
  * Created by chengyongqiao on 13/01/2017.
  */
class XmlTokenizer {


    val factory = new InputFactoryImpl()
    val allocator = EventAllocatorImpl.getDefaultInstance()
    val reader = factory.createAsyncForByteArray()

    def decode(data: Array[Byte]): Either[List[XMLEvent], Exception] = {

        try {

            var result: List[XMLEvent] = List.empty
            reader.getInputFeeder().feedInput(data, 0, data.length)

            var hasXmlFrame = reader.hasNext() && reader.next() != AsyncXMLStreamReader.EVENT_INCOMPLETE
            while (hasXmlFrame) {
                val xmlEvent = allocator.allocate(reader)
                result = result :+ xmlEvent

                hasXmlFrame = reader.hasNext() && reader.next() != AsyncXMLStreamReader.EVENT_INCOMPLETE
            }

            Left(result)

        } catch {
            case ex: Exception => Right(ex)
        }

    }

}
