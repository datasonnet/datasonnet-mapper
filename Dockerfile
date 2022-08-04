#
# Copyright 2019-2022 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM openjdk:8-jre-alpine
VOLUME /tmp

ENV _JAVA_OPTIONS "-Djava.awt.headless=true"
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/datasonnet/app.jar"]

RUN addgroup datasonnet && \
    adduser -D -S -h /var/cache/datasonnet -s /sbin/nologin -G datasonnet datasonnet
USER datasonnet
WORKDIR /var/cache/datasonnet

ARG JAR_FILE
COPY ${JAR_FILE} /opt/datasonnet/app.jar
