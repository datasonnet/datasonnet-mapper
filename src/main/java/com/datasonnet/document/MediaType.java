package com.datasonnet.document;

/*-
 * Copyright 2019-2020 the original author or authors.
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

import com.datasonnet.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static com.datasonnet.document.MediaTypes.PARAM_CHARSET;
import static com.datasonnet.document.MediaTypes.PARAM_QUALITY_FACTOR;

/**
 * Represents a MediaType as defined in the HTTP Specification.
 *
 * <p>
 * This file is a derived work of org.springframework.util.MimeType and org.springframework.http.MediaType classes from
 * Spring Framework v5.3.0-M1. Modifications made to the original work include:
 * <li>Combining both classes into a single file, removing references to MimeType</li>
 * <li>Added null check to isQuotedString</li>
 * <li>Replace escape chars from parameter values in unquote</li>
 * <li>Made checkParameters and support methods static</li>
 * <li>Removed parameters validation in constructor</li>
 * <li>unquote each param value in constructor</li>
 * </p>
 *
 * @author Arjen Poutsma (2002-2020)
 * @author Juergen Hoeller (2002-2020)
 * @author Rossen Stoyanchev (2002-2020)
 * @author Sebastien Deleuze (2002-2020)
 * @author Kazuki Shimizu (2002-2020)
 * @author Sam Brannen (2002-2020)
 * @author Jose Montoya
 * @see MediaTypeUtils
 * @see MediaTypes
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.1">HTTP 1.1: Semantics and Content, section 3.1.1.1</a>
 * @since 0.3.0
 */
public class MediaType implements Comparable<MediaType>, Serializable {
    private static final long serialVersionUID = 2069937152339670231L;

    protected static final String WILDCARD_TYPE = "*";

    private static final BitSet TOKEN;

    static {
        // variable names refer to RFC 2616, section 2.2
        BitSet ctl = new BitSet(128);
        for (int i = 0; i <= 31; i++) {
            ctl.set(i);
        }
        ctl.set(127);

        BitSet separators = new BitSet(128);
        separators.set('(');
        separators.set(')');
        separators.set('<');
        separators.set('>');
        separators.set('@');
        separators.set(',');
        separators.set(';');
        separators.set(':');
        separators.set('\\');
        separators.set('\"');
        separators.set('/');
        separators.set('[');
        separators.set(']');
        separators.set('?');
        separators.set('=');
        separators.set('{');
        separators.set('}');
        separators.set(' ');
        separators.set('\t');

        TOKEN = new BitSet(128);
        TOKEN.set(0, 128);
        TOKEN.andNot(ctl);
        TOKEN.andNot(separators);
    }

    private final String type;

    private final String subtype;

    private final Map<String, String> parameters;

    @Nullable
    private volatile String toStringValue;

    /**
     * Create a new {@code MediaType} for the given primary type.
     * <p>The {@linkplain #getSubtype() subtype} is set to "&#42;", parameters empty.
     *
     * @param type the primary type
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type) {
        this(type, WILDCARD_TYPE);
    }

    /**
     * Create a new {@code MediaType} for the given primary type and subtype.
     * <p>The parameters are empty.
     *
     * @param type    the primary type
     * @param subtype the subtype
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype) {
        this(type, subtype, Collections.emptyMap());
    }

    /**
     * Create a new {@code MediaType} for the given type, subtype, and character set.
     *
     * @param type    the primary type
     * @param subtype the subtype
     * @param charset the character set
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype, Charset charset) {
        this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charset.name()));
    }

    /**
     * Create a new {@code MediaType} for the given type, subtype, and quality value.
     *
     * @param type         the primary type
     * @param subtype      the subtype
     * @param qualityValue the quality value
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype, double qualityValue) {
        this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
    }

    /**
     * Copy-constructor that copies the type, subtype and parameters of the given
     * {@code MediaType}, and allows to set the specified character set.
     *
     * @param other   the other media type
     * @param charset the character set
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     * @since 0.3.0
     */
    public MediaType(MediaType other, Charset charset) {
        this(other.getType(), other.getSubtype(), addCharsetParameter(charset, other.getParameters()));
    }

    /**
     * Copy-constructor that copies the type and subtype of the given {@code MediaType},
     * and allows for different parameters.
     *
     * @param other      the other media type
     * @param parameters the parameters, may be {@code null}
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(MediaType other, @Nullable Map<String, String> parameters) {
        this(other.getType(), other.getSubtype(), parameters);
    }

    /**
     * Create a new {@code MediaType} for the given type, subtype, and parameters.
     *
     * @param type       the primary type
     * @param subtype    the subtype
     * @param parameters the parameters, may be {@code null}
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     */
    public MediaType(String type, String subtype, @Nullable Map<String, String> parameters) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("'type' must not be empty");
        }

        if (subtype == null || subtype.isEmpty()) {
            throw new IllegalArgumentException("'subtype' must not be empty");
        }

        checkToken(type);
        checkToken(subtype);
        this.type = type.toLowerCase(Locale.ENGLISH);
        this.subtype = subtype.toLowerCase(Locale.ENGLISH);
        if ((parameters != null && !parameters.isEmpty())) {
            Map<String, String> map = new LinkedHashMap<>(parameters.size());
            parameters.forEach((attribute, value) -> {
                map.put(attribute.toLowerCase(Locale.ENGLISH), unquote(value));
            });
            this.parameters = Collections.unmodifiableMap(map);
        } else {
            this.parameters = Collections.emptyMap();
        }
    }

    /**
     * Create a new {@code MediaType} for the given {@link MediaType}.
     * The type, subtype and parameters information is copied and {@code MediaType}-specific
     * checks on parameters are performed.
     *
     * @param other the MIME type
     * @throws IllegalArgumentException if any of the parameters contain illegal characters
     * @since 0.3.0
     */
    public MediaType(MediaType other) {
        this.type = other.type;
        this.subtype = other.subtype;
        this.parameters = other.parameters;
        this.toStringValue = other.toStringValue;
    }

    /**
     * Checks the given token string for illegal characters, as defined in RFC 2616,
     * section 2.2.
     *
     * @throws IllegalArgumentException in case of illegal characters
     * @see <a href="https://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1, section 2.2</a>
     */
    private static void checkToken(String token) {
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (!TOKEN.get(ch)) {
                throw new IllegalArgumentException("Invalid token character '" + ch + "' in token \"" + token + "\"");
            }
        }
    }

    private static boolean isQuotedString(String s) {
        if (s == null) {
            return false;
        }

        if (s.length() < 2) {
            return false;
        } else {
            return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
        }
    }

    protected static String unquote(String s) {
        if (!isQuotedString(s)) {
            return s;
        }

        return s.substring(1, s.length() - 1).replaceAll("\\\\(.)", "$1");
    }

    /**
     * Indicates whether the {@linkplain #getType() type} is the wildcard character
     * <code>&#42;</code> or not.
     */
    public boolean isWildcardType() {
        return WILDCARD_TYPE.equals(getType());
    }

    /**
     * Indicates whether the {@linkplain #getSubtype() subtype} is the wildcard
     * character <code>&#42;</code> or the wildcard character followed by a suffix
     * (e.g. <code>&#42;+xml</code>).
     *
     * @return whether the subtype is a wildcard
     */
    public boolean isWildcardSubtype() {
        return WILDCARD_TYPE.equals(getSubtype()) || getSubtype().startsWith("*+");
    }

    /**
     * Indicates whether this MIME Type is concrete, i.e. whether neither the type
     * nor the subtype is a wildcard character <code>&#42;</code>.
     *
     * @return whether this MIME Type is concrete
     */
    public boolean isConcrete() {
        return !isWildcardType() && !isWildcardSubtype();
    }

    /**
     * Return the primary type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Return the subtype.
     */
    public String getSubtype() {
        return this.subtype;
    }

    /**
     * Return the character set, as indicated by a {@code charset} parameter, if any.
     *
     * @return the character set, or {@code null} if not available
     * @since 0.3.0
     */
    @Nullable
    public Charset getCharset() {
        String charset = getParameter(PARAM_CHARSET);
        return (charset != null ? Charset.forName(unquote(charset)) : null);
    }

    /**
     * Return a generic parameter value, given a parameter name.
     *
     * @param name the parameter name
     * @return the parameter value, or {@code null} if not present
     */
    @Nullable
    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    /**
     * Return all generic parameter values.
     *
     * @return a read-only map (possibly empty, never {@code null})
     */
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    /**
     * Similar to {@link #equals(Object)} but based on the type and subtype
     * only, i.e. ignoring parameters.
     *
     * @param other the other mime type to compare to
     * @return whether the two mime types have the same type and subtype
     * @since 0.3.0
     */
    public boolean equalsTypeAndSubtype(@Nullable MediaType other) {
        if (other == null) {
            return false;
        }
        return this.type.equalsIgnoreCase(other.type) && this.subtype.equalsIgnoreCase(other.subtype);
    }

    /**
     * Unlike {@link Collection#contains(Object)} which relies on
     * {@link MediaType#equals(Object)}, this method only checks the type and the
     * subtype, but otherwise ignores parameters.
     *
     * @param mimeTypes the list of mime types to perform the check against
     * @return whether the list contains the given mime type
     * @since 0.3.0
     */
    public boolean isPresentIn(Collection<? extends MediaType> mimeTypes) {
        for (MediaType mimeType : mimeTypes) {
            if (mimeType.equalsTypeAndSubtype(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaType)) {
            return false;
        }
        MediaType otherType = (MediaType) other;
        return (this.type.equalsIgnoreCase(otherType.type) &&
                this.subtype.equalsIgnoreCase(otherType.subtype) &&
                parametersAreEqual(otherType));
    }

    /**
     * Determine if the parameters in this {@code MimeType} and the supplied
     * {@code MimeType} are equal, performing case-insensitive comparisons
     * for {@link Charset Charsets}.
     *
     * @since 0.3.0
     */
    private boolean parametersAreEqual(MediaType other) {
        if (this.parameters.size() != other.parameters.size()) {
            return false;
        }

        for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
            String key = entry.getKey();
            if (!other.parameters.containsKey(key)) {
                return false;
            }
            if (PARAM_CHARSET.equals(key)) {
                if (!Utils.nullSafeEquals(getCharset(), other.getCharset())) {
                    return false;
                }
            } else if (!Utils.nullSafeEquals(entry.getValue(), other.parameters.get(key))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.subtype.hashCode();
        result = 31 * result + this.parameters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        String value = this.toStringValue;
        if (value == null) {
            StringBuilder builder = new StringBuilder();
            appendTo(builder);
            value = builder.toString();
            this.toStringValue = value;
        }
        return value;
    }

    protected void appendTo(StringBuilder builder) {
        builder.append(this.type);
        builder.append('/');
        builder.append(this.subtype);
        appendTo(this.parameters, builder);
    }

    // TODO: 8/12/20 quote param values as necessary, the reverse of unquote(String)
    private void appendTo(Map<String, String> map, StringBuilder builder) {
        map.forEach((key, val) -> {
            builder.append(';');
            builder.append(key);
            builder.append('=');
            builder.append(val);
        });
    }

    /**
     * Compares this MIME Type to another alphabetically.
     *
     * @param other the MIME Type to compare to
     * @see MediaTypeUtils#sortBySpecificity(List)
     */
    @Override
    public int compareTo(MediaType other) {
        int comp = getType().compareToIgnoreCase(other.getType());
        if (comp != 0) {
            return comp;
        }
        comp = getSubtype().compareToIgnoreCase(other.getSubtype());
        if (comp != 0) {
            return comp;
        }
        comp = getParameters().size() - other.getParameters().size();
        if (comp != 0) {
            return comp;
        }

        TreeSet<String> thisAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        thisAttributes.addAll(getParameters().keySet());
        TreeSet<String> otherAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        otherAttributes.addAll(other.getParameters().keySet());
        Iterator<String> thisAttributesIterator = thisAttributes.iterator();
        Iterator<String> otherAttributesIterator = otherAttributes.iterator();

        while (thisAttributesIterator.hasNext()) {
            String thisAttribute = thisAttributesIterator.next();
            String otherAttribute = otherAttributesIterator.next();
            comp = thisAttribute.compareToIgnoreCase(otherAttribute);
            if (comp != 0) {
                return comp;
            }
            if (PARAM_CHARSET.equals(thisAttribute)) {
                Charset thisCharset = getCharset();
                Charset otherCharset = other.getCharset();
                if (thisCharset != otherCharset) {
                    if (thisCharset == null) {
                        return -1;
                    }
                    if (otherCharset == null) {
                        return 1;
                    }
                    comp = thisCharset.compareTo(otherCharset);
                    if (comp != 0) {
                        return comp;
                    }
                }
            } else {
                String thisValue = getParameters().get(thisAttribute);
                String otherValue = other.getParameters().get(otherAttribute);
                if (otherValue == null) {
                    otherValue = "";
                }
                comp = thisValue.compareTo(otherValue);
                if (comp != 0) {
                    return comp;
                }
            }
        }

        return 0;
    }

    private static Map<String, String> addCharsetParameter(Charset charset, Map<String, String> parameters) {
        Map<String, String> map = new LinkedHashMap<>(parameters);
        map.put(PARAM_CHARSET, charset.name());
        return map;
    }

    /**
     * Comparator to sort {@link MediaType MimeTypes} in order of specificity.
     *
     * @param <T> the type of mime types that may be compared by this comparator
     */
    public static class SpecificityComparator<T extends MediaType> implements Comparator<T> {

        @Override
        public int compare(T mimeType1, T mimeType2) {
            if (mimeType1.isWildcardType() && !mimeType2.isWildcardType()) {  // */* < audio/*
                return 1;
            } else if (mimeType2.isWildcardType() && !mimeType1.isWildcardType()) {  // audio/* > */*
                return -1;
            } else if (!mimeType1.getType().equals(mimeType2.getType())) {  // audio/basic == text/html
                return 0;
            } else {  // mediaType1.getType().equals(mediaType2.getType())
                if (mimeType1.isWildcardSubtype() && !mimeType2.isWildcardSubtype()) {  // audio/* < audio/basic
                    return 1;
                } else if (mimeType2.isWildcardSubtype() && !mimeType1.isWildcardSubtype()) {  // audio/basic > audio/*
                    return -1;
                } else if (!mimeType1.getSubtype().equals(mimeType2.getSubtype())) {  // audio/basic == audio/wave
                    return 0;
                } else {  // mediaType2.getSubtype().equals(mediaType2.getSubtype())
                    return compareParameters(mimeType1, mimeType2);
                }
            }
        }

        protected int compareParameters(T mimeType1, T mimeType2) {
            int paramsSize1 = mimeType1.getParameters().size();
            int paramsSize2 = mimeType2.getParameters().size();
            return Integer.compare(paramsSize2, paramsSize1);  // audio/basic;level=1 < audio/basic
        }
    }

    protected static void checkParameters(String attribute, String value) {
        if (attribute == null || attribute.isEmpty()) {
            throw new IllegalArgumentException("'attribute' must not be empty");
        }

        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("'value' must not be empty");
        }

        checkToken(attribute);
        if (PARAM_CHARSET.equals(attribute)) {
            value = unquote(value);
            Charset.forName(value);
        } else if (!isQuotedString(value)) {
            checkToken(value);
        }

        if (PARAM_QUALITY_FACTOR.equals(attribute)) {
            value = unquote(value);
            double d = Double.parseDouble(value);

            if (!(d >= 0D && d <= 1D)) {
                throw new IllegalArgumentException("Invalid quality value \"" + value + "\": should be between 0.0 and 1.0");
            }
        }
    }

    /**
     * Return the quality factor, as indicated by a {@code q} parameter, if any.
     * Defaults to {@code 1.0}.
     *
     * @return the quality factor as double value
     */
    public double getQualityValue() {
        String qualityFactor = getParameter(PARAM_QUALITY_FACTOR);
        return (qualityFactor != null ? Double.parseDouble(unquote(qualityFactor)) : 1D);
    }

    /**
     * Indicate whether this {@code MediaType} includes the given media type.
     * <p>For instance, {@code text/*} includes {@code text/plain} and {@code text/html},
     * and {@code application/*+xml} includes {@code application/soap+xml}, etc.
     * This method is <b>not</b> symmetric.
     * <p>Simply calls {@link MediaType#includes(MediaType)} but declared with a
     * {@code MediaType} parameter for binary backwards compatibility.
     *
     * @param other the reference media type with which to compare
     * @return {@code true} if this media type includes the given media type;
     * {@code false} otherwise
     */
    public boolean includes(@Nullable MediaType other) {
        if (other == null) {
            return false;
        }
        if (isWildcardType()) {
            // */* includes anything
            return true;
        } else if (getType().equals(other.getType())) {
            if (getSubtype().equals(other.getSubtype())) {
                return true;
            }
            if (isWildcardSubtype()) {
                // Wildcard with suffix, e.g. application/*+xml
                int thisPlusIdx = getSubtype().lastIndexOf('+');
                if (thisPlusIdx == -1) {
                    return true;
                } else {
                    // application/*+xml includes application/soap+xml
                    int otherPlusIdx = other.getSubtype().lastIndexOf('+');
                    if (otherPlusIdx != -1) {
                        String thisSubtypeNoSuffix = getSubtype().substring(0, thisPlusIdx);
                        String thisSubtypeSuffix = getSubtype().substring(thisPlusIdx + 1);
                        String otherSubtypeSuffix = other.getSubtype().substring(otherPlusIdx + 1);
                        return thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Indicate whether this {@code MediaType} is compatible with the given media type.
     * <p>For instance, {@code text/*} is compatible with {@code text/plain},
     * {@code text/html}, and vice versa. In effect, this method is similar to
     * {@link #includes}, except that it <b>is</b> symmetric.
     * <p>Simply calls {@link MediaType#isCompatibleWith(MediaType)} but declared with a
     * {@code MediaType} parameter for binary backwards compatibility.
     *
     * @param other the reference media type with which to compare
     * @return {@code true} if this media type is compatible with the given media type;
     * {@code false} otherwise
     */
    public boolean isCompatibleWith(@Nullable MediaType other) {
        if (other == null) {
            return false;
        }
        if (isWildcardType() || other.isWildcardType()) {
            return true;
        } else if (getType().equals(other.getType())) {
            if (getSubtype().equals(other.getSubtype())) {
                return true;
            }
            // Wildcard with suffix? e.g. application/*+xml
            if (isWildcardSubtype() || other.isWildcardSubtype()) {
                int thisPlusIdx = getSubtype().lastIndexOf('+');
                int otherPlusIdx = other.getSubtype().lastIndexOf('+');
                if (thisPlusIdx == -1 && otherPlusIdx == -1) {
                    return true;
                } else if (thisPlusIdx != -1 && otherPlusIdx != -1) {
                    String thisSubtypeNoSuffix = getSubtype().substring(0, thisPlusIdx);
                    String otherSubtypeNoSuffix = other.getSubtype().substring(0, otherPlusIdx);
                    String thisSubtypeSuffix = getSubtype().substring(thisPlusIdx + 1);
                    String otherSubtypeSuffix = other.getSubtype().substring(otherPlusIdx + 1);
                    return thisSubtypeSuffix.equals(otherSubtypeSuffix) &&
                            (WILDCARD_TYPE.equals(thisSubtypeNoSuffix) || WILDCARD_TYPE.equals(otherSubtypeNoSuffix));
                }
            }
        }
        return false;
    }

    /**
     * Return a replica of this instance with the quality value of the given {@code MediaType}.
     *
     * @return the same instance if the given MediaType doesn't have a quality value,
     * or a new one otherwise
     */
    public MediaType copyQualityValue(MediaType mediaType) {
        if (!mediaType.getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
            return this;
        }
        Map<String, String> params = new LinkedHashMap<>(getParameters());
        params.put(PARAM_QUALITY_FACTOR, mediaType.getParameters().get(PARAM_QUALITY_FACTOR));
        return new MediaType(this, params);
    }

    /**
     * Return a replica of this instance with its quality value removed.
     *
     * @return the same instance if the media type doesn't contain a quality value,
     * or a new one otherwise
     */
    public MediaType removeQualityValue() {
        if (!getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
            return this;
        }
        Map<String, String> params = new LinkedHashMap<>(getParameters());
        params.remove(PARAM_QUALITY_FACTOR);
        return new MediaType(this, params);
    }


    /**
     * Parse the given String value into a {@code MediaType} object,
     * with this method name following the 'valueOf' naming convention
     * (as supported by {org.springframework.core.convert.ConversionService}.
     *
     * @param value the string to parse
     * @throws InvalidMediaTypeException if the media type value cannot be parsed
     * @see #parseMediaType(String)
     */
    public static MediaType valueOf(String value) {
        return parseMediaType(value);
    }

    /**
     * Parse the given String into a single {@code MediaType}.
     *
     * @param mediaType the string to parse
     * @return the media type
     * @throws InvalidMediaTypeException if the media type value cannot be parsed
     */
    public static MediaType parseMediaType(String mediaType) {
        try {
            return MediaTypeUtils.parseMediaType(mediaType);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMediaTypeException(mediaType, ex.getMessage());
        }
    }

    /**
     * Parse the comma-separated string into a list of {@code MediaType} objects.
     * <p>This method can be used to parse an Accept or Content-Type header.
     *
     * @param mediaTypes the string to parse
     * @return the list of media types
     * @throws InvalidMediaTypeException if the media type value cannot be parsed
     */
    public static List<MediaType> parseMediaTypes(@Nullable String mediaTypes) {
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            return Collections.emptyList();
        }

        // Avoid using java.util.stream.Stream in hot paths
        List<String> tokenizedTypes = MediaTypeUtils.tokenize(mediaTypes);
        List<MediaType> result = new ArrayList<>(tokenizedTypes.size());
        for (String type : tokenizedTypes) {
            if (Utils.hasText(type)) {
                result.add(parseMediaType(type));
            }
        }
        return result;
    }

    /**
     * Parse the given list of (potentially) comma-separated strings into a
     * list of {@code MediaType} objects.
     * <p>This method can be used to parse an Accept or Content-Type header.
     *
     * @param mediaTypes the string to parse
     * @return the list of media types
     * @throws InvalidMediaTypeException if the media type value cannot be parsed
     * @since 0.3.0
     */
    public static List<MediaType> parseMediaTypes(@Nullable List<String> mediaTypes) {
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            return Collections.emptyList();
        } else if (mediaTypes.size() == 1) {
            return parseMediaTypes(mediaTypes.get(0));
        } else {
            List<MediaType> result = new ArrayList<>(8);
            for (String mediaType : mediaTypes) {
                result.addAll(parseMediaTypes(mediaType));
            }
            return result;
        }
    }

    /**
     * Return a string representation of the given list of {@code MediaType} objects.
     * <p>This method can be used to for an {@code Accept} or {@code Content-Type} header.
     *
     * @param mediaTypes the media types to create a string representation for
     * @return the string representation
     */
    public static String toString(Collection<MediaType> mediaTypes) {
        return MediaTypeUtils.toString(mediaTypes);
    }

    /**
     * Sorts the given list of {@code MediaType} objects by specificity.
     * <p>Given two media types:
     * <ol>
     * <li>if either media type has a {@linkplain #isWildcardType() wildcard type}, then the media type without the
     * wildcard is ordered before the other.</li>
     * <li>if the two media types have different {@linkplain #getType() types}, then they are considered equal and
     * remain their current order.</li>
     * <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard subtype}, then the media type without
     * the wildcard is sorted before the other.</li>
     * <li>if the two media types have different {@linkplain #getSubtype() subtypes}, then they are considered equal
     * and remain their current order.</li>
     * <li>if the two media types have different {@linkplain #getQualityValue() quality value}, then the media type
     * with the highest quality value is ordered before the other.</li>
     * <li>if the two media types have a different amount of {@linkplain #getParameter(String) parameters}, then the
     * media type with the most parameters is ordered before the other.</li>
     * </ol>
     * <p>For example:
     * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
     * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
     * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
     * <blockquote>audio/basic == text/html</blockquote>
     * <blockquote>audio/basic == audio/wave</blockquote>
     *
     * @param mediaTypes the list of media types to be sorted
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
     * and Content, section 5.3.2</a>
     */
    public static void sortBySpecificity(List<MediaType> mediaTypes) {
        Objects.requireNonNull(mediaTypes, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            mediaTypes.sort(SPECIFICITY_COMPARATOR);
        }
    }

    /**
     * Sorts the given list of {@code MediaType} objects by quality value.
     * <p>Given two media types:
     * <ol>
     * <li>if the two media types have different {@linkplain #getQualityValue() quality value}, then the media type
     * with the highest quality value is ordered before the other.</li>
     * <li>if either media type has a {@linkplain #isWildcardType() wildcard type}, then the media type without the
     * wildcard is ordered before the other.</li>
     * <li>if the two media types have different {@linkplain #getType() types}, then they are considered equal and
     * remain their current order.</li>
     * <li>if either media type has a {@linkplain #isWildcardSubtype() wildcard subtype}, then the media type without
     * the wildcard is sorted before the other.</li>
     * <li>if the two media types have different {@linkplain #getSubtype() subtypes}, then they are considered equal
     * and remain their current order.</li>
     * <li>if the two media types have a different amount of {@linkplain #getParameter(String) parameters}, then the
     * media type with the most parameters is ordered before the other.</li>
     * </ol>
     *
     * @param mediaTypes the list of media types to be sorted
     * @see #getQualityValue()
     */
    public static void sortByQualityValue(List<MediaType> mediaTypes) {
        Objects.requireNonNull(mediaTypes, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            mediaTypes.sort(QUALITY_VALUE_COMPARATOR);
        }
    }

    /**
     * Sorts the given list of {@code MediaType} objects by specificity as the
     * primary criteria and quality value the secondary.
     *
     * @see MediaType#sortBySpecificity(List)
     * @see MediaType#sortByQualityValue(List)
     */
    public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
        Objects.requireNonNull(mediaTypes, "'mediaTypes' must not be null");
        if (mediaTypes.size() > 1) {
            mediaTypes.sort(MediaType.SPECIFICITY_COMPARATOR.thenComparing(MediaType.QUALITY_VALUE_COMPARATOR));
        }
    }

    /**
     * Comparator used by {@link #sortByQualityValue(List)}.
     */
    public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = (mediaType1, mediaType2) -> {
        double quality1 = mediaType1.getQualityValue();
        double quality2 = mediaType2.getQualityValue();
        int qualityComparison = Double.compare(quality2, quality1);
        if (qualityComparison != 0) {
            return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
        } else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) {  // */* < audio/*
            return 1;
        } else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) {  // audio/* > */*
            return -1;
        } else if (!mediaType1.getType().equals(mediaType2.getType())) {  // audio/basic == text/html
            return 0;
        } else {  // mediaType1.getType().equals(mediaType2.getType())
            if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) {  // audio/* < audio/basic
                return 1;
            } else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) {  // audio/basic > audio/*
                return -1;
            } else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) {  // audio/basic == audio/wave
                return 0;
            } else {
                int paramsSize1 = mediaType1.getParameters().size();
                int paramsSize2 = mediaType2.getParameters().size();
                return Integer.compare(paramsSize2, paramsSize1);  // audio/basic;level=1 < audio/basic
            }
        }
    };

    /**
     * Comparator used by {@link #sortBySpecificity(List)}.
     */
    public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new SpecificityComparator<MediaType>() {

        @Override
        protected int compareParameters(MediaType mediaType1, MediaType mediaType2) {
            double quality1 = mediaType1.getQualityValue();
            double quality2 = mediaType2.getQualityValue();
            int qualityComparison = Double.compare(quality2, quality1);
            if (qualityComparison != 0) {
                return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
            }
            return super.compareParameters(mediaType1, mediaType2);
        }
    };
}

