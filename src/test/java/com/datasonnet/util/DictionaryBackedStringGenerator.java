package com.datasonnet.util;

/*-
 * The original work for this file is available under the terms of the
 * BSD 2-Clause "Simplified" License. The derived work is made available
 * under the terms of the Apache License, Version 2.0
 */

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
 * Copyright (c) 2017-2018 The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: 9/23/20 state modifications
public class DictionaryBackedStringGenerator extends Generator<String> {

    private final List<String> dictionary;
    private Generator<String> fallback;

    public DictionaryBackedStringGenerator(String source, Generator<String> fallback) throws IOException {
        super(String.class);
        this.fallback = fallback;

        List<String> dictionary = new ArrayList<>();

        // Read dictionary words
        try (InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(source)) {
            if (in == null) {
                throw new FileNotFoundException("Dictionary file not found: " + source);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String item;
            while ((item = br.readLine()) != null) {
                dictionary.add(item);
            }
        }
        this.dictionary = Collections.unmodifiableList(dictionary);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        if (true) {
            int choice = random.nextInt(dictionary.size());
            return dictionary.get(choice);
        } else {
            if (fallback == null) {
                fallback = gen().type(String.class);
            }
            return fallback.generate(random, status);
        }
    }

}