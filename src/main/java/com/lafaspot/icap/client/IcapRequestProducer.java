package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapOptions;
import com.lafaspot.icap.client.codec.IcapRespmod;

/**
 * @author nimmyr
 * This interface define methods for generating icap request messages.
 */
public interface IcapRequestProducer {

    /**
     * Generate ICAP OPTIONS Message.
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc3507#page-29">https://datatracker.ietf.org/doc/html/rfc3507#page-29</a>
     * @return Icap options object
     */
    IcapOptions generateOptions();

    /**
     * Generate ICAP RESPMOD message.
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc3507#page-27">https://datatracker.ietf.org/doc/html/rfc3507#page-27</a>
     * @param keepAlive KeepAlive header
     * @return Icap RespMod object
     */
    IcapRespmod generateRespMod(boolean keepAlive);

    //TODO: Add generateReqMod()

}
