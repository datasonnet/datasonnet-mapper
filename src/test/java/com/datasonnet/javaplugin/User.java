package com.datasonnet.javaplugin;

/*-
 * Copyright 2019-2025 the original author or authors.
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class User {
    private String name;
    private Optional<String> email;
    private OptionalInt age;
    private OptionalLong height;
    private OptionalDouble weight;
    private LocalDate birthDate;
    private LocalTime wakeUpTime;
    private LocalDateTime lunchTime;


    public User(String name, String email, int age, long height, double weight,
                LocalDate birthDate, LocalTime wakeUpTime, LocalDateTime lunchTime) {
        this.name = name;
        this.email = Optional.ofNullable(email);
        this.age = OptionalInt.of(age);
        this.height = OptionalLong.of(height);
        this.weight = OptionalDouble.of(weight);
        this.birthDate = birthDate;
        this.wakeUpTime = wakeUpTime;
        this.lunchTime = lunchTime;
    }

    public User(String name, String email) {
        this.name = name;
        this.email = Optional.ofNullable(email);
    }

    // Getters e setters para todos os campos
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Optional.ofNullable(email);
    }

    public OptionalInt getAge() {
        return age;
    }

    public void setAge(OptionalInt age) {
        this.age = age;
    }

    public OptionalLong getHeight() {
        return height;
    }

    public void setHeight(OptionalLong height) {
        this.height = height;
    }

    public OptionalDouble getWeight() {
        return weight;
    }

    public void setWeight(OptionalDouble weight) {
        this.weight = weight;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalTime getWakeUpTime() {
        return wakeUpTime;
    }

    public void setWakeUpTime(LocalTime wakeUpTime) {
        this.wakeUpTime = wakeUpTime;
    }

    public LocalDateTime getLunchTime() {
        return lunchTime;
    }

    public void setLunchTime(LocalDateTime lunchTime) {
        this.lunchTime = lunchTime;
    }
}
