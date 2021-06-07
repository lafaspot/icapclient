package com.lafaspot.icap.client;

public abstract class AbstractIcapResponseConsumer implements IcapResponseConsumer{

    /** HTTP status code 200. */
    protected static final int HTTP_STATUS_CODE_200 = 200;

    /** HTTP status code 201. */
    protected static final int HTTP_STATUS_CODE_201 = 201;

    /** HTTP status code 500. */
    protected static final int HTTP_STATUS_CODE_500 = 500;

    /** ICAP Encapsulated header. */
    protected static final String ICAP_ENCAPSULATED_PREFIX = "Encapsulated:";

    /** ICAP response body len prefix. */
    protected static final String ICAP_RES_BODY_PREFIX = "res-body";

    /** Max length of debug string in Exception. */
    protected static final int MAX_DEBUG_STR_LEN = 10;

    /** ICAP NULL body prefix. */
    protected static final String ICAP_NULL_BODY_PREFIX = "null-body";
}
