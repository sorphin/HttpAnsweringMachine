FROM ham.master:latest
RUN echo samples.calendar.multimaster version-4.2.1

COPY ./docker/multi/calendar.multi.external.json /etc/app/ham/app/external.json

