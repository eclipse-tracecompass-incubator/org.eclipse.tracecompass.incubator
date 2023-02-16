# Copyright (c) 2023 Polytechnique de Montréal
#
# All rights reserved. This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0 which
# accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

ARG VARIANT=latest
FROM tracecompass:${VARIANT} as packager

ENV JAVA_MINIMAL="/opt/java-minimal"

RUN apk --no-cache add openjdk17-jdk openjdk17-jmods maven && \
    # build minimal JRE
    /usr/lib/jvm/java-17-openjdk/bin/jlink \
    --verbose \
    --add-modules \
    java.base,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.unsupported \
    --compress 2 --no-header-files --strip-java-debug-attributes --no-man-pages \
    --output "$JAVA_MINIMAL"

COPY ./ /app/

WORKDIR /app/

RUN mvn clean install -DskipTests

FROM alpine:3.16.0

ENV JAVA_HOME=/opt/java-minimal
ENV PATH="$PATH:$JAVA_HOME/bin"

# Required dependency for Eclipse Trace Compass Server
RUN apk --no-cache add libc6-compat

COPY --from=packager "$JAVA_HOME" "$JAVA_HOME"
COPY --from=packager /app/trace-server/org.eclipse.tracecompass.incubator.trace.server.product/target/products/traceserver/linux/gtk/x86_64/trace-compass-server /usr/src/tracecompass

WORKDIR /usr/src/tracecompass
EXPOSE 8080
CMD ["./tracecompass-server"]
