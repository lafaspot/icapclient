package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapOptions;
import com.lafaspot.icap.client.codec.IcapRespmod;

public interface IcapRequestProducer {

    IcapOptions generateOptions();

    IcapRespmod generateRespMod();

    //TODO: Add generateReqMod()

}
