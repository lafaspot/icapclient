/**
 *
 */
package com.lafaspot.icap.client.codec;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.IcapResult.Disposition;
import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.icap.client.exception.IcapException.FailureType;
import com.lafaspot.logfast.logging.Logger;

/**
 * Base IcapMessage object.
 *
 * @author kraman
 *
 */
public class IcapMessage {

    /** Holds the next states to move to if parsing is successful. */
    private List<State> nextStates = new ArrayList<State>();

    /** Current parsing state. */
    private State state = State.PARSE_ICAP_MESSAGE;

    /** Offset used on parsing. */
    private int payloadOffset;

    /** String buffer to hold the current parsed ICAP headers. */
    private StringBuffer currentMessage = new StringBuffer(MAX_HEADER_BUFFER);

    /** Payload length parsed from response. */
    private int payloadLen;

    /** Holds the response scanned/cleaned payload. */
    private byte[] resPayload;

    /** The logger object. */
    private final Logger logger;

    /** The result object. */
    private IcapResult result = new IcapResult();

    /** Parsed ICAP headers. */
    private String[] icapHeaders;

    /** Failure cause. */
    private Exception cause;

    /** ICAP message prefix. */
    private static final String ICAP_PREFIX = "ICAP/1.0";

    /** ICAP violations found prefix. */
    private static final String ICAP_VIOLATIONS_PREFIX = "X-Violations-Found:";

    /** ICAP Encapsulated header. */
    private static final String ICAP_ENCAPSULATED_PREFIX = "Encapsulated:";

    /** ICAP response body len prefix. */
    private static final String ICAP_RES_BODY_PREFIX = "res-body";

    /** ICAP response header len prefix. */
    private static final String ICAP_RES_HDR_PREFIX = "res-hdr";

    /** ICAP NULL body prefix. */
    private static final String ICAP_NULL_BODY_PREFIX = "null-body";

    /** ICAP header delimiter. */
    private static final byte[] ICAP_ENDOFHEADER_DELIM_HALF = { '\r', '\n' };

    /** ICAP header delimiter. */
    private static final byte[] ICAP_ENDOFHEADER_DELIM_FULL = { '\r', '\n', '\r', '\n' };

    /** HTTP status code 200. */
    private static final int HTTP_STATUS_CODE_200 = 200;

    /** HTTP status code 201. */
    private static final int HTTP_STATUS_CODE_201 = 201;

    /** HTTP status code 500. */
    private static final int HTTP_STATUS_CODE_500 = 500;

    /** Max buffer size. */
    private static final int MAX_HEADER_BUFFER = 1024;

    /** Max length of debug string in Exception. */
    private static final int MAX_DEBUG_STR_LEN = 10;

    /** Base for length parsing. */
    private static final int HEX_BASE = 16;

    /**
     * Constructor.
     *
     * @param logger logger object
     */
    public IcapMessage(@Nonnull final Logger logger) {
        this.logger = logger;
    }

    /**
     * Reset the state to reuse the message object, when parsing more than one message per session.
     */
    public void reset() {
        state = State.PARSE_ICAP_MESSAGE;
        currentMessage.setLength(0);
        cause = null;
        resPayload = null;
        icapHeaders = null;
        result = new IcapResult();
        payloadLen = payloadOffset = 0;
        nextStates.clear();
        resPayload = null;
    }

    /**
     * Returns if parsing is complete.
     *
     * @return true when parsing is complete
     */
    public boolean parsingDone() {
        return (state == State.PARSE_DONE);
    }

    /**
     * Function to parse incoming buffer.
     *
     * @param buf incoming buffer from channel
     * @param dec the decoder object
     */
    @SuppressWarnings("checkstyle:illegalcatch")
    public void parse(@Nonnull final ByteBuf buf, @Nonnull final IcapMessageDecoder dec) {
        try {
            // logger.debug("<- parse in - " + state + " - " + this.hashCode(), null);
            switch (state) {
            case PARSE_ICAP_MESSAGE: {
                if (!parseForHeader(buf, ICAP_ENDOFHEADER_DELIM_FULL)) {
                    return;
                }
                String header = currentMessage.toString();
                currentMessage.setLength(0);

                String[] headers = parseHeader(header);

                // now handle the ICAP message
                if (!handleIcapMessage(headers)) {
                    state = State.PARSE_DONE;

                    // reset the readIndex to avoid replay
                    buf.readerIndex(buf.writerIndex());

                    cause = new IcapException(FailureType.SERVER_ERROR);
                    return;
                }

                BasicLineParser lineParser = new BasicLineParser();
                final int maxLineLen = 128;
                CharArrayBuffer charBuf = new CharArrayBuffer(maxLineLen);

                int resHdr = -1;
                int resBody = -1;
                String encapsulatedHeaderStr = getEncapsulatedHeader(headers);
                charBuf.setLength(0);
                charBuf.append(encapsulatedHeaderStr);
                Header encapsulateHeder = lineParser.parseHeader(charBuf);
                String encapsulateHeaderVal = encapsulateHeder.getValue();
                if (null != encapsulateHeaderVal) {
                    BasicHeaderValueParser encParser = new BasicHeaderValueParser();

                    charBuf.setLength(0);
                    charBuf.append(encapsulateHeaderVal);
                    ParserCursor encCursor = new ParserCursor(0, encapsulateHeaderVal.length());
                    HeaderElement[] elems = encParser.parseElements(charBuf, encCursor);

                    try {
                        for (int i = 0; i < elems.length; i++) {
                            if (elems[i].getName().startsWith(ICAP_RES_HDR_PREFIX)) {
                                resHdr = Integer.parseInt(elems[i].getValue().trim());
                            }

                            if (elems[i].getName().startsWith(ICAP_RES_BODY_PREFIX)) {
                                resBody = Integer.parseInt(elems[i].getValue().trim());
                            }
                        }
                    } catch (NumberFormatException e) {
                        throw new IcapException(IcapException.FailureType.PARSE_ERROR, e);
                    }
                }

                // logger.debug("<- encap " + encapsulateHeaderVal + ", rh " + resHdr + ", rb " + resBody, null);
                if (-1 != resHdr) {
                    nextStates.add(State.PARSE_RES_HEADER);
                }

                if (-1 != resBody) {
                    // nextStates.add(State.PARSE_RES_BODY);
                    nextStates.add(State.PARSE_RES_PAYLOAD_LENGTH);
                    nextStates.add(State.PARSE_PAYLOAD);
                }

                nextStates.add(State.PARSE_DONE);

                state = nextStates.remove(0);
                break;
            }

            case PARSE_RES_HEADER: {
                if (!parseForHeader(buf, ICAP_ENDOFHEADER_DELIM_FULL)) {
                    return;
                }

                String header = currentMessage.toString();
                currentMessage.setLength(0);
                String[] headers = parseHeader(header);

                LineParser parser = new BasicLineParser();
                ParserCursor cursor = new ParserCursor(0, headers[0].length());
                final int maxStatusBuffer = 64;
                CharArrayBuffer statusBuffer = new CharArrayBuffer(maxStatusBuffer);
                statusBuffer.append(headers[0]);
                StatusLine statusLine = parser.parseStatusLine(statusBuffer, cursor);

                // handle 200 OK
                if (statusLine.getStatusCode() == HTTP_STATUS_CODE_200) {
                    // todo
                } else {
                    // todo
                }

                state = nextStates.remove(0);
                // logger.debug(" done with parsing body - moving to " + state, null);
                break;
            }

            case PARSE_RES_PAYLOAD_LENGTH: {
                if (!parseForHeader(buf, dec, ICAP_ENDOFHEADER_DELIM_HALF)) {
                    return;
                }
                final String lengthStr = currentMessage.toString().trim();

                currentMessage.setLength(0);
                try {
                    payloadLen = Integer.parseInt(lengthStr, HEX_BASE);
                } catch (NumberFormatException e) {
                    final String errorLenStr = (lengthStr.length() > MAX_DEBUG_STR_LEN ? lengthStr.substring(0, MAX_DEBUG_STR_LEN) : lengthStr);
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR, Arrays.asList("payloadLen", errorLenStr));
                }

                resPayload = new byte[payloadLen];
                payloadOffset = 0;
                state = nextStates.remove(0);
                // logger.debug(" done with parsing payload len=" + payloadLen + " - moving to " + state, null);
                break;
            }

            case PARSE_PAYLOAD:
                if (0 == payloadLen) {
                    // bad
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR);
                }
                final int availableLen = buf.writerIndex() - buf.readerIndex();
                final int toReadLen = payloadLen - payloadOffset;

                // the readBytes() API will update readerIndex
                if (toReadLen < availableLen) {
                    buf.readBytes(resPayload, payloadOffset, toReadLen);
                    payloadOffset += toReadLen;

                } else {
                    buf.readBytes(resPayload, payloadOffset, availableLen);
                    payloadOffset += availableLen;
                }
                if (payloadOffset < payloadLen) {
                    // logger.debug("more to raad o " + payloadOffset + ", l " + payloadLen + ", ri " + buf.readerIndex(), null);
                    // still more to read
                    return;
                }

                // reset the readIndex to avoid replay
                buf.readerIndex(buf.writerIndex());

                result.setCleanedBytes(resPayload);

                state = nextStates.remove(0);
                // logger.debug(" done with parsing payload of " + payloadLen + " bytes - moving to " + state, null);

            case PARSE_DONE:
                // reset the readIndex to avoid replay
                buf.readerIndex(buf.writerIndex());
                break;
            default:

            }
        } catch (Exception e) {
            cause = e;
            state = State.PARSE_DONE;
            // reset the readIndex to avoid replay
            buf.readerIndex(buf.writerIndex());
        }
    }

    /**
     * Get AV scan result.
     *
     * @return the result object
     */
    public IcapResult getResult() {
        return result;
    }

    /**
     * Get AV scan failure cause.
     *
     * @return failure cause if any
     */
    public Exception getCause() {
        return cause;
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        if (null != cause) {
            buf.append(cause);
        }

        if (null != icapHeaders) {
            for (int i = 0; i < icapHeaders.length; i++) {
                buf.append(icapHeaders[i]);
                buf.append("\n");
            }
        }

        return buf.toString();
    }

    /**
     * Parse ICAP message.
     *
     * @param headers headers to be parsed
     * @return true if response was successful
     * @throws IcapException on failure
     */
    private boolean handleIcapMessage(@Nonnull final String[] headers) throws IcapException {
        if (headers[0].startsWith(ICAP_PREFIX)) {

            icapHeaders = headers;
            String[] toks = headers[0].split(" ");
            if (toks.length > 2) {
                int status;
                try {
                    status = Integer.parseInt(toks[1].trim());
                } catch (NumberFormatException e) {
                    final String errorStatusStr = toks[1].length() > MAX_DEBUG_STR_LEN ? toks[1].substring(0, 0) : toks[1];
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR, Arrays.asList("icapStatusHdr", errorStatusStr));
                }
                // logger.debug("-- icap status code " + status, null);
                switch (status) {
                case HTTP_STATUS_CODE_201:
                    handleIcap201Ok(headers);
                    return true;
                case HTTP_STATUS_CODE_200:
                    handleIcap200Ok(headers);
                    return true;
                case HTTP_STATUS_CODE_500:
                    return false;
                default:
                }
            }
        }
        throw new IcapException(IcapException.FailureType.PARSE_ERROR_ICAP_STATUS, Arrays.asList("invalidStatus",
                String.valueOf(headers[0])));
    }

    /**
     * Look for the "Encapsulated" header in an ICAP message.
     *
     * @param headers list of ICAP headers
     * @return the parsed Encapsualted header if present
     * @throws IcapException on failure
     */
    private String getEncapsulatedHeader(final String[] headers) throws IcapException {

        for (int index = 0; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_ENCAPSULATED_PREFIX)) {
                int j = headers[index].indexOf(ICAP_ENCAPSULATED_PREFIX);
                if (-1 != j) {
                    return headers[index]; // .substring(j + ICAP_RES_BODY_PREFIX.length() + 1);
                }
            }
        }

        throw new IcapException(IcapException.FailureType.PARSE_ERROR);
    }

    /**
     * Parse until the header delimiter is reached. Handles partial message buffer, will return false if the delimiter is not yet found.
     *
     * @param buf incoming buffer
     * @param delim delimiter
     * @return true if parsing is complete, false otherwise
     * @throws IcapException on failure
     */
    private boolean parseForHeader(@Nonnull final ByteBuf buf, @Nonnull final byte[] delim) throws IcapException {
        // logger.debug(" parseHeader r:" + buf.readerIndex() + ", w:" + buf.writerIndex(), null);
        if (buf.readableBytes() < delim.length) {
            // error
            throw new IcapException(IcapException.FailureType.PARSE_ERROR);
        }
        int eohIdx = 0;
        for (int idx = buf.readerIndex(); idx < buf.writerIndex(); idx++) {
            final char msg = (char) buf.getByte(idx);
            if (msg == delim[eohIdx]) {
                eohIdx++;
                if (eohIdx == (delim.length)) {
                    buf.readerIndex(idx + 1);
                    // remove last 3 bytes because we did not add the 4th delim byte yet
                    currentMessage.setLength(currentMessage.length() - (delim.length - 1));
                    return true;
                } else {
                    currentMessage.append(msg);
                }
            } else {
                currentMessage.append(msg);
                eohIdx = 0;
            }
        }

        // drop the entire message and parse again
        currentMessage.setLength(0);
        return false;
    }

    /**
     * A variant of parseForHeader where we can look for CRLFCRLF or just CRLF. TODO: merge the two functions.
     *
     * @param buf incoming buffer
     * @param dec the decoder object
     * @param delim delimiter
     * @return true if parsing is complete
     * @throws IcapException on failure
     */
    private boolean parseForHeader(@Nonnull final ByteBuf buf, @Nonnull final IcapMessageDecoder dec, @Nonnull final byte[] delim)
            throws IcapException {

        // logger.debug(" parseHeader r:" + buf.readerIndex() + ", w:" + buf.writerIndex(), null);
        if (buf.readableBytes() < delim.length) {
            // error
            throw new IcapException(IcapException.FailureType.PARSE_ERROR);
        }
        int eohIdx = 0;
        // delim bytes CRLF
        final int eohLen = delim.length;
        for (int idx = buf.readerIndex(); idx < buf.writerIndex(); idx++) {
            final char msg = (char) buf.getByte(idx);
            if (msg == delim[eohIdx]) {
                eohIdx++;
                if (eohIdx == (delim.length)) {
                    // remove the last (eohLen -1) bytes as the last byte is yet to be added
                    currentMessage.setLength(currentMessage.length() - (eohLen - 1));
                    // next byte to be read is idx+1
                    buf.readerIndex(idx + 1);
                    return true;
                } else {
                    // length did not match, add the byte (as char) to global buffer
                    currentMessage.append(msg);
                }
            } else {
                currentMessage.append(msg);
                eohIdx = 0;
            }
        }

        currentMessage.setLength(0);
        return false;
    }

    /**
     * Parse ICAP 200 OK message.
     *
     * @param headers ICAP headers
     * @throws IcapException on failure
     */
    private void handleIcap200Ok(@Nonnull final String[] headers) throws IcapException {
        int index = 1;
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_ENCAPSULATED_PREFIX)) {
                int j = headers[index].indexOf(ICAP_RES_BODY_PREFIX);
                if (-1 != j) {
                    String resBodyStr = headers[index].substring(j + ICAP_RES_BODY_PREFIX.length() + 1);

                    try {
                        // TODO: validate the parsed value
                        Integer.parseInt(resBodyStr.trim());
                        result.setNumViolations(0);
                        result.setDisposition(Disposition.CLEAN);
                        break;
                    } catch (NumberFormatException e) {
                        final String partOfTheErrorStr = (resBodyStr.length() > MAX_DEBUG_STR_LEN ? resBodyStr.substring(0,
                                (MAX_DEBUG_STR_LEN - 1)) : resBodyStr);
                        throw new IcapException(IcapException.FailureType.PARSE_ERROR, Arrays.asList("bodyLen", partOfTheErrorStr));
                    }

                } else if (headers[index].indexOf(ICAP_NULL_BODY_PREFIX) != -1) {
                    // done
                    return;
                }
            }
        }
    }

    /**
     * Parse ICAP 201 message.
     *
     * @param headers ICAP headers
     * @throws IcapException on failure
     */
    private void handleIcap201Ok(@Nonnull final String[] headers) throws IcapException {

        // skip first status line
        int index = 1;
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_VIOLATIONS_PREFIX)) {
                break;
            }
        }

        int numViolations;
        if (index < headers.length) {
            final int k = headers[index].indexOf(':');
            if (-1 != k) {
                try {
                    numViolations = Integer.parseInt(headers[index].substring(k + 1).trim());
                    result.setNumViolations(numViolations);
                } catch (NumberFormatException e) {
                    final String partOfTheErrorStr = (headers[index].length() > MAX_DEBUG_STR_LEN ? headers[index].substring(0,
                            (MAX_DEBUG_STR_LEN - 1))
                            : headers[index]);
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR, Arrays.asList("numViolations", partOfTheErrorStr));
                }
            } else {
                throw new IcapException(IcapException.FailureType.PARSE_ERROR);
            }
            // increment
            index++;

            final int headersPerViolation = 4;
            // validate header size
            if (index + (headersPerViolation * numViolations) < headers.length) {
                // look at first violation only
                result.setViolationFilename(headers[index++]);
                result.setViolationName(headers[index++]);
                result.setViolationId(headers[index++]);
                String dispositionStr = headers[index++];
                result.setDispositionAsStr(dispositionStr);
            } else {
                throw new IcapException(IcapException.FailureType.PARSE_ERROR);
            }

        } else {
            throw new IcapException(IcapException.FailureType.PARSE_ERROR);
        }
    }

    /**
     * Parse ICAP headers.
     *
     * @param buf message buffer
     * @return headers
     */
    private String[] parseHeader(final String buf) {
        final Pattern pat = Pattern.compile("\r\n");
        return pat.split(buf);
    }

    /** ICAP message states - when parsing. */
    enum State {
        /** parsing ICAP message. */
        PARSE_ICAP_MESSAGE,
        /** parsing ICAP response header. */
        PARSE_RES_HEADER, PARSE_RES_BODY,
        /** Parsing ICAP payload length. */
        PARSE_RES_PAYLOAD_LENGTH,
        /** Parsing ICAP payload. */
        PARSE_PAYLOAD,
        /** Parsing complete. */
        PARSE_DONE
    }

}
