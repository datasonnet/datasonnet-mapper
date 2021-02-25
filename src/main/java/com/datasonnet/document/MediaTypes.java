package com.datasonnet.document;

/*-
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Optional;

/**
 * Collection of well known MediaTypes.
 *
 * <p>
 * This file is a derived work of org.springframework.util.MimeType and org.springframework.http.MediaType classes from
 * Spring Framework v5.3.0-M1. Modifications made to the original work include:
 * <li>Collected declared MimeTypes and MediaTypes</li>
 * <li>Added CSV and Java MediaType</li>
 * <li>Rename ALL as ANY</li>
 * </p>
 *
 * @author Arjen Poutsma (2002-2020)
 * @author Juergen Hoeller (2002-2020)
 * @author Rossen Stoyanchev (2002-2020)
 * @author Sebastien Deleuze (2002-2020)
 * @author Kazuki Shimizu (2002-2020)
 * @author Sam Brannen (2002-2020)
 * @author Jose Montoya
 * @since 0.3.0
 */
public class MediaTypes {
    /**
     * Public constant media type that includes all media ranges (i.e. "&#42;/&#42;").
     */
    public static final MediaType ANY;

    /**
     * A String equivalent of {@link MediaTypes#ANY}.
     */
    public static final String ANY_VALUE = "*/*";

    /**
     * Public constant media type for {@code application/atom+xml}.
     */
    public static final MediaType APPLICATION_ATOM_XML;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_ATOM_XML}.
     */
    public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

    /**
     * Public constant media type for {@code application/cbor}.
     *
     * @since 0.3.0
     */
    public static final MediaType APPLICATION_CBOR;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_CBOR}.
     *
     * @since 0.3.0
     */
    public static final String APPLICATION_CBOR_VALUE = "application/cbor";

    /**
     * Public constant media type for {@code application/x-www-form-urlencoded}.
     */
    public static final MediaType APPLICATION_FORM_URLENCODED;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_FORM_URLENCODED}.
     */
    public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

    /**
     * Public constant media type for {@code application/json}.
     */
    public static final MediaType APPLICATION_JSON;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_JSON}.
     */
    public static final String APPLICATION_JSON_VALUE = "application/json";

    /**
     * Public constant media type for {@code application/octet-stream}.
     */
    public static final MediaType APPLICATION_OCTET_STREAM;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_OCTET_STREAM}.
     */
    public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

    /**
     * Public constant media type for {@code application/pdf}.
     *
     * @since 0.3.0
     */
    public static final MediaType APPLICATION_PDF;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_PDF}.
     *
     * @since 0.3.0
     */
    public static final String APPLICATION_PDF_VALUE = "application/pdf";

    /**
     * Public constant media type for {@code application/problem+json}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1">
     * Problem Details for HTTP APIs, 6.1. application/problem+json</a>
     * @since 0.3.0
     */
    public static final MediaType APPLICATION_PROBLEM_JSON;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_PROBLEM_JSON}.
     *
     * @since 0.3.0
     */
    public static final String APPLICATION_PROBLEM_JSON_VALUE = "application/problem+json";

    /**
     * Public constant media type for {@code application/problem+xml}.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.2">
     * Problem Details for HTTP APIs, 6.2. application/problem+xml</a>
     * @since 0.3.0
     */
    public static final MediaType APPLICATION_PROBLEM_XML;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_PROBLEM_XML}.
     *
     * @since 0.3.0
     */
    public static final String APPLICATION_PROBLEM_XML_VALUE = "application/problem+xml";

    /**
     * Public constant media type for {@code application/rss+xml}.
     *
     * @since 0.3.0
     */
    public static final MediaType APPLICATION_RSS_XML;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_RSS_XML}.
     *
     * @since 0.3.0
     */
    public static final String APPLICATION_RSS_XML_VALUE = "application/rss+xml";

    /**
     * Public constant media type for {@code application/stream+json}.
     *
     * @since 0.3.0
     */
    public static final MediaType APPLICATION_STREAM_JSON;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_STREAM_JSON}.
     *
     * @since 0.3.0
     */
    public static final String APPLICATION_STREAM_JSON_VALUE = "application/stream+json";

    /**
     * Public constant media type for {@code application/xhtml+xml}.
     */
    public static final MediaType APPLICATION_XHTML_XML;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_XHTML_XML}.
     */
    public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

    /**
     * Public constant media type for {@code application/xml}.
     */
    public static final MediaType APPLICATION_XML;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_XML}.
     */
    public static final String APPLICATION_XML_VALUE = "application/xml";

    /**
     * Public constant media type for {@code image/gif}.
     */
    public static final MediaType IMAGE_GIF;

    /**
     * A String equivalent of {@link MediaTypes#IMAGE_GIF}.
     */
    public static final String IMAGE_GIF_VALUE = "image/gif";

    /**
     * Public constant media type for {@code image/jpeg}.
     */
    public static final MediaType IMAGE_JPEG;

    /**
     * A String equivalent of {@link MediaTypes#IMAGE_JPEG}.
     */
    public static final String IMAGE_JPEG_VALUE = "image/jpeg";

    /**
     * Public constant media type for {@code image/png}.
     */
    public static final MediaType IMAGE_PNG;

    /**
     * A String equivalent of {@link MediaTypes#IMAGE_PNG}.
     */
    public static final String IMAGE_PNG_VALUE = "image/png";

    /**
     * Public constant media type for {@code multipart/form-data}.
     */
    public static final MediaType MULTIPART_FORM_DATA;

    /**
     * A String equivalent of {@link MediaTypes#MULTIPART_FORM_DATA}.
     */
    public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

    /**
     * Public constant media type for {@code multipart/mixed}.
     *
     * @since 0.3.0
     */
    public static final MediaType MULTIPART_MIXED;

    /**
     * A String equivalent of {@link MediaTypes#MULTIPART_MIXED}.
     *
     * @since 0.3.0
     */
    public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";

    /**
     * Public constant media type for {@code multipart/related}.
     *
     * @since 0.3.0
     */
    public static final MediaType MULTIPART_RELATED;

    /**
     * A String equivalent of {@link MediaTypes#MULTIPART_RELATED}.
     *
     * @since 0.3.0
     */
    public static final String MULTIPART_RELATED_VALUE = "multipart/related";

    /**
     * Public constant media type for {@code text/event-stream}.
     *
     * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
     * @since 0.3.0
     */
    public static final MediaType TEXT_EVENT_STREAM;

    /**
     * A String equivalent of {@link MediaTypes#TEXT_EVENT_STREAM}.
     *
     * @since 0.3.0
     */
    public static final String TEXT_EVENT_STREAM_VALUE = "text/event-stream";

    /**
     * Public constant media type for {@code text/html}.
     */
    public static final MediaType TEXT_HTML;

    /**
     * A String equivalent of {@link MediaTypes#TEXT_HTML}.
     */
    public static final String TEXT_HTML_VALUE = "text/html";

    /**
     * Public constant media type for {@code text/markdown}.
     *
     * @since 0.3.0
     */
    public static final MediaType TEXT_MARKDOWN;

    /**
     * A String equivalent of {@link MediaTypes#TEXT_MARKDOWN}.
     *
     * @since 0.3.0
     */
    public static final String TEXT_MARKDOWN_VALUE = "text/markdown";

    /**
     * Public constant media type for {@code text/plain}.
     */
    public static final MediaType TEXT_PLAIN;

    /**
     * A String equivalent of {@link MediaTypes#TEXT_PLAIN}.
     */
    public static final String TEXT_PLAIN_VALUE = "text/plain";

    /**
     * Public constant media type for {@code text/xml}.
     */
    public static final MediaType TEXT_XML;

    /**
     * A String equivalent of {@link MediaTypes#TEXT_XML}.
     */
    public static final String TEXT_XML_VALUE = "text/xml";

    public static final MediaType APPLICATION_JAVA;

    public static final String APPLICATION_JAVA_VALUE = "application/x-java-object";

    public static final MediaType APPLICATION_CSV;

    public static final String APPLICATION_CSV_VALUE = "application/csv";

    /**
     * Public constant media type for {@code application/yaml}.
     */
    public static final MediaType APPLICATION_YAML;

    /**
     * A String equivalent of {@link MediaTypes#APPLICATION_YAML}.
     */
    public static final String APPLICATION_YAML_VALUE = "application/x-yaml";

    // See Null Object pattern
    /**
     * Public constant media type for representing an unknown content type. This is meant to used to signal to Datasonnet
     * that the content type of a given input is unknown at design time. Datasonnet may, for example, look in the header
     * or default to a particular MediaType. Using this MediaType should be avoided outside of interactions with Datasonnet
     */
    public static final MediaType UNKNOWN;

    public static final String UNKNOWN_VALUE = "unknown/unknown";

    public static final String PARAM_CHARSET = "charset";

    public static final String PARAM_QUALITY_FACTOR = "q";

    static {
        // Not using "valueOf' to avoid static init cost
        ANY = new MediaType("*", "*");
        APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");
        APPLICATION_CBOR = new MediaType("application", "cbor");
        APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");
        APPLICATION_JSON = new MediaType("application", "json");
        APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
        APPLICATION_PDF = new MediaType("application", "pdf");
        APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");
        APPLICATION_PROBLEM_XML = new MediaType("application", "problem+xml");
        APPLICATION_RSS_XML = new MediaType("application", "rss+xml");
        APPLICATION_STREAM_JSON = new MediaType("application", "stream+json");
        APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml");
        APPLICATION_XML = new MediaType("application", "xml");
        IMAGE_GIF = new MediaType("image", "gif");
        IMAGE_JPEG = new MediaType("image", "jpeg");
        IMAGE_PNG = new MediaType("image", "png");
        MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");
        MULTIPART_MIXED = new MediaType("multipart", "mixed");
        MULTIPART_RELATED = new MediaType("multipart", "related");
        TEXT_EVENT_STREAM = new MediaType("text", "event-stream");
        TEXT_HTML = new MediaType("text", "html");
        TEXT_MARKDOWN = new MediaType("text", "markdown");
        TEXT_PLAIN = new MediaType("text", "plain");
        TEXT_XML = new MediaType("text", "xml");
        APPLICATION_JAVA = new MediaType("application", "x-java-object");
        APPLICATION_CSV = new MediaType("application", "csv");
        UNKNOWN = new MediaType("unknown", "unknown");
        APPLICATION_YAML = new MediaType("application", "x-yaml");
    }

    // TODO: 8/11/20 add explicit file extension support to MediaType class
    public static Optional<MediaType> forExtension(String ext) {
        switch (ext) {
            case "json":
                return Optional.of(APPLICATION_JSON);
            case "xml":
                return Optional.of(APPLICATION_XML);
            case "csv":
                return Optional.of(APPLICATION_CSV);
            case "txt":
                return Optional.of(TEXT_PLAIN);
            case "yml":
            case "yaml":
                return Optional.of(APPLICATION_YAML);
            default:
                return Optional.empty();
        }
    }
}
