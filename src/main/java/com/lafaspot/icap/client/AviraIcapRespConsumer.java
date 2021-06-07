package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.exception.IcapException;

import javax.annotation.Nonnull;

public class AviraIcapRespConsumer extends AbstractIcapResponseConsumer {

    @Override
    public void responseReceived(int status, @Nonnull final IcapMessage icapMessage) throws IcapException {
        if(status != HTTP_STATUS_CODE_500) {
            handleIcap200Ok(icapMessage.getIcapHeaders(), icapMessage.getResult());
        }
    }

    protected void handleIcap200Ok(@Nonnull final String[] headers, @Nonnull IcapResult result) throws IcapException {
        int index = 1;
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_AVIRA_X_RESPONSE_INFO)) {
                int j = headers[index].indexOf(ICAP_AVIRA_X_RESPONSE_INFO);
                if (-1 != j) {
                    String xResponseInfo = headers[index].substring(j + ICAP_AVIRA_X_RESPONSE_INFO.length() + 1);
                    switch (xResponseInfo) {
                    case "Clean":
                        result.setDisposition(IcapResult.Disposition.CLEAN);
                        break;
                    default:
                    case "Infected file":
                        result.setDisposition(IcapResult.Disposition.INFECTED_UNREPAIRED);
                        break;
                    }
                }
            }
        }
    }

    /**
     * X_Response_Info header.
     */
    private static final String ICAP_AVIRA_X_RESPONSE_INFO = "X-Response-Info:";
}
