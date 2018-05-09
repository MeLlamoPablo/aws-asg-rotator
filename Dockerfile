FROM alpine:3.7

RUN apk add --no-cache openjdk8

# We download the gradle distribution without including the whole /app directroy
# so each time we have to rebuild the image we can run from cache.
ADD ./gradle /app/gradle
ADD ./gradlew /app
WORKDIR /app
RUN ./gradlew --no-daemon wrapper

# Same here, we only re-download dependencies when any of the .gradle file is
# changed, instead of any file in the whole project.
ADD ./build.gradle /app
ADD ./settings.gradle /app
RUN ./gradlew --no-daemon dependencies

# Finally, build the app.
ADD . /app
RUN ./gradlew --no-daemon shadowJar

RUN mv ./build/libs/aws-asg-rotator-prod-*.jar /app.jar && \
	echo "#!/bin/sh" > /usr/local/bin/rotate-asg && \
	echo "java -jar /app.jar" >> /usr/local/bin/rotate-asg && \
	chmod +x /usr/local/bin/rotate-asg

# Remove bloat from the image
RUN rm -rf ~/.gradle /app

