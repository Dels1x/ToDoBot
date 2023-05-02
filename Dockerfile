FROM rabbitmq:3.11-management


COPY rabbitmq.conf /etc/rabbitmq/

RUN rabbitmq-plugins enable --offline rabbitmq_management
RUN rabbitmq-plugins enable --offline rabbitmq_mqtt
RUN rabbitmq-plugins enable --offline rabbitmq_web_mqtt

ENV RABBITMQ_DEFAULT_USER=delsix
ENV RABBITMQ_DEFAULT_PASS=delsix

EXPOSE 15672 5672