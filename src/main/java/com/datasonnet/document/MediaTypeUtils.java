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

import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Miscellaneous {@link MediaType} utility methods.
 *
 * <p>
 * This file is a derived work of org.springframework.util.MimeTypeUtils class from
 * Spring Framework v5.3.0-M1. Modifications made to the original work include:
 * <li>Handle escape chars when parsing params in parseMediaTypeInternal</li>
 * <li>Check parameter validity when parsing params in parseMediaTypeInternal</li>
 * </p>
 *
 * @author Arjen Poutsma (2002-2020)
 * @author Rossen Stoyanchev (2002-2020)
 * @author Dimitrios Liapis (2002-2020)
 * @author Brian Clozel (2002-2020)
 * @author Sam Brannen (2002-2020)
 * @author Jose Montoya
 * @since 0.3.0
 */
public abstract class MediaTypeUtils {

    private static final byte[] BOUNDARY_CHARS =
            new byte[]{'-', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
                    'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
                    'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                    'V', 'W', 'X', 'Y', 'Z'};

    /**
     * Comparator used by {@link #sortBySpecificity(List)}.
     */
    public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new MediaType.SpecificityComparator<>();

    private static final ConcurrentLruCache<String, MediaType> cachedMimeTypes =
            new ConcurrentLruCache<>(64, MediaTypeUtils::parseMediaTypeInternal);

    @Nullable
    private static volatile Random random;

    /**
     * Parse the given String into a single {@code MimeType}.
     * Recently parsed {@code MimeType} are cached for further retrieval.
     *
     * @param mediaType the string to parse
     * @return the mime type
     * @throws InvalidMediaTypeException if the string cannot be parsed
     */
    public static MediaType parseMediaType(String mediaType) {
        if (mediaType == null || mediaType.isEmpty()) {
            throw new InvalidMediaTypeException(mediaType, "'mimeType' must not be empty");
        }

        // do not cache multipart mime types with random boundaries
        if (mediaType.startsWith("multipart")) {
            return parseMediaTypeInternal(mediaType);
        }
        return cachedMimeTypes.get(mediaType);
    }

    private static MediaType parseMediaTypeInternal(String mimeType) {
        int index = mimeType.indexOf(';');
        String fullType = (index >= 0 ? mimeType.substring(0, index) : mimeType).trim();
        if (fullType.isEmpty()) {
            throw new InvalidMediaTypeException(mimeType, "'mimeType' must not be empty");
        }

        // java.net.HttpURLConnection returns a *; q=.2 Accept header
        if (MediaType.WILDCARD_TYPE.equals(fullType)) {
            fullType = "*/*";
        }
        int subIndex = fullType.indexOf('/');
        if (subIndex == -1) {
            throw new InvalidMediaTypeException(mimeType, "does not contain '/'");
        }
        if (subIndex == fullType.length() - 1) {
            throw new InvalidMediaTypeException(mimeType, "does not contain subtype after '/'");
        }
        String type = fullType.substring(0, subIndex);
        String subtype = fullType.substring(subIndex + 1);
        if (MediaType.WILDCARD_TYPE.equals(type) && !MediaType.WILDCARD_TYPE.equals(subtype)) {
            throw new InvalidMediaTypeException(mimeType, "wildcard type is legal only in '*/*' (all mime types)");
        }

        Map<String, String> parameters = null;
        do {
            int nextIndex = index + 1;
            boolean quoted = false;
            while (nextIndex < mimeType.length()) {
                char ch = mimeType.charAt(nextIndex);
                if (ch == ';') {
                    if (!quoted) {
                        break;
                    }
                } else if (ch == '"') {
                    quoted = !quoted;
                } else if (ch == '\\') {
                    nextIndex++; // skip the escaped char too. eg: "\";"
                }
                nextIndex++;
            }
            String parameter = mimeType.substring(index + 1, nextIndex).trim();
            if (parameter.length() > 0) {
                if (parameters == null) {
                    parameters = new LinkedHashMap<>(4);
                }
                int eqIndex = parameter.indexOf('=');
                if (eqIndex >= 0) {
                    String attribute = parameter.substring(0, eqIndex).trim();
                    String value = parameter.substring(eqIndex + 1).trim();

                    try {
                        MediaType.checkParameters(attribute, value);
                    } catch (IllegalArgumentException ex) {
                        throw new InvalidMediaTypeException(mimeType, ex.getMessage());
                    }

                    parameters.put(attribute, value);
                }
            }
            index = nextIndex;
        }
        while (index < mimeType.length());

        try {
            return new MediaType(type, subtype, parameters);
        } catch (UnsupportedCharsetException ex) {
            throw new InvalidMediaTypeException(mimeType, "unsupported charset '" + ex.getCharsetName() + "'");
        } catch (IllegalArgumentException ex) {
            throw new InvalidMediaTypeException(mimeType, ex.getMessage());
        }
    }

    /**
     * Parse the comma-separated string into a list of {@code MimeType} objects.
     *
     * @param mimeTypes the string to parse
     * @return the list of mime types
     * @throws InvalidMediaTypeException if the string cannot be parsed
     */
    public static List<MediaType> parseMediaTypes(String mimeTypes) {
        if (mimeTypes == null || mimeTypes.isEmpty()) {
            return Collections.emptyList();
        }

        return tokenize(mimeTypes).stream()
                .filter(Utils::hasText)
                .map(MediaTypeUtils::parseMediaType)
                .collect(Collectors.toList());
    }

    /**
     * Tokenize the given comma-separated string of {@code MimeType} objects
     * into a {@code List<String>}. Unlike simple tokenization by ",", this
     * method takes into account quoted parameters.
     *
     * @param mediaTypes the string to tokenize
     * @return the list of tokens
     * @since 0.3.0
     */
    public static List<String> tokenize(String mediaTypes) {
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        int startIndex = 0;
        int i = 0;
        while (i < mediaTypes.length()) {
            switch (mediaTypes.charAt(i)) {
                case '"':
                    inQuotes = !inQuotes;
                    break;
                case ',':
                    if (!inQuotes) {
                        tokens.add(mediaTypes.substring(startIndex, i));
                        startIndex = i + 1;
                    }
                    break;
                case '\\':
                    i++;
                    break;
            }
            i++;
        }
        tokens.add(mediaTypes.substring(startIndex));
        return tokens;
    }

    /**
     * Return a string representation of the given list of {@code MimeType} objects.
     *
     * @param mimeTypes the string to parse
     * @return the list of mime types
     * @throws IllegalArgumentException if the String cannot be parsed
     */
    public static String toString(Collection<? extends MediaType> mimeTypes) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<? extends MediaType> iterator = mimeTypes.iterator(); iterator.hasNext(); ) {
            MediaType mimeType = iterator.next();
            mimeType.appendTo(builder);
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    /**
     * Sorts the given list of {@code MimeType} objects by specificity.
     * <p>Given two mime types:
     * <ol>
     * <li>if either mime type has a {@linkplain MediaType#isWildcardType() wildcard type},
     * then the mime type without the wildcard is ordered before the other.</li>
     * <li>if the two mime types have different {@linkplain MediaType#getType() types},
     * then they are considered equal and remain their current order.</li>
     * <li>if either mime type has a {@linkplain MediaType#isWildcardSubtype() wildcard subtype}
     * , then the mime type without the wildcard is sorted before the other.</li>
     * <li>if the two mime types have different {@linkplain MediaType#getSubtype() subtypes},
     * then they are considered equal and remain their current order.</li>
     * <li>if the two mime types have a different amount of
     * {@linkplain MediaType#getParameter(String) parameters}, then the mime type with the most
     * parameters is ordered before the other.</li>
     * </ol>
     * <p>For example: <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
     * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
     * <blockquote>audio/basic == text/html</blockquote> <blockquote>audio/basic ==
     * audio/wave</blockquote>
     *
     * @param mimeTypes the list of mime types to be sorted
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
     * and Content, section 5.3.2</a>
     */
    public static void sortBySpecificity(List<MediaType> mimeTypes) {
        Objects.requireNonNull(mimeTypes, "'mimeTypes' must not be null");
        if (mimeTypes.size() > 1) {
            mimeTypes.sort(SPECIFICITY_COMPARATOR);
        }
    }

    /**
     * Lazily initialize the {@link SecureRandom} for {@link #generateMultipartBoundary()}.
     */
    private static Random initRandom() {
        Random randomToUse = random;
        if (randomToUse == null) {
            synchronized (MediaTypeUtils.class) {
                randomToUse = random;
                if (randomToUse == null) {
                    randomToUse = new SecureRandom();
                    random = randomToUse;
                }
            }
        }
        return randomToUse;
    }

    /**
     * Generate a random MIME boundary as bytes, often used in multipart mime types.
     */
    public static byte[] generateMultipartBoundary() {
        Random randomToUse = initRandom();
        byte[] boundary = new byte[randomToUse.nextInt(11) + 30];
        for (int i = 0; i < boundary.length; i++) {
            boundary[i] = BOUNDARY_CHARS[randomToUse.nextInt(BOUNDARY_CHARS.length)];
        }
        return boundary;
    }

    /**
     * Generate a random MIME boundary as String, often used in multipart mime types.
     */
    public static String generateMultipartBoundaryString() {
        return new String(generateMultipartBoundary(), StandardCharsets.US_ASCII);
    }


    /**
     * Simple Least Recently Used cache, bounded by the maximum size given
     * to the class constructor.
     * <p>This implementation is backed by a {@code ConcurrentHashMap} for storing
     * the cached values and a {@code ConcurrentLinkedQueue} for ordering the keys
     * and choosing the least recently used key when the cache is at full capacity.
     *
     * @param <K> the type of the key used for caching
     * @param <V> the type of the cached values
     */
    private static class ConcurrentLruCache<K, V> {

        private final int maxSize;

        private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

        private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

        private final ReadWriteLock lock;

        private final Function<K, V> generator;

        private volatile int size;

        public ConcurrentLruCache(int maxSize, Function<K, V> generator) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("LRU max size should be positive");
            }

            Objects.requireNonNull(generator, "Generator function should not be null");
            this.maxSize = maxSize;
            this.generator = generator;
            this.lock = new ReentrantReadWriteLock();
        }

        public V get(K key) {
            V cached = this.cache.get(key);
            if (cached != null) {
                if (this.size < this.maxSize) {
                    return cached;
                }
                this.lock.readLock().lock();
                try {
                    if (this.queue.removeLastOccurrence(key)) {
                        this.queue.offer(key);
                    }
                    return cached;
                } finally {
                    this.lock.readLock().unlock();
                }
            }
            this.lock.writeLock().lock();
            try {
                // Retrying in case of concurrent reads on the same key
                cached = this.cache.get(key);
                if (cached != null) {
                    if (this.queue.removeLastOccurrence(key)) {
                        this.queue.offer(key);
                    }
                    return cached;
                }
                // Generate value first, to prevent size inconsistency
                V value = this.generator.apply(key);
                int cacheSize = this.size;
                if (cacheSize == this.maxSize) {
                    K leastUsed = this.queue.poll();
                    if (leastUsed != null) {
                        this.cache.remove(leastUsed);
                        cacheSize--;
                    }
                }
                this.queue.offer(key);
                this.cache.put(key, value);
                this.size = cacheSize + 1;
                return value;
            } finally {
                this.lock.writeLock().unlock();
            }
        }
    }
}
